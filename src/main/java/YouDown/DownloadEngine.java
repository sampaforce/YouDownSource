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
        // Marca o início para distinguir as capas geradas por este download
        // das imagens que o usuário já tinha na pasta
        long startedAt = System.currentTimeMillis();
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
                // Com --ignore-errors o yt-dlp retorna código != 0 mesmo quando
                // parte da playlist baixou: conta como concluída se algo saiu.
                boolean partialPlaylist = item.isPlaylist()
                        && exitCode != 0
                        && item.getSavedFilePath() != null;

                if (exitCode == 0 || partialPlaylist) {
                    item.setStatus(DownloadItem.Status.COMPLETED);
                    item.setProgress(100);
                    item.setCompletedAt(java.time.LocalDateTime.now());
                } else {
                    item.setStatus(DownloadItem.Status.ERROR);
                    if (item.getErrorMessage() == null) {
                        item.setErrorMessage(item.isPlaylist() && item.getFailedItems() > 0
                                ? "Nenhum item da playlist pôde ser baixado (" +
                                  item.getFailedItems() + " falha(s)).\n" +
                                  "Playlists privadas exigem cookies — configure em Configurações."
                                : "yt-dlp retornou código de erro: " + exitCode);
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
        } finally {
            cleanupLeftoverImages(item, startedAt);
        }
    }

    /**
     * Apaga as capas que o yt-dlp deixou para trás quando um item falha.
     * Conservador de propósito: só remove arquivos de imagem dentro da pasta
     * deste download que foram criados durante ele — imagens que já estavam
     * na pasta antes de começar não são tocadas.
     */
    private void cleanupLeftoverImages(DownloadItem item, long startedAt) {
        Set<File> dirs = new LinkedHashSet<>();
        File base = new File(item.getOutputDir());
        dirs.add(base);

        // Subpasta da playlist: vem do arquivo salvo ou, se tudo falhou,
        // das subpastas tocadas por este download
        if (item.getSavedFilePath() != null) {
            File parent = new File(item.getSavedFilePath()).getParentFile();
            if (parent != null) dirs.add(parent);
        }
        File[] subs = base.listFiles(File::isDirectory);
        if (subs != null) {
            for (File d : subs) {
                if (d.lastModified() >= startedAt) dirs.add(d);
            }
        }

        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isFile() && isImageFile(f.getName()) && f.lastModified() >= startedAt) {
                    f.delete();
                }
            }
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

        // ── Playlist: baixa tudo ou apenas o vídeo único ─────
        if (item.isPlaylist()) {
            cmd.add("--yes-playlist");
            // Um item quebrado (privado/removido) não pode derrubar a playlist
            cmd.add("--ignore-errors");

            // O YouTube entrega a playlist em páginas de 100. Lendo pela página
            // web a continuação falha e a playlist chega truncada no item 100
            // (uma de 153 virava 100). Forçar a API do YouTube pagina até o fim.
            cmd.add("--extractor-args");
            cmd.add("youtubetab:skip=webpage");

            // Permite completar uma playlist já baixada pela metade sem refazer
            // o que existe: arquivo presente é pulado.
            cmd.add("--no-overwrites");

            if (item.getPlaylistRange() != null && !item.getPlaylistRange().isEmpty()) {
                cmd.add("--playlist-items");
                cmd.add(item.getPlaylistRange());
            }
        } else {
            cmd.add("--no-playlist");
        }

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
        String outputTemplate;
        if (item.isPlaylist()) {
            String prefix = item.isCreateSubfolder()
                    ? "%(playlist_title|Playlist)s" + File.separator
                    : "";
            outputTemplate = item.getOutputDir() + File.separator + prefix
                    + "%(playlist_index)03d - %(title)s.%(ext)s";
        } else {
            outputTemplate = item.getOutputDir() + File.separator + "%(title)s.%(ext)s";
        }
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

        // ── Metadados e capa embarcados ───────────────────────
        // A capa é apenas EMBUTIDA no arquivo final. O yt-dlp grava a imagem em
        // disco (com o nome do arquivo de mídia) antes de embutir e a apaga
        // sozinho quando o item termina bem; quando o item falha, ela fica
        // órfã — daí a limpeza em cleanupLeftoverImages().
        // --convert-thumbnails jpg evita falha de embed com thumbnails webp.
        cmd.add("--embed-thumbnail");
        cmd.add("--convert-thumbnails");
        cmd.add("jpg");
        cmd.add("--add-metadata");

        // ── URL ───────────────────────────────────────────────
        cmd.add(item.isPlaylist()
                ? playlistUrl(item.getUrl())
                : cleanUrl(item.getUrl()));

        return cmd;
    }

    /**
     * Detecta se a URL aponta para uma playlist, canal ou aba de vídeos.
     * Ex: youtube.com/playlist?list=XYZ, youtube.com/watch?v=ID&list=XYZ,
     *     youtube.com/@canal/videos
     */
    public static boolean isPlaylistUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String u = url.toLowerCase();

        if (u.contains("list=") || u.contains("/playlist")) return true;

        // Canais: /@nome, /channel/ID, /c/nome, /user/nome
        return u.matches(".*youtube\\.com/(@[^/?]+|channel/|c/|user/).*");
    }

    /**
     * Normaliza a URL para o download da playlist inteira, independentemente
     * do índice do vídeo colado pelo usuário.
     * Ex: youtube.com/watch?v=ID&list=XYZ  →  youtube.com/playlist?list=XYZ
     * URLs de canal são mantidas como estão.
     */
    private String playlistUrl(String url) {
        if (url == null || url.isEmpty()) return url;

        String[] parts = url.split("[?&]");
        for (String part : parts) {
            if (part.startsWith("list=")) {
                String listId = part.substring(5);
                if (!listId.isEmpty()) {
                    return "https://www.youtube.com/playlist?list=" + listId;
                }
            }
        }
        return url;
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

        // ── Playlist: nome ────────────────────────────────────
        if (line.contains("[download] Downloading playlist:")) {
            String name = line.substring(line.indexOf("playlist:") + 9).trim();
            if (!name.isEmpty()) {
                item.setPlaylistTitle(name);
                item.setTitle(name);
                notifyStatusChange(item);
            }
            return;
        }

        // ── Playlist: total de itens ──────────────────────────
        // Ex: "[youtube:tab] Playlist Nome: Downloading 30 items of 30"
        java.util.regex.Matcher total =
                java.util.regex.Pattern.compile("Downloading (\\d+) items of (\\d+)").matcher(line);
        if (total.find()) {
            item.setPlaylistTotal(Integer.parseInt(total.group(1)));
            notifyStatusChange(item);
            return;
        }

        // ── Playlist: item atual ──────────────────────────────
        // Ex: "[download] Downloading item 3 of 30" (ou "video 3 of 30")
        java.util.regex.Matcher current =
                java.util.regex.Pattern.compile("Downloading (?:item|video) (\\d+) of (\\d+)").matcher(line);
        if (current.find()) {
            item.setPlaylistIndex(Integer.parseInt(current.group(1)));
            item.setPlaylistTotal(Integer.parseInt(current.group(2)));
            item.setStatus(DownloadItem.Status.DOWNLOADING);
            notifyStatusChange(item);
            return;
        }

        // Captura destino do download (vídeo/áudio bruto)
        if (line.contains("[download] Destination:")) {
            String path = line.replace("[download] Destination:", "").trim();
            // Capas não são o arquivo do usuário: não viram título nem
            // "arquivo salvo" (senão uma playlist só de falhas pareceria OK)
            if (isImageFile(path)) return;
            item.setSavedFilePath(path);
            setTitleFromPath(path, item);
            return;
        }

        // Captura arquivo final após conversão de áudio
        if (line.contains("[ExtractAudio] Destination:")) {
            String path = line.replace("[ExtractAudio] Destination:", "").trim();
            item.setSavedFilePath(path);
            setTitleFromPath(path, item);
            markConverting(item);
            return;
        }

        // Captura arquivo final após merge de vídeo+áudio
        if (line.contains("[Merger] Merging formats into")) {
            String path = line.replaceAll(".*\"(.+)\".*", "$1").trim();
            if (!path.isEmpty() && !path.equals(line)) {
                item.setSavedFilePath(path);
                setTitleFromPath(path, item);
            }
            markConverting(item);
            return;
        }

        // Captura fase de conversão de vídeo
        if (line.contains("[VideoConvertor]")) {
            markConverting(item);
            return;
        }

        // Parseia progresso: %|speed|eta|size
        if (line.matches("\\s*\\d+\\.\\d+%.*")) {
            try {
                String[] parts = line.trim().split("\\|");
                if (parts.length >= 1) {
                    String pct = parts[0].replace("%", "").trim();
                    int progress = (int) Double.parseDouble(pct);
                    item.setProgress(overallProgress(item, progress));
                }
                if (parts.length >= 2) item.setSpeed(parts[1].trim());
                if (parts.length >= 3) item.setEta(parts[2].trim());
                if (parts.length >= 4) item.setFileSize(parts[3].trim());
            } catch (NumberFormatException ignored) {}
            return;
        }

        // Captura erros
        if (line.startsWith("ERROR:")) {
            // Numa playlist um item com erro é apenas contabilizado:
            // o --ignore-errors mantém o restante da fila rodando
            if (item.isPlaylist()) {
                item.incrementFailedItems();
            } else {
                item.setErrorMessage(line.replace("ERROR:", "").trim());
            }
        }
    }

    private static boolean isImageFile(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".jpg")  || p.endsWith(".jpeg") || p.endsWith(".png")
                || p.endsWith(".webp") || p.endsWith(".gif");
    }

    private void markConverting(DownloadItem item) {
        item.setStatus(DownloadItem.Status.CONVERTING);
        item.setProgress(overallProgress(item, 99));
        notifyStatusChange(item);
    }

    /**
     * Converte o progresso do item atual em progresso geral.
     * Vídeo único: o próprio percentual (limitado a 99%).
     * Playlist: (itens concluídos * 100 + percentual atual) / total.
     */
    private int overallProgress(DownloadItem item, int itemProgress) {
        if (!item.isPlaylist() || item.getPlaylistTotal() <= 0) {
            return Math.min(itemProgress, 99);
        }
        int done = Math.max(item.getPlaylistIndex() - 1, 0);
        int overall = (int) ((done * 100.0 + itemProgress) / item.getPlaylistTotal());
        return Math.min(overall, 99);
    }

    private void setTitleFromPath(String path, DownloadItem item) {
        String name = new File(path).getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        if (name.isEmpty()) return;

        // Na playlist o título principal é o da playlist; o arquivo é o item atual
        if (item.isPlaylist()) {
            item.setCurrentItemTitle(name);
            if (item.getPlaylistTitle() == null) item.setTitle(name);
        } else {
            item.setTitle(name);
        }
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