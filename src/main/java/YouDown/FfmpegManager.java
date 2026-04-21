package YouDown;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;

public class FfmpegManager {

    private static final String FFMPEG_WIN_URL =
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";

    private static FfmpegManager instance;

    private FfmpegManager() {}

    public static FfmpegManager getInstance() {
        if (instance == null) instance = new FfmpegManager();
        return instance;
    }

    // ── Localização dinâmica: mesma pasta do YouDown.exe ─────────────────────

    /**
     * Retorna a pasta raiz onde o YouDown está sendo executado.
     * Mesmo comportamento do yt-dlp: busca na pasta do próprio executável.
     */
    public static String getAppDir() {
        try {
            // Quando rodando como JAR/EXE, pega a pasta do arquivo
            String path = FfmpegManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            // Remove barra inicial no Windows (/C:/... → C:/...)
            if (isWindows() && path.startsWith("/")) path = path.substring(1);

            File f = new File(path);
            // Se for um .jar ou .exe, retorna a pasta pai
            if (f.isFile()) return f.getParent();
            // Se for pasta (IDE), retorna ela mesma
            return f.getAbsolutePath();
        } catch (Exception e) {
            // Fallback: diretório de trabalho atual
            return System.getProperty("user.dir");
        }
    }

    /**
     * Pasta onde o ffmpeg.exe será instalado: raiz do YouDown.
     */
    public String getFfmpegDir() {
        return getAppDir();
    }

    /**
     * Arquivo ffmpeg.exe dentro da pasta do YouDown.
     */
    private File getLocalExeFile() {
        return new File(getFfmpegDir(), isWindows() ? "ffmpeg.exe" : "ffmpeg");
    }

    // ── Verificações ─────────────────────────────────────────────────────────

    /**
     * Verifica se o ffmpeg.exe na pasta do YouDown é válido (> 1MB).
     */
    public boolean isLocalInstallValid() {
        File exe = getLocalExeFile();
        return exe.exists() && exe.length() > 1_000_000;
    }

    /**
     * Verifica se o ffmpeg está no PATH do sistema.
     */
    public boolean isInPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * FFmpeg disponível se estiver na pasta local OU no PATH.
     */
    public boolean isAvailable() {
        return isLocalInstallValid() || isInPath();
    }

    /**
     * Retorna versão do ffmpeg (local tem prioridade sobre PATH).
     */
    public String getVersion() {
        String cmd = isLocalInstallValid()
                ? getLocalExeFile().getAbsolutePath()
                : "ffmpeg";
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            if (line != null && line.startsWith("ffmpeg version")) {
                String[] parts = line.split("\\s+");
                return parts.length >= 3 ? parts[2] : line;
            }
        } catch (Exception ignored) {}
        return "Desconhecida";
    }

    /**
     * Caminho do ffmpeg.exe para passar ao yt-dlp via --ffmpeg-location.
     * Retorna null se estiver apenas no PATH (yt-dlp acha sozinho).
     */
    public String getFfmpegPath() {
        if (isLocalInstallValid()) return getLocalExeFile().getAbsolutePath();
        return null; // yt-dlp usa o PATH
    }

    // ── PATH do Windows ───────────────────────────────────────────────────────

    /**
     * Verifica se a pasta do YouDown já está no PATH do sistema (MACHINE).
     */
    public boolean isInSystemPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-Command",
                    "[System.Environment]::GetEnvironmentVariable('Path','Machine')"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String currentPath = br.readLine();
            p.waitFor();
            if (currentPath == null) return false;
            String appDir = getFfmpegDir().replace("/", "\\");
            for (String entry : currentPath.split(";")) {
                if (entry.trim().equalsIgnoreCase(appDir)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Adiciona a pasta do YouDown ao PATH do sistema (HKLM - requer admin).
     * Usa PowerShell + SetEnvironmentVariable para persistir no registro.
     * @return true se adicionado com sucesso
     */
    public boolean addToSystemPath() {
        if (!isWindows()) return false;
        if (isInSystemPath()) return true; // já está, não duplica

        try {
            String appDir = getFfmpegDir().replace("/", "\\");

            // Lê PATH atual, adiciona a pasta, salva de volta
            String psCommand =
                    "$oldPath = [System.Environment]::GetEnvironmentVariable('Path','Machine');" +
                            "$newEntry = '" + appDir.replace("'", "''") + "';" +
                            "if ($oldPath -notlike ('*' + $newEntry + '*')) {" +
                            "  $newPath = $oldPath.TrimEnd(';') + ';' + $newEntry;" +
                            "  [System.Environment]::SetEnvironmentVariable('Path',$newPath,'Machine');" +
                            "  Write-Output 'OK';" +
                            "} else { Write-Output 'ALREADY'; }";

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", psCommand
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String result = br.readLine();
            p.waitFor();

            return "OK".equalsIgnoreCase(result != null ? result.trim() : "") ||
                    "ALREADY".equalsIgnoreCase(result != null ? result.trim() : "");
        } catch (Exception e) {
            System.err.println("Erro ao adicionar ao PATH: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a pasta do YouDown do PATH do sistema.
     */
    public boolean removeFromSystemPath() {
        if (!isWindows()) return false;
        try {
            String appDir = getFfmpegDir().replace("/", "\\");
            String psCommand =
                    "$oldPath = [System.Environment]::GetEnvironmentVariable('Path','Machine');" +
                            "$entries = $oldPath -split ';' | Where-Object { $_.Trim() -ne '" +
                            appDir.replace("'", "''") + "' };" +
                            "$newPath = $entries -join ';';" +
                            "[System.Environment]::SetEnvironmentVariable('Path',$newPath,'Machine');" +
                            "Write-Output 'OK';";

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", psCommand
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String result = br.readLine();
            p.waitFor();
            return "OK".equalsIgnoreCase(result != null ? result.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    public void checkOnStartup(JFrame parent) {
        SwingWorker<Boolean, Void> checker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() { return isAvailable(); }
            @Override protected void done() {
                try { if (!get()) showFfmpegWarning(parent); }
                catch (Exception ignored) {}
            }
        };
        checker.execute();
    }

    private void showFfmpegWarning(JFrame parent) {
        String appDir = getFfmpegDir();
        String msg = "<html><body style='width:400px;font-family:Segoe UI;font-size:12px'>" +
                "<b style='font-size:14px;color:#FF4444'>⚠ FFmpeg não encontrado</b><br><br>" +
                "O <b>FFmpeg</b> é necessário para:<br>" +
                "• Converter vídeos para MP4<br>" +
                "• Converter áudio para MP3, M4A ou WAV<br><br>";

        if (isWindows()) {
            msg += "Deseja baixar e instalar o FFmpeg automaticamente?<br>" +
                    "<small style='color:#888'>Será instalado em: <b>" + appDir + "</b><br>" +
                    "e adicionado ao PATH do Windows.</small>";
        } else {
            msg += "Instale com:<br>" +
                    "<code>sudo apt install ffmpeg</code> (Linux)<br>" +
                    "<code>brew install ffmpeg</code> (macOS)";
        }
        msg += "</body></html>";

        if (isWindows()) {
            int choice = JOptionPane.showOptionDialog(parent, new JLabel(msg),
                    "FFmpeg necessário", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, null,
                    new String[]{"📥 Instalar automaticamente", "Depois"},
                    "📥 Instalar automaticamente");
            if (choice == JOptionPane.YES_OPTION)
                downloadFfmpegWithProgress(parent, null);
        } else {
            JOptionPane.showMessageDialog(parent, new JLabel(msg),
                    "FFmpeg necessário", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── Download / Instalação ─────────────────────────────────────────────────

    public void downloadFfmpegWithProgress(JFrame parent, Runnable onDone) {
        JDialog dialog = new JDialog(parent, "Instalando FFmpeg...", true);
        dialog.setSize(480, 185);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel statusLabel = new JLabel("Iniciando...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(420, 22));

        JLabel sizeLabel = new JLabel(" ", SwingConstants.CENTER);
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(new Color(140, 140, 140));

        JLabel pathLabel = new JLabel(
                "<html><center><font color='#666666'>Destino: " + getFfmpegDir() + "</font></center></html>",
                SwingConstants.CENTER);
        pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JButton cancelBtn = new JButton("Cancelar");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(pathLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(bar);
        panel.add(Box.createVerticalStrut(4));
        panel.add(sizeLabel);
        panel.add(Box.createVerticalStrut(8));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnRow.add(cancelBtn);
        panel.add(btnRow);
        dialog.add(panel);

        SwingWorker<String, Object[]> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                String appDir = getFfmpegDir();

                // ── 1. Remove exe corrompido anterior ──
                publish(new Object[]{"Preparando instalação...", 2, ""});
                File exeFile = getLocalExeFile();
                if (exeFile.exists()) exeFile.delete();
                Files.createDirectories(Paths.get(appDir));

                // ── 2. Download com progresso ──
                publish(new Object[]{"Conectando ao servidor...", 5, ""});
                File zipFile = new File(appDir, "ffmpeg_download.zip");

                URL url = new URL(FFMPEG_WIN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setFollowRedirects(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(60_000);
                conn.setRequestProperty("User-Agent", "YouDown/1.0");

                long total = conn.getContentLengthLong();
                long downloaded = 0;

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(zipFile)) {
                    byte[] buffer = new byte[32768];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (isCancelled()) { zipFile.delete(); return "CANCELLED"; }
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (total > 0) {
                            int pct = (int) ((downloaded / (double) total) * 78) + 5;
                            long dlMb  = downloaded / (1024 * 1024);
                            long totMb = total / (1024 * 1024);
                            publish(new Object[]{"Baixando FFmpeg...", pct, dlMb + " MB / " + totMb + " MB"});
                        }
                    }
                }

                // ── 3. Valida ZIP ──
                publish(new Object[]{"Verificando arquivo baixado...", 84, ""});
                if (zipFile.length() < 10_000_000) {
                    zipFile.delete();
                    throw new IOException("Arquivo corrompido ou incompleto (" +
                            zipFile.length() / 1024 + " KB). Verifique sua conexão.");
                }

                // ── 4. Extração ──
                publish(new Object[]{"Extraindo ffmpeg.exe...", 88, ""});
                boolean extracted = extractFfmpegFromZip(zipFile);

                // ── 5. Limpeza do ZIP ──
                publish(new Object[]{"Removendo arquivos temporários...", 93, ""});
                zipFile.delete();

                if (!extracted) throw new IOException(
                        "ffmpeg.exe não encontrado no ZIP. Tente novamente.");

                if (!isLocalInstallValid()) throw new IOException(
                        "ffmpeg.exe extraído está inválido.\n" +
                                "Verifique se o YouDown está rodando como Administrador.\n" +
                                "Pasta: " + appDir);

                // ── 6. Adiciona ao PATH do Windows ──
                publish(new Object[]{"Adicionando ao PATH do Windows...", 96, ""});
                boolean pathAdded = addToSystemPath();

                publish(new Object[]{"✅ Concluído!", 100, ""});
                return pathAdded ? "OK_WITH_PATH" : "OK_NO_PATH";
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                if (chunks.isEmpty()) return;
                Object[] last = chunks.get(chunks.size() - 1);
                statusLabel.setText((String) last[0]);
                bar.setValue((Integer) last[1]);
                sizeLabel.setText((String) last[2]);
            }

            @Override
            protected void done() {
                dialog.dispose();
                try {
                    String result = get();
                    if ("CANCELLED".equals(result)) return;

                    boolean pathOk = "OK_WITH_PATH".equals(result);

                    // Mostra sucesso e avisa que vai reiniciar
                    JOptionPane.showMessageDialog(parent,
                            "<html><b>✅ FFmpeg instalado com sucesso!</b><br><br>" +
                                    "Versão: " + getVersion() + "<br>" +
                                    "Local: " + getFfmpegDir() + "<br><br>" +
                                    (pathOk
                                            ? "✅ Adicionado ao PATH do Windows.<br>"
                                            : "⚠ Não foi possível adicionar ao PATH.<br>") +
                                    "<br><b>O YouDown será reiniciado automaticamente.</b></html>",
                            "FFmpeg instalado", JOptionPane.INFORMATION_MESSAGE);

                    // Notifica o callback (ex: atualizar painel) e reinicia
                    if (onDone != null) onDone.run();
                    closeApp(parent);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent,
                            "❌ Falha na instalação:\n\n" + e.getMessage() +
                                    "\n\nCertifique-se que o YouDown está rodando como Administrador.",
                            "Erro na instalação", JOptionPane.ERROR_MESSAGE);
                    if (onDone != null) onDone.run();
                }
            }
        };

        cancelBtn.addActionListener(e -> { worker.cancel(true); dialog.dispose(); });
        worker.execute();
        dialog.setVisible(true);
    }


    /**
     * Informa o usuário para abrir o YouDown novamente e fecha o app.
     */
    public static void closeApp(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
                "<html><b>✅ Instalação concluída!</b><br><br>" +
                        "Por favor, <b>abra o YouDown novamente</b><br>" +
                        "para que as alterações sejam aplicadas.</html>",
                "Fechar e reabrir", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private boolean extractFfmpegFromZip(File zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace("\\", "/");
                if (name.endsWith("/bin/ffmpeg.exe") || name.equals("ffmpeg.exe")) {
                    File dest = getLocalExeFile();
                    try (FileOutputStream fos = new FileOutputStream(dest)) {
                        byte[] buf = new byte[32768];
                        int read;
                        while ((read = zis.read(buf)) != -1) fos.write(buf, 0, read);
                    }
                    return true;
                }
                zis.closeEntry();
            }
        }
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}