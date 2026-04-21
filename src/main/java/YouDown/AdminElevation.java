package YouDown;

import javax.swing.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.List;

public class AdminElevation {

    /**
     * Chame como PRIMEIRA linha do main().
     * Se não for admin, relança como admin e encerra a instância atual.
     */
    public static void ensureAdmin() {
        if (!isWindows()) return;
        if (isRunningAsAdmin()) return;
        try {
            relaunchAsAdmin();
        } catch (Exception e) {
            // Falhou a elevação — continua sem admin com aviso
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "<html><b>Aviso:</b> Não foi possível obter privilégios de Administrador.<br>" +
                                    "Algumas funções podem falhar se o app estiver em pasta protegida.</html>",
                            "Sem privilégios de Admin",
                            JOptionPane.WARNING_MESSAGE)
            );
        }
    }

    /**
     * Verifica privilégio de admin usando a API do Windows via PowerShell.
     * Mais confiável que "net session" em todos os ambientes.
     */
    public static boolean isRunningAsAdmin() {
        if (!isWindows()) return true;
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

    private static void relaunchAsAdmin() throws Exception {
        // Caminho do java/javaw atual
        String javaExe = ProcessHandle.current()
                .info()
                .command()
                .orElse(System.getProperty("java.home") + "\\bin\\javaw.exe")
                .replace("java.exe", "javaw.exe"); // sem janela de console

        // Caminho do JAR em execução
        String jarPath = getJarPath();

        // Argumentos extras da JVM (ex: -Xmx)
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        StringBuilder extraArgs = new StringBuilder();
        for (String a : jvmArgs) {
            if (!a.startsWith("-agentlib") && !a.startsWith("-javaagent")) {
                extraArgs.append(" \"").append(a.replace("\"", "\\\"")).append("\"");
            }
        }

        String args;
        if (jarPath != null && (jarPath.endsWith(".jar") || jarPath.endsWith(".JAR"))) {
            // Rodando como JAR
            args = extraArgs + " -jar \"" + jarPath + "\"";
        } else {
            // Rodando pela IDE — usa o classpath
            String cp = System.getProperty("java.class.path");
            args = extraArgs + " -cp \"" + cp + "\" YouDown.Main";
        }

        // Usa PowerShell Start-Process com -Verb RunAs (dispara UAC)
        // Cada argumento é passado separadamente para evitar problemas com espaços
        String psCommand = String.format(
                "Start-Process -FilePath '%s' -ArgumentList '%s' -Verb RunAs -WindowStyle Normal",
                javaExe.replace("'", "''"),
                args.trim().replace("'", "''")
        );

        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", psCommand
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Aguarda um pouco para garantir que o PowerShell iniciou
        Thread.sleep(2000);

        // Verifica se o PowerShell retornou erro
        if (!p.isAlive() && p.exitValue() != 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder err = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) err.append(line).append("\n");
            throw new Exception("PowerShell retornou erro:\n" + err);
        }

        System.exit(0);
    }

    /**
     * Retorna o caminho absoluto do JAR em execução.
     * Funciona tanto com JAR quanto rodando pela IDE.
     */
    private static String getJarPath() {
        try {
            String path = AdminElevation.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            // Remove barra inicial no Windows (/C:/... → C:/...)
            if (isWindows() && path.startsWith("/")) {
                path = path.substring(1);
            }

            // Converte barras para o padrão Windows
            path = path.replace("/", "\\");

            return path;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}