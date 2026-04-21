package YouDown;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.prefs.Preferences;

public class TermsDialog extends JDialog {

    private static final String PREF_KEY = "terms_accepted_v1";
    private JCheckBox acceptCheck;
    private JButton   acceptBtn;
    private boolean   accepted = false;

    public TermsDialog(Frame parent) {
        super(parent, "Termos de Uso — YouDown", true);
        setSize(620, 560);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));

        // ── Cabeçalho ─────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 30));
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("▶ YouDown  —  Termos de Uso");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(new Color(255, 68, 68));

        JLabel subtitle = new JLabel("Leia com atenção antes de utilizar o software.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(130, 130, 130));

        header.add(title,    BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        // ── Texto dos termos ──────────────────────────────────
        JTextArea termsArea = new JTextArea(getTermsText());
        termsArea.setEditable(false);
        termsArea.setWrapStyleWord(true);
        termsArea.setLineWrap(true);
        termsArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        termsArea.setForeground(new Color(200, 200, 200));
        termsArea.setBackground(new Color(28, 28, 28));
        termsArea.setBorder(new EmptyBorder(16, 20, 16, 20));
        termsArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(termsArea);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(55, 55, 55)));
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        // ── Rodapé ────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBorder(new EmptyBorder(14, 20, 14, 20));
        footer.setBackground(new Color(30, 30, 30));

        acceptCheck = new JCheckBox("Li e concordo com os Termos de Uso acima.");
        acceptCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        acceptCheck.setForeground(new Color(200, 200, 200));
        acceptCheck.setBackground(new Color(30, 30, 30));
        acceptCheck.addActionListener(e ->
            acceptBtn.setEnabled(acceptCheck.isSelected()));

        acceptBtn = new JButton("✅  Aceitar e Continuar");
        acceptBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        acceptBtn.setPreferredSize(new Dimension(200, 38));
        acceptBtn.setBackground(new Color(255, 68, 68));
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setEnabled(false);
        acceptBtn.addActionListener(e -> {
            saveAcceptance();
            accepted = true;
            dispose();
        });

        JButton declineBtn = new JButton("Recusar e Sair");
        declineBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        declineBtn.setPreferredSize(new Dimension(150, 38));
        declineBtn.setForeground(new Color(140, 140, 140));
        declineBtn.addActionListener(e -> {
            System.exit(0);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(declineBtn);
        btnPanel.add(acceptBtn);

        footer.add(acceptCheck, BorderLayout.CENTER);
        footer.add(btnPanel,    BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private String getTermsText() {
        return
            "TERMOS DE USO — YouDown\n" +
            "Versão 1.0  |  Desenvolvido por Guilherme Sampaio\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +

            "1. ACEITAÇÃO DOS TERMOS\n\n" +
            "Ao instalar, copiar ou utilizar o YouDown, você concorda com os presentes " +
            "Termos de Uso. Caso não concorde, não utilize o software.\n\n" +

            "2. DESCRIÇÃO DO SOFTWARE\n\n" +
            "O YouDown é uma interface gráfica que facilita o uso das ferramentas de " +
            "código aberto yt-dlp e FFmpeg para download e conversão de mídia. " +
            "O desenvolvedor não é o criador do yt-dlp nem do FFmpeg.\n\n" +

            "3. RESPONSABILIDADE DO USUÁRIO\n\n" +
            "O YouDown destina-se EXCLUSIVAMENTE ao download de conteúdos:\n" +
            "  • De domínio público;\n" +
            "  • Com autorização expressa do detentor dos direitos autorais;\n" +
            "  • Para uso pessoal em conformidade com as leis locais aplicáveis.\n\n" +
            "É de responsabilidade exclusiva do usuário garantir que o conteúdo " +
            "baixado não infrinja direitos autorais, marcas registradas ou quaisquer " +
            "outros direitos de terceiros.\n\n" +

            "4. TERMOS DE SERVIÇO DE TERCEIROS\n\n" +
            "O uso do YouDown para baixar conteúdo de plataformas como YouTube, " +
            "Instagram, TikTok, entre outras, pode violar os Termos de Serviço dessas " +
            "plataformas. O usuário é o único responsável por verificar e cumprir os " +
            "termos de cada plataforma antes de realizar downloads.\n\n" +

            "5. ISENÇÃO DE RESPONSABILIDADE\n\n" +
            "O desenvolvedor Guilherme Sampaio e o YouDown:\n" +
            "  • Não se responsabilizam por qualquer uso indevido do software;\n" +
            "  • Não garantem disponibilidade contínua de funcionalidades que dependam " +
            "de serviços de terceiros;\n" +
            "  • Não armazenam, acessam ou transmitem qualquer conteúdo baixado pelo usuário;\n" +
            "  • Não são responsáveis por danos diretos, indiretos ou consequentes " +
            "decorrentes do uso do software.\n\n" +

            "6. PROPRIEDADE INTELECTUAL\n\n" +
            "O YouDown é distribuído gratuitamente. É proibido:\n" +
            "  • Vender ou sublicenciar o software;\n" +
            "  • Remover créditos do desenvolvedor;\n" +
            "  • Distribuir versões modificadas sem atribuição ao desenvolvedor original.\n\n" +

            "7. COLETA DE DADOS\n\n" +
            "O YouDown NÃO coleta, armazena nem transmite dados pessoais do usuário. " +
            "Todas as configurações são salvas localmente no dispositivo do usuário.\n\n" +

            "8. ATUALIZAÇÕES\n\n" +
            "O desenvolvedor poderá lançar atualizações do software a qualquer momento. " +
            "O uso continuado após atualizações implica aceitação dos termos revisados.\n\n" +

            "9. CONTATO\n\n" +
            "Dúvidas, sugestões ou suporte:\n" +
            "  • WhatsApp: +55 (73) 99170-7161\n" +
            "  • E-mail:   guilhermesampaio.adm@gmail.com\n\n" +

            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Ao clicar em \"Aceitar e Continuar\", você declara ter lido, compreendido\n" +
            "e concordado integralmente com estes Termos de Uso.\n";
    }

    private void saveAcceptance() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(TermsDialog.class);
            prefs.putBoolean(PREF_KEY, true);
            prefs.flush();
        } catch (Exception ignored) {}
    }

    public boolean isAccepted() { return accepted; }

    /**
     * Verifica se o usuário já aceitou os termos anteriormente.
     */
    public static boolean alreadyAccepted() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(TermsDialog.class);
            return prefs.getBoolean(PREF_KEY, false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Exibe os termos se ainda não foram aceitos.
     * Retorna false se o usuário recusar (app deve fechar).
     */
    public static boolean showIfNeeded(Frame parent) {
        if (alreadyAccepted()) return true;
        TermsDialog dialog = new TermsDialog(parent);
        dialog.setVisible(true);
        return dialog.isAccepted();
    }
}
