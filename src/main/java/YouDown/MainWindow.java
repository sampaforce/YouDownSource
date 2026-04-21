package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MainWindow extends JFrame {

    private JTabbedPane tabbedPane;
    private QueuePanel queuePanel;

    public MainWindow() {
        super("YouDown - YouTube Downloader");
        setMinimumSize(new Dimension(780, 580));
        setPreferredSize(new Dimension(900, 650));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        buildUI();
        pack();
        setLocationRelativeTo(null);

        // Verifica ffmpeg após janela estar visível
        SwingUtilities.invokeLater(() ->
                FfmpegManager.getInstance().checkOnStartup(this)
        );
    }

    private void buildUI() {
        // ── Barra do topo com logo ──
        JPanel topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);

        // ── Abas principais ──
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.putClientProperty("JTabbedPane.tabWidthMode", "equal");

        DownloadPanel downloadPanel = new DownloadPanel();
        queuePanel = new QueuePanel();
        SettingsPanel settingsPanel = new SettingsPanel();
        AboutPanel    aboutPanel    = new AboutPanel();

        tabbedPane.addTab("  ⬇  Baixar  ", downloadPanel);
        tabbedPane.addTab("  📋  Fila  ", queuePanel);
        tabbedPane.addTab("  ⚙  Config  ", settingsPanel);
        tabbedPane.addTab("  ℹ  Sobre  ", aboutPanel);

        // Estilo das abas
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        add(tabbedPane, BorderLayout.CENTER);

        // ── Barra de status ──
        JPanel statusBar = buildStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // Atualizar contador da aba Fila
        DownloadEngine.getInstance().addListener(new DownloadEngine.DownloadListener() {
            @Override
            public void onProgressUpdate(DownloadItem item) {}
            @Override
            public void onStatusChange(DownloadItem item) { updateQueueTabTitle(); }
            @Override
            public void onQueueChange() { updateQueueTabTitle(); }
        });
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(25, 25, 25));
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Logo / nome
        JLabel logo = new JLabel("▶ YouDown");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(new Color(255, 68, 68));

        JLabel subtitle = new JLabel("  YouTube Downloader");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(140, 140, 140));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(logo);
        leftGroup.add(subtitle);

        // Versão
        JLabel version = new JLabel("v1.0.0  |  powered by yt-dlp");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(90, 90, 90));

        bar.add(leftGroup, BorderLayout.WEST);
        bar.add(version, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(25, 25, 25));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(50, 50, 50)),
                new EmptyBorder(5, 12, 5, 12)
        ));

        JLabel statusLabel = new JLabel("Pronto.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(100, 100, 100));

        JLabel tipLabel = new JLabel("Dica: Cole uma URL e pressione Enter para baixar rapidamente");
        tipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tipLabel.setForeground(new Color(80, 80, 80));

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(tipLabel, BorderLayout.EAST);

        return bar;
    }

    private void updateQueueTabTitle() {
        long active = DownloadEngine.getInstance().getQueue().stream()
                .filter(i -> i.getStatus() == DownloadItem.Status.DOWNLOADING ||
                        i.getStatus() == DownloadItem.Status.PENDING)
                .count();

        String tabTitle = active > 0
                ? "  📋  Fila (" + active + ")  "
                : "  📋  Fila  ";

        SwingUtilities.invokeLater(() -> tabbedPane.setTitleAt(1, tabTitle));
    }

    private void confirmExit() {
        long activeCount = DownloadEngine.getInstance().getQueue().stream()
                .filter(i -> i.getStatus() == DownloadItem.Status.DOWNLOADING ||
                        i.getStatus() == DownloadItem.Status.PENDING)
                .count();

        if (activeCount > 0) {
            int res = JOptionPane.showConfirmDialog(this,
                    "Existem " + activeCount + " download(s) em andamento.\nDeseja sair e cancelar todos?",
                    "Confirmar saída",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.YES_OPTION) return;
        }

        DownloadEngine.getInstance().shutdown();
        AppConfig.getInstance().save();
        dispose();
        System.exit(0);
    }
}