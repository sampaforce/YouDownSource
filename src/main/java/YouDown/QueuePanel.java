package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class QueuePanel extends JPanel implements DownloadEngine.DownloadListener {

    private JPanel itemsPanel;
    private JScrollPane scrollPane;
    private JLabel emptyLabel;
    private JLabel statsLabel;

    public QueuePanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));
        buildUI();
        DownloadEngine.getInstance().addListener(this);
    }

    private void buildUI() {
        // ── Header ──
        JLabel title = new JLabel("Fila de Downloads");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(255, 68, 68));

        statsLabel = new JLabel("0 itens");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsLabel.setForeground(new Color(140, 140, 140));

        JButton clearBtn = new JButton("🗑 Limpar concluídos");
        clearBtn.addActionListener(e -> {
            DownloadEngine.getInstance().clearCompleted();
        });

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleGroup.add(title);
        titleGroup.add(Box.createHorizontalStrut(10));
        titleGroup.add(statsLabel);

        headerPanel.add(titleGroup, BorderLayout.WEST);
        headerPanel.add(clearBtn, BorderLayout.EAST);

        // ── Painel de itens ──
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        emptyLabel = new JLabel("Nenhum download na fila", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emptyLabel.setForeground(new Color(100, 100, 100));
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setBorder(new EmptyBorder(60, 0, 0, 0));

        itemsPanel.add(emptyLabel);

        scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshUI() {
        List<DownloadItem> items = DownloadEngine.getInstance().getQueue();

        itemsPanel.removeAll();

        if (items.isEmpty()) {
            itemsPanel.add(emptyLabel);
            statsLabel.setText("0 itens");
        } else {
            long active = items.stream().filter(i ->
                    i.getStatus() == DownloadItem.Status.DOWNLOADING ||
                            i.getStatus() == DownloadItem.Status.PENDING).count();
            statsLabel.setText(items.size() + " itens | " + active + " ativo(s)");

            for (DownloadItem item : items) {
                itemsPanel.add(createItemCard(item));
                itemsPanel.add(Box.createVerticalStrut(8));
            }
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel createItemCard(DownloadItem item) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, getStatusColor(item.getStatus())),
                new CompoundBorder(
                        BorderFactory.createLineBorder(new Color(55, 55, 55), 1),
                        new EmptyBorder(10, 12, 10, 10)
                )
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.putClientProperty("JPanel.background", new Color(42, 42, 42));

        // ── Coluna esquerda: info ──
        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(Color.WHITE);

        String urlShort = item.getUrl().length() > 60
                ? item.getUrl().substring(0, 60) + "..."
                : item.getUrl();
        JLabel urlLabel = new JLabel(urlShort);
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        urlLabel.setForeground(new Color(120, 120, 120));

        // Badges de formato e status
        JLabel formatBadge = new JLabel(item.getFormat().getLabel());
        formatBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        formatBadge.setForeground(new Color(200, 200, 200));
        formatBadge.setOpaque(true);
        formatBadge.setBackground(new Color(60, 60, 60));
        formatBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        JLabel statusBadge = new JLabel(item.getStatus().getLabel());
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusBadge.setForeground(Color.WHITE);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(getStatusColor(item.getStatus()));
        statusBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        badgeRow.add(formatBadge);
        badgeRow.add(statusBadge);
        if (item.getFileSize() != null && !item.getFileSize().isEmpty() && !item.getFileSize().equals("N/A")) {
            JLabel sizeLabel = new JLabel("📦 " + item.getFileSize());
            sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            sizeLabel.setForeground(new Color(120, 120, 120));
            badgeRow.add(sizeLabel);
        }
        if (item.getSpeed() != null && item.getStatus() == DownloadItem.Status.DOWNLOADING) {
            JLabel speedLabel = new JLabel("⚡ " + item.getSpeed());
            speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            speedLabel.setForeground(new Color(120, 200, 120));
            badgeRow.add(speedLabel);
        }
        if (item.getEta() != null && item.getStatus() == DownloadItem.Status.DOWNLOADING
                && !item.getEta().equals("N/A")) {
            JLabel etaLabel = new JLabel("⏱ " + item.getEta());
            etaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            etaLabel.setForeground(new Color(180, 180, 120));
            badgeRow.add(etaLabel);
        }

        // Barra de progresso
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(item.getProgress());
        progressBar.setStringPainted(true);
        progressBar.setString(item.getProgress() + "%");
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));

        if (item.getStatus() == DownloadItem.Status.COMPLETED) {
            progressBar.setForeground(new Color(80, 200, 120));
        } else if (item.getStatus() == DownloadItem.Status.ERROR) {
            progressBar.setForeground(new Color(220, 60, 60));
        }

        // Mensagem de erro
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(urlLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(badgeRow);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(progressBar);

        if (item.getStatus() == DownloadItem.Status.ERROR && item.getErrorMessage() != null) {
            JLabel errLabel = new JLabel("<html><font color='#FF6B6B'>⚠ " +
                    item.getErrorMessage().replace("\n", "<br>") + "</font></html>");
            errLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(errLabel);
        }

        // ── Coluna direita: botões ──
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setPreferredSize(new Dimension(110, 0));

        if (item.getStatus() == DownloadItem.Status.DOWNLOADING ||
                item.getStatus() == DownloadItem.Status.PENDING) {
            JButton cancelBtn = new JButton("⏹ Cancelar");
            cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            cancelBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            cancelBtn.addActionListener(e -> DownloadEngine.getInstance().cancelDownload(item));
            btnPanel.add(cancelBtn);
        }

        if (item.getStatus() == DownloadItem.Status.COMPLETED) {
            JButton openBtn = new JButton("📂 Abrir pasta");
            openBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            openBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            openBtn.addActionListener(e -> {
                try {
                    // Tenta abrir a pasta do arquivo salvo, senão usa a pasta de destino
                    File target = null;
                    if (item.getSavedFilePath() != null) {
                        File f = new File(item.getSavedFilePath());
                        target = f.exists() ? f.getParentFile() : new File(item.getOutputDir());
                    } else {
                        target = new File(item.getOutputDir());
                    }
                    if (target != null && target.exists()) {
                        Desktop.getDesktop().open(target);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Pasta não encontrada:\n" + item.getOutputDir(),
                                "Erro", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            btnPanel.add(openBtn);
            btnPanel.add(Box.createVerticalStrut(4));
        }

        if (item.getStatus() != DownloadItem.Status.DOWNLOADING &&
                item.getStatus() != DownloadItem.Status.PENDING) {
            JButton removeBtn = new JButton("🗑 Remover");
            removeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            removeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            removeBtn.addActionListener(e -> {
                DownloadEngine.getInstance().removeFromQueue(item);
            });
            btnPanel.add(removeBtn);
        }

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }

    private Color getStatusColor(DownloadItem.Status status) {
        switch (status) {
            case PENDING: return new Color(100, 100, 180);
            case DOWNLOADING: return new Color(255, 180, 0);
            case CONVERTING: return new Color(0, 180, 255);
            case COMPLETED: return new Color(80, 200, 120);
            case ERROR: return new Color(220, 60, 60);
            case CANCELLED: return new Color(100, 100, 100);
            default: return Color.GRAY;
        }
    }

    @Override
    public void onProgressUpdate(DownloadItem item) { refreshUI(); }

    @Override
    public void onStatusChange(DownloadItem item) { refreshUI(); }

    @Override
    public void onQueueChange() { refreshUI(); }
}