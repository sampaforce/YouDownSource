package YouDown;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    public static void main(String[] args) {

        // Log de inicialização para diagnóstico
        setupLogging();

        // A elevação de admin agora é feita pelo Launch4j via manifesto,
        // não por código Java (que causava o duplo UAC com o JRE).
        // Se quiser avisar o usuário caso rode sem admin de outra forma:
        if (isWindows() && !isRunningAsAdmin()) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                    "<html><b>Atenção:</b> YouDown não está rodando como Administrador.<br>" +
                    "Se estiver instalado em <i>Arquivos de Programas</i> ou <i>C:\\</i>,<br>" +
                    "algumas funções podem falhar.<br><br>" +
                    "Clique com o botão direito no YouDown.exe → <b>Executar como administrador</b>.</html>",
                    "Permissão insuficiente",
                    JOptionPane.WARNING_MESSAGE)
            );
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                customizeTheme();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Exibe termos de uso na primeira execução
            if (!TermsDialog.showIfNeeded(null)) {
                System.exit(0);
                return;
            }

            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    public static boolean isRunningAsAdmin() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "([Security.Principal.WindowsPrincipal]" +
                "[Security.Principal.WindowsIdentity]::GetCurrent())" +
                ".IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String result = br.readLine();
            p.waitFor();
            return "True".equalsIgnoreCase(result != null ? result.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void setupLogging() {
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), ".youdown");
            Files.createDirectories(logDir);
            File logFile = logDir.resolve("youdown.log").toFile();
            PrintStream ps = new PrintStream(new FileOutputStream(logFile, false));
            System.setErr(ps);
            System.setOut(ps);
            System.out.println("YouDown iniciando — " + new java.util.Date());
            System.out.println("Java: "  + System.getProperty("java.version"));
            System.out.println("OS: "    + System.getProperty("os.name"));
            System.out.println("Admin: " + isRunningAsAdmin());
            System.out.println("Dir: "   + System.getProperty("user.dir"));
            System.out.println("---");
        } catch (Exception ignored) {}
    }

    private static void customizeTheme() {
        UIManager.put("@accentColor", "#FF4444");
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("TabbedPane.tabArc", 8);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    }
}
