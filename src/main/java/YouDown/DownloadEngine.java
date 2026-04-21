package YouDown;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class DownloadEngine {

    private static DownloadEngine instance;
    private final ExecutorService executor;
    private final List<DownloadItem> queue = new ArrayList<>();
    private final List<DownloadListener> listeners = new ArrayList<>();
    private final Map<DownloadItem, Process> activeProcesses = new ConcurrentHashMap<>();

    public interface DownloadListener {
        void onProgressUpdate(DownloadItem item);
        void onStatusChange(DownloadItem item);
        void onQueueChange();
    }

    private DownloadEngine() {
        int maxThreads = AppConfig.getInstance().getMaxConcurrentDownloads();
        executor = Executors.newFixedThreadPool(maxThreads);
    }

    public static DownloadEngine getInstance() {
        if (instance == null) instance = new DownloadEngine();
        return instance;
    }

    public void addListener(DownloadListener l) { listeners.add(l); }

    public List<DownloadItem> getQueue() { return Collections.unmodifiableList(queue); }

    public void addDownload(DownloadItem item) {
        queue.add(item);
        notifyQueueChange();
        executor.submit(() -> executeDownload(item));
    }

    public void cancelDownload(DownloadItem item) {
        Process p = activeProcesses.get(item);
        if (p != null) {
            p.destroyForcibly();
            item.setStatus(DownloadItem.Status.CANCELLED);
            notifyStatusChange(item);
        }
    }

    public void removeFromQueue(DownloadItem item) {
        cancelDownload(item);
        queue.remove(item);
        notifyQueueChange();
    }

    public void clearCompleted() {
        queue.removeIf(i ->
                i.getStatus() == DownloadItem.Status.COMPLETED ||
                        i.getStatus() == DownloadItem.Status.CANCELLED ||
                        i.getStatus() == DownloadItem.Status.ERROR
        );
        notifyQueueChange();
    }

    private void executeDownload(DownloadItem item) {
        try {
            item.setStatus(DownloadItem.Status.DOWNLOADING);
            notifyStatusChange(item);

            AppConfig cfg = AppConfig.getInstance();
            List<String> cmd = buildCommand(item, cfg);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeProcesses.put(item, process);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                parseOutputLine(line, item);
                notifyProgressUpdate(item);
            }

            int exitCode = process.waitFor();
            activeProcesses.remove(item);

            if (item.getStatus() != DownloadItem.Status.CANCELLED) {
                if (exitCode == 0) {
                    item.setStatus(DownloadItem.Status.COMPLETED);
                    item.setProgress(100);
                    item.setCompletedAt(java.time.LocalDateTime.now());
                } else {
                    item.setStatus(DownloadItem.Status.ERROR);
                    if (item.getErrorMessage() == null) {
                        item.setErrorMessage("yt-dlp retornou código de erro: " + exitCode);
                    }
                }
                notifyStatusChange(item);
            }

        } catch (IOException e) {
            item.setStatus(DownloadItem.Status.ERROR);
            item.setErrorMessage("Erro ao iniciar yt-dlp: " + e.getMessage() +
                    "\n\nVerifique se o yt-dlp está instalado e no PATH do sistema.\n" +
                    "Download em: https://github.com/yt-dlp/yt-dlp/releases");
            notifyStatusChange(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<String> buildCommand(DownloadItem item, AppConfig cfg) {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.getYtdlpPath());

        // ── FFmpeg location (usa local se não tiver no PATH) ──
        String ffmpegPath = FfmpegManager.getInstance().getFfmpegPath();
        if (ffmpegPath != null) {
            cmd.add("--ffmpeg-location");
            cmd.add(new java.io.File(ffmpegPath).getParent());
        }

        // ── IMPEDE download de playlist ──────────────────────
        cmd.add("--no-playlist");

        // ── Cookies ──────────────────────────────────────────
        if (cfg.isUseCookies() && !cfg.getCookiesPath().isEmpty()) {
            File cookiesFile = new File(cfg.getCookiesPath());
            if (cookiesFile.exists()) {
                cmd.add("--cookies");
                cmd.add(cfg.getCookiesPath());
            }
        }

        // ── Progresso parseável ───────────────────────────────
        cmd.add("--newline");
        cmd.add("--progress-template");
        cmd.add("%(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s|%(progress._total_bytes_str)s");

        // ── Pasta de saída ────────────────────────────────────
        String outputTemplate = item.getOutputDir() + File.separator + "%(title)s.%(ext)s";
        cmd.add("-o");
        cmd.add(outputTemplate);

        // ── Formato ───────────────────────────────────────────
        switch (item.getFormat()) {

            case VIDEO_MP4:
                // Força vídeo h264 + áudio aac → container mp4
                // Fallback progressivo caso o codec exato não exista
                cmd.add("-f");
                cmd.add("bestvideo[vcodec^=avc][ext=mp4]+bestaudio[ext=m4a]" +
                        "/bestvideo[ext=mp4]+bestaudio[ext=m4a]" +
                        "/bestvideo[ext=mp4]+bestaudio" +
                        "/bestvideo+bestaudio" +
                        "/best[ext=mp4]/best");
                cmd.add("--merge-output-format");
                cmd.add("mp4");
                // Força remux para mp4 sem transcodar se possível
                cmd.add("--recode-video");
                cmd.add("mp4");
                break;

            case VIDEO_BEST:
                cmd.add("-f");
                cmd.add("bestvideo+bestaudio/best");
                cmd.add("--merge-output-format");
                cmd.add("mkv");
                break;

            case AUDIO_MP3:
                // Baixa apenas áudio e converte para mp3
                cmd.add("-f");
                cmd.add("bestaudio/best");
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add("mp3");
                cmd.add("--audio-quality");
                cmd.add("0"); // qualidade máxima (VBR ~245kbps)
                break;

            case AUDIO_M4A:
                cmd.add("-f");
                cmd.add("bestaudio[ext=m4a]/bestaudio/best");
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add("m4a");
                break;

            case AUDIO_WAV:
                cmd.add("-f");
                cmd.add("bestaudio/best");
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add("wav");
                break;
        }

        // ── Metadados e thumbnail embarcados ──────────────────
        // --embed-thumbnail pode falhar se o ffmpeg não tiver suporte a webp,
        // então adicionamos --convert-thumbnails jpg para garantir compatibilidade
        cmd.add("--embed-thumbnail");
        cmd.add("--convert-thumbnails");
        cmd.add("jpg");
        cmd.add("--add-metadata");

        // ── URL (limpa parâmetros de playlist) ───────────────
        cmd.add(cleanUrl(item.getUrl()));

        return cmd;
    }


    /**
     * Remove parâmetros de playlist da URL, garantindo download de vídeo único.
     * Ex: https://youtu.be/ID?list=XYZ  →  https://www.youtube.com/watch?v=ID
     *     https://youtube.com/watch?v=ID&list=XYZ  →  https://www.youtube.com/watch?v=ID
     */
    private String cleanUrl(String url) {
        if (url == null || url.isEmpty()) return url;

        // Formato curto: youtu.be/VIDEO_ID?qualquercoisa
        if (url.contains("youtu.be/")) {
            String videoId = url.replaceAll(".*youtu\\.be/([\\w-]+).*", "$1");
            return "https://www.youtube.com/watch?v=" + videoId;
        }

        // Formato longo: youtube.com/watch?v=ID&list=...&index=...
        if (url.contains("youtube.com/watch")) {
            String videoId = null;
            String[] parts = url.split("[?&]");
            for (String part : parts) {
                if (part.startsWith("v=")) {
                    videoId = part.substring(2);
                    break;
                }
            }
            if (videoId != null && !videoId.isEmpty()) {
                return "https://www.youtube.com/watch?v=" + videoId;
            }
        }

        // Outros formatos: retorna sem parâmetros de playlist
        return url.replaceAll("[&?]list=[^&]*", "")
                .replaceAll("[&?]index=[^&]*", "")
                .replaceAll("[&?]start_radio=[^&]*", "");
    }

    private void parseOutputLine(String line, DownloadItem item) {

        // Captura destino do download (vídeo/áudio bruto)
        if (line.contains("[download] Destination:")) {
            String path = line.replace("[download] Destination:", "").trim();
            item.setSavedFilePath(path);
            setTitleFromPath(path, item);
            return;
        }

        // Captura arquivo final após conversão de áudio
        if (line.contains("[ExtractAudio] Destination:")) {
            String path = line.replace("[ExtractAudio] Destination:", "").trim();
            item.setSavedFilePath(path);
            setTitleFromPath(path, item);
            item.setStatus(DownloadItem.Status.CONVERTING);
            item.setProgress(99);
            notifyStatusChange(item);
            return;
        }

        // Captura arquivo final após merge de vídeo+áudio
        if (line.contains("[Merger] Merging formats into")) {
            String path = line.replaceAll(".*\"(.+)\".*", "$1").trim();
            if (!path.isEmpty() && !path.equals(line)) {
                item.setSavedFilePath(path);
                setTitleFromPath(path, item);
            }
            item.setStatus(DownloadItem.Status.CONVERTING);
            item.setProgress(99);
            notifyStatusChange(item);
            return;
        }

        // Captura fase de conversão de vídeo
        if (line.contains("[VideoConvertor]")) {
            item.setStatus(DownloadItem.Status.CONVERTING);
            item.setProgress(99);
            notifyStatusChange(item);
            return;
        }

        // Parseia progresso: %|speed|eta|size
        if (line.matches("\\s*\\d+\\.\\d+%.*")) {
            try {
                String[] parts = line.trim().split("\\|");
                if (parts.length >= 1) {
                    String pct = parts[0].replace("%", "").trim();
                    int progress = (int) Double.parseDouble(pct);
                    item.setProgress(Math.min(progress, 99));
                }
                if (parts.length >= 2) item.setSpeed(parts[1].trim());
                if (parts.length >= 3) item.setEta(parts[2].trim());
                if (parts.length >= 4) item.setFileSize(parts[3].trim());
            } catch (NumberFormatException ignored) {}
            return;
        }

        // Captura erros
        if (line.startsWith("ERROR:")) {
            item.setErrorMessage(line.replace("ERROR:", "").trim());
        }
    }
    private void setTitleFromPath(String path, DownloadItem item) {
        String name = new File(path).getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        if (!name.isEmpty()) item.setTitle(name);
    }

    private void notifyProgressUpdate(DownloadItem item) {
        SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.onProgressUpdate(item)));
    }

    private void notifyStatusChange(DownloadItem item) {
        SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.onStatusChange(item)));
    }

    private void notifyQueueChange() {
        SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.onQueueChange()));
    }

    public void shutdown() {
        activeProcesses.values().forEach(Process::destroyForcibly);
        executor.shutdownNow();
    }
}