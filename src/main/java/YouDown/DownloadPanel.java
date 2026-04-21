package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class DownloadPanel extends JPanel {

    private JTextField urlField;
    private JComboBox<DownloadItem.Format> formatCombo;
    private JTextField outputDirField;
    private JButton downloadBtn;
    private JButton pasteBtn;

    public DownloadPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        buildUI();
    }

    private void buildUI() {
        // ── Título da seção ──
        JLabel title = new JLabel("Novo Download");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(255, 68, 68));
        title.setBorder(new EmptyBorder(0, 0, 16, 0));

        // ── Campo URL ──
        JLabel urlLabel = new JLabel("URL do YouTube");
        urlLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        urlLabel.setForeground(new Color(180, 180, 180));

        urlField = new JTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        urlField.setPreferredSize(new Dimension(0, 38));
        urlField.putClientProperty("JTextField.placeholderText", "https://www.youtube.com/watch?v=...");

        pasteBtn = new JButton("📋 Colar");
        pasteBtn.setPreferredSize(new Dimension(90, 38));
        pasteBtn.addActionListener(e -> {
            String clip = getClipboardText();
            if (clip != null && !clip.isEmpty()) urlField.setText(clip);
        });

        JPanel urlRow = new JPanel(new BorderLayout(6, 0));
        urlRow.add(urlField, BorderLayout.CENTER);
        urlRow.add(pasteBtn, BorderLayout.EAST);

        // ── Formato ──
        JLabel formatLabel = new JLabel("Formato");
        formatLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formatLabel.setForeground(new Color(180, 180, 180));

        formatCombo = new JComboBox<>(DownloadItem.Format.values());
        formatCombo.setPreferredSize(new Dimension(0, 38));
        formatCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DownloadItem.Format) {
                    DownloadItem.Format f = (DownloadItem.Format) value;
                    boolean audio = f == DownloadItem.Format.AUDIO_MP3
                            || f == DownloadItem.Format.AUDIO_M4A
                            || f == DownloadItem.Format.AUDIO_WAV;
                    setText((audio ? "🎵 " : "🎬 ") + f.getLabel());
                }
                return this;
            }
        });

        // Selecionar formato padrão das configurações
        String defaultFmt = AppConfig.getInstance().getDefaultFormat();
        try {
            formatCombo.setSelectedItem(DownloadItem.Format.valueOf(defaultFmt));
        } catch (Exception ignored) {}

        // ── Pasta de destino ──
        JLabel dirLabel = new JLabel("Salvar em");
        dirLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dirLabel.setForeground(new Color(180, 180, 180));

        outputDirField = new JTextField(AppConfig.getInstance().getDefaultDownloadDir());
        outputDirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        outputDirField.setPreferredSize(new Dimension(0, 38));

        JButton browseBtn = new JButton("📁 Procurar");
        browseBtn.setPreferredSize(new Dimension(100, 38));
        browseBtn.addActionListener(e -> chooseDirectory());

        JPanel dirRow = new JPanel(new BorderLayout(6, 0));
        dirRow.add(outputDirField, BorderLayout.CENTER);
        dirRow.add(browseBtn, BorderLayout.EAST);

        // ── Botão principal ──
        downloadBtn = new JButton("⬇  BAIXAR AGORA");
        downloadBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        downloadBtn.setPreferredSize(new Dimension(0, 48));
        downloadBtn.setBackground(new Color(255, 68, 68));
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setFocusPainted(false);
        downloadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        downloadBtn.addActionListener(e -> startDownload());

        // Tecla Enter no campo URL inicia download
        urlField.addActionListener(e -> startDownload());

        // ── Dicas de uso ──
        JTextArea tipsArea = new JTextArea(
                "💡 Dicas de uso:\n" +
                        "  • Cole a URL de um vídeo, playlist ou canal do YouTube\n" +
                        "  • Para playlists inteiras, use a URL da playlist\n" +
                        "  • O yt-dlp deve estar instalado e no PATH do sistema\n" +
                        "  • Configure cookies em Configurações para vídeos com restrição de idade"
        );
        tipsArea.setEditable(false);
        tipsArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tipsArea.setForeground(new Color(140, 140, 140));
        tipsArea.setBackground(UIManager.getColor("Panel.background"));
        tipsArea.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(60, 60, 60)),
                new EmptyBorder(14, 4, 4, 4)
        ));

        // ── Montar layout ──
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        formPanel.add(title);
        formPanel.add(urlLabel);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(urlRow);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(formatLabel);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(formatCombo);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(dirLabel);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(dirRow);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(downloadBtn);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(tipsArea);

        // Fixar largura dos componentes
        for (Component c : new Component[]{urlRow, formatCombo, dirRow, downloadBtn}) {
            ((JComponent)c).setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        for (Component c : new Component[]{title, urlLabel, formatLabel, dirLabel, tipsArea}) {
            ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showError("Por favor, insira uma URL do YouTube.");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showError("URL inválida. Deve começar com http:// ou https://");
            return;
        }

        String outputDir = outputDirField.getText().trim();
        if (outputDir.isEmpty()) {
            showError("Por favor, escolha uma pasta de destino.");
            return;
        }

        File dir = new File(outputDir);
        if (!dir.exists()) {
            int res = JOptionPane.showConfirmDialog(this,
                    "A pasta não existe. Deseja criá-la?", "Criar pasta",
                    JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                if (!dir.mkdirs()) {
                    showError("Não foi possível criar a pasta.");
                    return;
                }
            } else {
                return;
            }
        }

        DownloadItem.Format format = (DownloadItem.Format) formatCombo.getSelectedItem();
        DownloadItem item = new DownloadItem(url, format, outputDir);
        DownloadEngine.getInstance().addDownload(item);

        urlField.setText("");
        JOptionPane.showMessageDialog(this,
                "✅ Download adicionado à fila!\n\nAcompanhe o progresso na aba 'Fila de Downloads'.",
                "Adicionado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(outputDirField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Escolha a pasta de destino");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private String getClipboardText() {
        try {
            return (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            return "";
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}