package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;

public class SettingsPanel extends JPanel {

    private JTextField ytdlpPathField;
    private JTextField cookiesPathField;
    private JCheckBox  useCookiesCheck;
    private JTextField defaultDirField;
    private JComboBox<Integer> maxDownloadsCombo;
    private JComboBox<String>  defaultFormatCombo;
    private JPanel ffmpegSectionContent;
    private JButton updateBtn;
    private JCheckBox autoUpdateCheck;
    private JComboBox<Integer> autoUpdateDaysCombo;
    private JLabel lastCheckLabel;

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Configurações");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(255, 68, 68));
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(title);
        mainPanel.add(buildSection("⚙ yt-dlp",               buildYtdlpSection()));
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(buildSection("🎬 FFmpeg",               buildFfmpegSection()));
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(buildSection("🍪 Cookies do YouTube",   buildCookiesSection()));
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(buildSection("📁 Pasta padrão",         buildDirSection()));
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(buildSection("⬇ Downloads",             buildDownloadSection()));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buildSaveButton());

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        loadValues();
    }

    // ── Seções genéricas ──────────────────────────────────────────────────────

    private JPanel buildSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 55)),
                new EmptyBorder(14, 14, 14, 14)));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(200, 200, 200));
        lbl.setBorder(new EmptyBorder(0, 0, 10, 0));

        section.add(lbl, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(section);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    // ── yt-dlp ────────────────────────────────────────────────────────────────

    private JPanel buildYtdlpSection() {
        ytdlpPathField = new JTextField();
        ytdlpPathField.putClientProperty("JTextField.placeholderText",
                "yt-dlp (se estiver no PATH) ou caminho completo");

        JButton browseBtn = new JButton("📂");
        browseBtn.setPreferredSize(new Dimension(40, 32));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Localizar yt-dlp");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                ytdlpPathField.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JButton testBtn = new JButton("🔍 Testar");
        testBtn.addActionListener(e -> testYtdlp());

        updateBtn = new JButton("⬆ Atualizar");
        updateBtn.setToolTipText("Baixa a última versão do yt-dlp (yt-dlp -U)");
        updateBtn.addActionListener(e -> updateYtdlp());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.add(browseBtn);
        btns.add(testBtn);
        btns.add(updateBtn);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(new JLabel("Caminho: "), BorderLayout.WEST);
        row.add(ytdlpPathField, BorderLayout.CENTER);
        row.add(btns, BorderLayout.EAST);

        // ── Atualização automática ──
        autoUpdateCheck = new JCheckBox("Manter o yt-dlp atualizado automaticamente");
        autoUpdateCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        autoUpdateCheck.setToolTipText(
                "Verifica na abertura do YouDown, respeitando o intervalo abaixo");

        autoUpdateDaysCombo = new JComboBox<>(new Integer[]{1, 3, 7, 15, 30});
        autoUpdateDaysCombo.setPreferredSize(new Dimension(70, 28));

        JLabel everyLabel = new JLabel("Verificar a cada ");
        everyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel daysLabel = new JLabel(" dia(s)");
        daysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lastCheckLabel = new JLabel();
        lastCheckLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lastCheckLabel.setForeground(new Color(136, 136, 136));

        JPanel everyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        everyRow.add(everyLabel);
        everyRow.add(autoUpdateDaysCombo);
        everyRow.add(daysLabel);
        everyRow.add(Box.createHorizontalStrut(12));
        everyRow.add(lastCheckLabel);

        autoUpdateCheck.addActionListener(e -> {
            boolean on = autoUpdateCheck.isSelected();
            autoUpdateDaysCombo.setEnabled(on);
            everyLabel.setEnabled(on);
            daysLabel.setEnabled(on);
        });

        JPanel autoBox = new JPanel();
        autoBox.setLayout(new BoxLayout(autoBox, BoxLayout.Y_AXIS));
        autoBox.setBorder(new EmptyBorder(10, 0, 0, 0));
        autoUpdateCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        everyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoBox.add(autoUpdateCheck);
        autoBox.add(Box.createVerticalStrut(4));
        autoBox.add(everyRow);

        JLabel hint = new JLabel("<html><font color='#888888'>O YouTube muda a extração a cada " +
                "poucas semanas: um yt-dlp antigo causa erro 403 e playlists incompletas.</font></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel sul = new JPanel(new BorderLayout());
        sul.add(autoBox, BorderLayout.NORTH);
        sul.add(hint,    BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout());
        p.add(row, BorderLayout.CENTER);
        p.add(sul, BorderLayout.SOUTH);
        return p;
    }

    /** "Última verificação: 08/09/2026 11:57" ou "nunca". */
    private void refreshLastCheckLabel() {
        long ts = AppConfig.getInstance().getLastYtdlpCheck();
        lastCheckLabel.setText("Última verificação: " + (ts <= 0
                ? "nunca"
                : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(ts))));
    }

    // ── FFmpeg ────────────────────────────────────────────────────────────────

    private JPanel buildFfmpegSection() {
        ffmpegSectionContent = new JPanel();
        ffmpegSectionContent.setLayout(new BoxLayout(ffmpegSectionContent, BoxLayout.Y_AXIS));
        refreshFfmpegSection();
        return ffmpegSectionContent;
    }

    private void refreshFfmpegSection() {
        ffmpegSectionContent.removeAll();

        FfmpegManager fm       = FfmpegManager.getInstance();
        boolean localValid     = fm.isLocalInstallValid();
        boolean inPath         = fm.isInPath();
        boolean available      = localValid || inPath;
        boolean inSysPath      = fm.isInSystemPath();
        boolean isWindows      = System.getProperty("os.name").toLowerCase().contains("win");
        String  appDir         = FfmpegManager.getAppDir();

        // ── Status do executável ──────────────────────────────
        String statusText;
        Color  statusColor;
        String detail;

        if (localValid) {
            statusText  = "✅ FFmpeg instalado na pasta do YouDown";
            statusColor = new Color(80, 200, 120);
            detail      = "Versão: " + fm.getVersion() + "   Pasta: " + appDir;
        } else if (inPath) {
            statusText  = "✅ FFmpeg encontrado no PATH do sistema";
            statusColor = new Color(80, 200, 120);
            detail      = "Versão: " + fm.getVersion() + "  (externo ao YouDown)";
        } else {
            File corruptExe = new File(appDir, "ffmpeg.exe");
            if (corruptExe.exists() && corruptExe.length() <= 1_000_000) {
                statusText  = "⚠ FFmpeg corrompido / instalação incompleta";
                statusColor = new Color(255, 180, 0);
                detail      = "Arquivo inválido (" + (corruptExe.length() / 1024) +
                        " KB) — Reinstale como Administrador.";
            } else {
                statusText  = "❌ FFmpeg não encontrado";
                statusColor = new Color(220, 80, 80);
                detail      = "Necessário para converter vídeos e áudio.";
            }
        }

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(statusColor);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLabel = new JLabel("<html><font color='#777777'>" + detail + "</font></html>");
        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 6, 0));
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ffmpegSectionContent.add(statusLabel);
        ffmpegSectionContent.add(detailLabel);

        if (isWindows) {
            // ── Status do PATH ────────────────────────────────
            JLabel pathLabel = new JLabel(
                    inSysPath ? "✅ Pasta do YouDown está no PATH do Windows"
                            : "⚠ Pasta do YouDown NÃO está no PATH do Windows");
            pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            pathLabel.setForeground(inSysPath ? new Color(80, 200, 120) : new Color(255, 180, 0));
            pathLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            ffmpegSectionContent.add(pathLabel);

            // ── Botões ────────────────────────────────────────
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (!available) {
                // Instalar
                JButton installBtn = new JButton("📥 Instalar FFmpeg");
                installBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                installBtn.setBackground(new Color(255, 68, 68));
                installBtn.setForeground(Color.WHITE);
                installBtn.addActionListener(e -> startFfmpegInstall());
                btnRow.add(installBtn);

            } else {
                // Reinstalar
                JButton reinstallBtn = new JButton("🔄 Reinstalar FFmpeg");
                reinstallBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                reinstallBtn.setToolTipText("Use se o FFmpeg estiver corrompido ou incompleto");
                reinstallBtn.addActionListener(e -> {
                    int c = JOptionPane.showConfirmDialog(
                            SwingUtilities.getWindowAncestor(ffmpegSectionContent),
                            "<html>Deseja <b>reinstalar</b> o FFmpeg?<br>" +
                                    "O arquivo atual será removido e baixado novamente.<br><br>" +
                                    "<small>Use se houve erro na instalação anterior (ex: sem admin).</small></html>",
                            "Confirmar reinstalação",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (c == JOptionPane.YES_OPTION) startFfmpegInstall();
                });
                btnRow.add(reinstallBtn);

                // Adicionar / Remover do PATH
                if (!inSysPath) {
                    JButton addPathBtn = new JButton("➕ Adicionar ao PATH");
                    addPathBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    addPathBtn.setToolTipText("Adiciona a pasta do YouDown ao PATH do Windows");
                    addPathBtn.addActionListener(e -> {
                        boolean ok = FfmpegManager.getInstance().addToSystemPath();
                        if (ok) {
                            JOptionPane.showMessageDialog(
                                    SwingUtilities.getWindowAncestor(ffmpegSectionContent),
                                    "<html><b>✅ Adicionado ao PATH do Windows!</b><br><br>" +
                                            "Pasta: " + appDir + "<br><br>" +
                                            "<b>O YouDown será reiniciado para aplicar as mudanças.</b></html>",
                                    "PATH atualizado", JOptionPane.INFORMATION_MESSAGE);
                            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(SettingsPanel.this);
                            FfmpegManager.closeApp(parent);
                        } else {
                            JOptionPane.showMessageDialog(
                                    SwingUtilities.getWindowAncestor(ffmpegSectionContent),
                                    "❌ Falha ao adicionar ao PATH.\n" +
                                            "Certifique-se que o YouDown está rodando como Administrador.",
                                    "Erro", JOptionPane.ERROR_MESSAGE);
                            SwingUtilities.invokeLater(this::refreshFfmpegSection);
                        }
                    });
                    btnRow.add(addPathBtn);
                } else {
                    JButton removePathBtn = new JButton("➖ Remover do PATH");
                    removePathBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    removePathBtn.setForeground(new Color(180, 100, 100));
                    removePathBtn.setToolTipText("Remove a pasta do YouDown do PATH do Windows");
                    removePathBtn.addActionListener(e -> {
                        int c = JOptionPane.showConfirmDialog(
                                SwingUtilities.getWindowAncestor(ffmpegSectionContent),
                                "Remover a pasta do YouDown do PATH do Windows?",
                                "Confirmar", JOptionPane.YES_NO_OPTION);
                        if (c == JOptionPane.YES_OPTION) {
                            FfmpegManager.getInstance().removeFromSystemPath();
                            SwingUtilities.invokeLater(this::refreshFfmpegSection);
                        }
                    });
                    btnRow.add(removePathBtn);
                }
            }

            ffmpegSectionContent.add(btnRow);

        } else {
            JLabel linuxHint = new JLabel(
                    "<html><font color='#AAAAAA'>Linux: <b>sudo apt install ffmpeg</b>" +
                            " &nbsp;|&nbsp; macOS: <b>brew install ffmpeg</b></font></html>");
            linuxHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            linuxHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            ffmpegSectionContent.add(linuxHint);
        }

        ffmpegSectionContent.revalidate();
        ffmpegSectionContent.repaint();
    }

    private void startFfmpegInstall() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        FfmpegManager.getInstance().downloadFfmpegWithProgress(parent,
                () -> SwingUtilities.invokeLater(this::refreshFfmpegSection));
    }

    // ── Cookies ───────────────────────────────────────────────────────────────

    private JPanel buildCookiesSection() {
        useCookiesCheck = new JCheckBox(
                "Usar cookies do YouTube (necessário para vídeos com restrição de idade)");
        useCookiesCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        cookiesPathField = new JTextField();
        cookiesPathField.putClientProperty("JTextField.placeholderText",
                "Caminho para o arquivo youtube_cookies.txt");

        JButton browseBtn = new JButton("📂");
        browseBtn.setPreferredSize(new Dimension(40, 32));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Localizar arquivo de cookies");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                cookiesPathField.setText(fc.getSelectedFile().getAbsolutePath());
        });

        useCookiesCheck.addActionListener(e -> {
            cookiesPathField.setEnabled(useCookiesCheck.isSelected());
            browseBtn.setEnabled(useCookiesCheck.isSelected());
        });

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.add(new JLabel("Arquivo: "), BorderLayout.WEST);
        pathRow.add(cookiesPathField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);

        JLabel hint = new JLabel("<html><font color='#888888'>Para exportar cookies, " +
                "use a extensão 'Get cookies.txt LOCALLY' no Chrome/Firefox.</font></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(useCookiesCheck);
        p.add(Box.createVerticalStrut(6));
        p.add(pathRow);
        p.add(hint);
        return p;
    }

    // ── Pasta padrão ──────────────────────────────────────────────────────────

    private JPanel buildDirSection() {
        defaultDirField = new JTextField();

        JButton browseBtn = new JButton("📁 Procurar");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(defaultDirField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                defaultDirField.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.add(new JLabel("Pasta: "), BorderLayout.WEST);
        p.add(defaultDirField, BorderLayout.CENTER);
        p.add(browseBtn, BorderLayout.EAST);
        return p;
    }

    // ── Downloads ─────────────────────────────────────────────────────────────

    private JPanel buildDownloadSection() {
        JPanel p = new JPanel(new GridLayout(2, 2, 10, 8));

        p.add(new JLabel("Downloads simultâneos:"));
        maxDownloadsCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        p.add(maxDownloadsCombo);

        p.add(new JLabel("Formato padrão:"));
        defaultFormatCombo = new JComboBox<>();
        for (DownloadItem.Format f : DownloadItem.Format.values())
            defaultFormatCombo.addItem(f.getLabel());
        p.add(defaultFormatCombo);

        return p;
    }

    // ── Botão salvar ──────────────────────────────────────────────────────────

    private JPanel buildSaveButton() {
        JButton saveBtn = new JButton("💾  Salvar Configurações");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setPreferredSize(new Dimension(220, 42));
        saveBtn.setBackground(new Color(255, 68, 68));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveSettings());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(saveBtn);
        return p;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private void loadValues() {
        AppConfig cfg = AppConfig.getInstance();
        ytdlpPathField.setText(cfg.getYtdlpPath());
        cookiesPathField.setText(cfg.getCookiesPath());
        useCookiesCheck.setSelected(cfg.isUseCookies());
        cookiesPathField.setEnabled(cfg.isUseCookies());
        defaultDirField.setText(cfg.getDefaultDownloadDir());
        maxDownloadsCombo.setSelectedItem(cfg.getMaxConcurrentDownloads());
        autoUpdateCheck.setSelected(cfg.isAutoUpdateYtdlp());
        autoUpdateDaysCombo.setSelectedItem(cfg.getAutoUpdateDays());
        autoUpdateDaysCombo.setEnabled(cfg.isAutoUpdateYtdlp());
        refreshLastCheckLabel();

        String fmtName = cfg.getDefaultFormat();
        DownloadItem.Format[] formats = DownloadItem.Format.values();
        for (int i = 0; i < formats.length; i++) {
            if (formats[i].name().equals(fmtName)) {
                defaultFormatCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void saveSettings() {
        AppConfig cfg = AppConfig.getInstance();
        cfg.setYtdlpPath(ytdlpPathField.getText().trim());
        cfg.setCookiesPath(cookiesPathField.getText().trim());
        cfg.setUseCookies(useCookiesCheck.isSelected());
        cfg.setDefaultDownloadDir(defaultDirField.getText().trim());
        cfg.setMaxConcurrentDownloads((Integer) maxDownloadsCombo.getSelectedItem());
        int idx = defaultFormatCombo.getSelectedIndex();
        cfg.setDefaultFormat(DownloadItem.Format.values()[idx].name());
        cfg.setAutoUpdateYtdlp(autoUpdateCheck.isSelected());
        cfg.setAutoUpdateDays((Integer) autoUpdateDaysCombo.getSelectedItem());
        cfg.save();

        JOptionPane.showMessageDialog(this,
                "✅ Configurações salvas com sucesso!\n" +
                        "Algumas mudanças (como downloads simultâneos) só têm efeito ao reiniciar.",
                "Salvo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    /**
     * A versão do yt-dlp é a data do release (2026.08.19). Passando de ~90 dias
     * o YouTube já mudou a extração e começam os erros 403 / playlist truncada.
     */
    private static String versionWarning(String version) {
        long dias = YtdlpUpdater.versionAgeDays(version);
        if (dias > 90) {
            return "\n\n⚠ Esta versão tem " + dias + " dias.\n" +
                   "Versões antigas causam erro 403 e playlists incompletas.\n" +
                   "Clique em Atualizar.";
        }
        return "";
    }

    /**
     * Atualização manual. O caminho digitado é salvo antes, senão o
     * YtdlpUpdater usaria o valor antigo do AppConfig.
     */
    private void updateYtdlp() {
        String path = ytdlpPathField.getText().trim();
        AppConfig.getInstance().setYtdlpPath(path.isEmpty() ? "yt-dlp" : path);

        updateBtn.setEnabled(false);
        updateBtn.setText("⏳ Atualizando...");

        YtdlpUpdater.getInstance().updateNow(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                () -> {
                    updateBtn.setEnabled(true);
                    updateBtn.setText("⬆ Atualizar");
                });
    }

    private void testYtdlp() {
        String path = ytdlpPathField.getText().trim();
        if (path.isEmpty()) path = "yt-dlp";
        final String finalPath = path;

        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(finalPath, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                byte[] bytes = p.getInputStream().readAllBytes();
                p.waitFor();
                return new String(bytes).trim();
            }
            @Override protected void done() {
                try {
                    String version = get();
                    String aviso = versionWarning(version);
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "✅ yt-dlp encontrado!\nVersão: " + version + aviso,
                            "yt-dlp OK",
                            aviso.isEmpty() ? JOptionPane.INFORMATION_MESSAGE
                                            : JOptionPane.WARNING_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "❌ yt-dlp não encontrado!\n\n" +
                                    "Verifique se está instalado e no PATH do sistema.\n" +
                                    "Download: https://github.com/yt-dlp/yt-dlp/releases",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }
}