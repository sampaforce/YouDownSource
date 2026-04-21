package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.net.URI;

public class AboutPanel extends JPanel {

    private static final String PIX_KEY     = "73991707161";
    private static final String PIX_NAME   = "Guilherme Sampaio";
    private static final String PAYPAL_EMAIL = "guilhermesampaio.adm@gmail.com";
    private static final String PAYPAL_URL   = "https://www.paypal.com/donate/?business=guilhermesampaio.adm%40gmail.com&currency_code=BRL";

    public AboutPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(24, 40, 24, 40));

        // ── Logo ──────────────────────────────────────────────
        JLabel logo = new JLabel("▶ YouDown", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        logo.setForeground(new Color(255, 68, 68));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("YouTube Downloader", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(140, 140, 140));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel version = new JLabel("Versão 1.0.0  •  powered by yt-dlp & ffmpeg", SwingConstants.CENTER);
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(80, 80, 80));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Card desenvolvedor ────────────────────────────────
        JPanel devCard = buildDevCard();
        devCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Card doação ───────────────────────────────────────
        JPanel donationCard = buildDonationCard();
        donationCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Tecnologias ───────────────────────────────────────
        JLabel techLabel = new JLabel(
                "Java  •  Swing  •  FlatLaf  •  yt-dlp  •  ffmpeg",
                SwingConstants.CENTER);
        techLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        techLabel.setForeground(new Color(65, 65, 65));
        techLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Montar ────────────────────────────────────────────
        center.add(logo);
        center.add(Box.createVerticalStrut(4));
        center.add(subtitle);
        center.add(Box.createVerticalStrut(4));
        center.add(version);
        center.add(Box.createVerticalStrut(20));
        center.add(separator());
        center.add(Box.createVerticalStrut(20));
        center.add(devCard);
        center.add(Box.createVerticalStrut(16));
        center.add(donationCard);
        center.add(Box.createVerticalStrut(20));
        center.add(separator());
        center.add(Box.createVerticalStrut(12));
        center.add(techLabel);

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Card do desenvolvedor ─────────────────────────────────────────────────

    private JPanel buildDevCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
                new EmptyBorder(16, 28, 16, 28)));
        card.setMaximumSize(new Dimension(460, 160));

        JLabel devTitle = new JLabel("Desenvolvido por", SwingConstants.CENTER);
        devTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        devTitle.setForeground(new Color(110, 110, 110));
        devTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel devName = new JLabel("Guilherme Sampaio", SwingConstants.CENTER);
        devName.setFont(new Font("Segoe UI", Font.BOLD, 19));
        devName.setForeground(new Color(220, 220, 220));
        devName.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(devTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(devName);
        card.add(Box.createVerticalStrut(14));
        card.add(thinSeparator());
        card.add(Box.createVerticalStrut(14));
        card.add(buildContactRow("📱", "WhatsApp", "+55 (73) 99170-7161",
                () -> openUrl("https://wa.me/5573991707161")));
        card.add(Box.createVerticalStrut(8));
        card.add(buildContactRow("✉", "E-mail", "guilhermesampaio.adm@gmail.com",
                () -> openUrl("mailto:guilhermesampaio.adm@gmail.com")));
        return card;
    }

    // ── Card de doação ────────────────────────────────────────────────────────

    private JPanel buildDonationCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 180, 0, 80), 1),
                new EmptyBorder(18, 28, 18, 28)));
        card.setMaximumSize(new Dimension(460, 300));
        card.setBackground(new Color(40, 35, 20));
        card.setOpaque(true);

        // Título
        JLabel title = new JLabel("☕  Apoie o Projeto", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(255, 200, 60));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Texto motivacional
        JLabel msg = new JLabel(
                "<html><center><font color='#AAAAAA'>" +
                        "O YouDown é gratuito e sem anúncios.<br>" +
                        "Se ele te ajudou, considere fazer uma doação<br>" +
                        "para manter o projeto vivo e atualizado! 🙏" +
                        "</font></center></html>",
                SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Chave PIX
        JLabel pixLabel = new JLabel("Chave PIX (Telefone):", SwingConstants.CENTER);
        pixLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pixLabel.setForeground(new Color(130, 130, 130));
        pixLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pixKey = new JLabel(PIX_KEY, SwingConstants.CENTER);
        pixKey.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pixKey.setForeground(new Color(255, 200, 60));
        pixKey.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pixName = new JLabel("Favorecido: " + PIX_NAME, SwingConstants.CENTER);
        pixName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pixName.setForeground(new Color(110, 110, 110));
        pixName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botão copiar chave PIX
        JButton copyPixBtn = new JButton("📋  Copiar Chave PIX");
        copyPixBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        copyPixBtn.setPreferredSize(new Dimension(200, 38));
        copyPixBtn.setMaximumSize(new Dimension(220, 38));
        copyPixBtn.setBackground(new Color(255, 180, 0));
        copyPixBtn.setForeground(new Color(30, 20, 0));
        copyPixBtn.setFocusPainted(false);
        copyPixBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyPixBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        copyPixBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(PIX_KEY), null);
            copyPixBtn.setText("✅  Chave copiada!");
            copyPixBtn.setBackground(new Color(80, 200, 120));
            copyPixBtn.setForeground(Color.WHITE);
            Timer t = new Timer(2000, ev -> {
                copyPixBtn.setText("📋  Copiar Chave PIX");
                copyPixBtn.setBackground(new Color(255, 180, 0));
                copyPixBtn.setForeground(new Color(30, 20, 0));
            });
            t.setRepeats(false);
            t.start();
        });

        // Botão PayPal
        JButton paypalBtn = new JButton("🅿  Doar via PayPal");
        paypalBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        paypalBtn.setPreferredSize(new Dimension(200, 38));
        paypalBtn.setMaximumSize(new Dimension(220, 38));
        paypalBtn.setBackground(new Color(0, 112, 186));
        paypalBtn.setForeground(Color.WHITE);
        paypalBtn.setFocusPainted(false);
        paypalBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        paypalBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        paypalBtn.addActionListener(e -> openUrl(PAYPAL_URL));

        // Label e-mail PayPal
        JLabel paypalEmailLabel = new JLabel(PAYPAL_EMAIL, SwingConstants.CENTER);
        paypalEmailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        paypalEmailLabel.setForeground(new Color(100, 140, 200));
        paypalEmailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(msg);
        card.add(Box.createVerticalStrut(14));
        card.add(thinSeparator());
        card.add(Box.createVerticalStrut(12));
        card.add(pixLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(pixKey);
        card.add(Box.createVerticalStrut(2));
        card.add(pixName);
        card.add(Box.createVerticalStrut(12));
        card.add(copyPixBtn);
        card.add(Box.createVerticalStrut(14));
        card.add(thinSeparator());
        card.add(Box.createVerticalStrut(10));
        JLabel orLabel = new JLabel("— ou doe pelo PayPal —", SwingConstants.CENTER);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        orLabel.setForeground(new Color(80, 80, 80));
        orLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(orLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(paypalBtn);
        card.add(Box.createVerticalStrut(4));
        card.add(paypalEmailLabel);

        return card;
    }

    // ── Linha de contato ──────────────────────────────────────────────────────

    private JPanel buildContactRow(String icon, String label, String value, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setMaximumSize(new Dimension(420, 34));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel(icon + "  " + label + ":");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        iconLabel.setForeground(new Color(130, 130, 130));
        iconLabel.setPreferredSize(new Dimension(105, 28));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueLabel.setForeground(new Color(100, 180, 255));
        valueLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        valueLabel.setToolTipText("Clique para abrir");
        valueLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
            @Override public void mouseEntered(MouseEvent e) {
                valueLabel.setForeground(new Color(160, 210, 255));
            }
            @Override public void mouseExited(MouseEvent e) {
                valueLabel.setForeground(new Color(100, 180, 255));
            }
        });

        JButton copyBtn = new JButton("📋");
        copyBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyBtn.setPreferredSize(new Dimension(36, 28));
        copyBtn.setToolTipText("Copiar " + label);
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(value), null);
            copyBtn.setText("✅");
            Timer t = new Timer(1500, ev -> copyBtn.setText("📋"));
            t.setRepeats(false);
            t.start();
        });

        row.add(iconLabel,  BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        row.add(copyBtn,    BorderLayout.EAST);
        return row;
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(460, 1));
        sep.setForeground(new Color(55, 55, 55));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private JSeparator thinSeparator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(340, 1));
        sep.setForeground(new Color(55, 55, 55));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível abrir o link.\nCopie manualmente: " + url,
                    "Erro", JOptionPane.WARNING_MESSAGE);
        }
    }
}