package YouDown;

import javax.swing.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Mantém o yt-dlp atualizado sozinho.
 *
 * O YouTube muda a extração a cada poucas semanas e um yt-dlp velho falha com
 * erros que parecem bug do app (403 Forbidden, PO Token, playlist truncada).
 * Esta classe roda "yt-dlp -U" em segundo plano na abertura do programa,
 * no máximo uma vez a cada N dias (AppConfig.autoUpdateDays).
 */
public class YtdlpUpdater {

    private static YtdlpUpdater instance;

    /** Verdadeiro enquanto o binário está sendo substituído. */
    private volatile boolean updating = false;

    private YtdlpUpdater() {}

    public static YtdlpUpdater getInstance() {
        if (instance == null) instance = new YtdlpUpdater();
        return instance;
    }

    public boolean isUpdating() { return updating; }

    // ── Versão ───────────────────────────────────────────────────────────────

    /**
     * Versão instalada, no formato de data do release (ex: "2026.08.19"),
     * ou null se o yt-dlp não for encontrado.
     */
    public String getVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    AppConfig.getInstance().getYtdlpPath(), "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return out.isEmpty() ? null : out;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Idade da versão instalada em dias, ou -1 se não der para determinar.
     * A versão do yt-dlp É a data do release, então dá para calcular direto.
     */
    public long getVersionAgeDays() {
        return versionAgeDays(getVersion());
    }

    static long versionAgeDays(String version) {
        if (version == null || version.length() < 10) return -1;
        try {
            LocalDate release = LocalDate.parse(version.trim().substring(0, 10),
                    DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            return ChronoUnit.DAYS.between(release, LocalDate.now());
        } catch (Exception e) {
            return -1;
        }
    }

    // ── Atualização ──────────────────────────────────────────────────────────

    /**
     * Executa "yt-dlp -U" e devolve a saída bruta do processo.
     * Bloqueia a thread chamadora — use sempre fora da EDT.
     */
    public String runUpdate() throws IOException, InterruptedException {
        updating = true;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    AppConfig.getInstance().getYtdlpPath(), "-U");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return out;
        } finally {
            updating = false;
        }
    }

    /** O yt-dlp avisa na saída quando de fato trocou de versão. */
    static boolean didUpdate(String output) {
        return output != null && output.contains("Updated yt-dlp");
    }

    static boolean isUpToDate(String output) {
        return output != null && output.contains("up to date");
    }

    /**
     * Espera o binário parar de ser substituído antes de iniciar um download —
     * chamar o yt-dlp no meio da troca do arquivo daria um erro confuso.
     * Devolve false se estourar o tempo.
     */
    public boolean awaitIdle(long timeoutMillis) {
        long limite = System.currentTimeMillis() + timeoutMillis;
        while (updating) {
            if (System.currentTimeMillis() > limite) return false;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    // ── Verificação automática na abertura ───────────────────────────────────

    /**
     * Decide se a verificação periódica deve rodar agora.
     */
    private boolean shouldCheckNow(AppConfig cfg) {
        if (!cfg.isAutoUpdateYtdlp()) return false;

        long dias = Math.max(cfg.getAutoUpdateDays(), 1);
        long intervalo = dias * 24L * 60L * 60L * 1000L;
        long desde = System.currentTimeMillis() - cfg.getLastYtdlpCheck();

        // lastYtdlpCheck == 0 (primeira execução) cai aqui e verifica
        return desde >= intervalo;
    }

    /**
     * Chamado pela MainWindow. Não bloqueia a interface e não incomoda o
     * usuário quando não há nada a fazer: só avisa se atualizou de fato,
     * se falhou, ou se a versão está velha e o auto-update está desligado.
     */
    public void checkOnStartup(JFrame parent) {
        AppConfig cfg = AppConfig.getInstance();

        if (!shouldCheckNow(cfg)) {
            avisarSeVelhoDemais(parent, cfg);
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                return runUpdate();
            }
            @Override protected void done() {
                String saida;
                try {
                    saida = get();
                } catch (Exception e) {
                    // Sem internet, sem permissão de escrita etc: silencioso.
                    // O erro real aparece na fila se um download falhar.
                    System.out.println("Auto-update do yt-dlp falhou: " + e.getMessage());
                    return;
                }

                System.out.println("Auto-update do yt-dlp: " + saida);

                // Só marca a data quando a verificação chegou ao fim
                cfg.setLastYtdlpCheck(System.currentTimeMillis());
                cfg.save();

                if (didUpdate(saida)) {
                    JOptionPane.showMessageDialog(parent,
                            "<html><b>yt-dlp atualizado!</b><br><br>" +
                            "Versão instalada: " + getVersion() + "<br><br>" +
                            "Isso mantém os downloads funcionando quando o<br>" +
                            "YouTube muda.</html>",
                            "Atualização automática", JOptionPane.INFORMATION_MESSAGE);
                } else if (!isUpToDate(saida)) {
                    // Não atualizou nem confirmou estar em dia: provavelmente
                    // faltou permissão de escrita no binário.
                    avisarFalha(parent, saida);
                }
            }
        };
        worker.execute();
    }

    /**
     * Auto-update desligado (ou verificado há pouco) mas o binário está velho:
     * avisa, porque é a causa nº 1 de "não baixa".
     */
    private void avisarSeVelhoDemais(JFrame parent, AppConfig cfg) {
        SwingWorker<Long, Void> w = new SwingWorker<>() {
            @Override protected Long doInBackground() { return getVersionAgeDays(); }
            @Override protected void done() {
                try {
                    long dias = get();
                    if (dias < 90) return;
                    int res = JOptionPane.showConfirmDialog(parent,
                            "<html>Seu yt-dlp tem <b>" + dias + " dias</b>.<br><br>" +
                            "Versões antigas causam erro <b>403 Forbidden</b> e<br>" +
                            "playlists incompletas, porque o YouTube muda a<br>" +
                            "forma de extrair os vídeos.<br><br>" +
                            "Deseja atualizar agora?</html>",
                            "yt-dlp desatualizado",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (res == JOptionPane.YES_OPTION) updateNow(parent, null);
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    /**
     * Atualização disparada pelo usuário (botão das Configurações ou o aviso
     * acima). Mostra o resultado e devolve o controle via onDone.
     */
    public void updateNow(JFrame parent, Runnable onDone) {
        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                return runUpdate();
            }
            @Override protected void done() {
                try {
                    String saida = get();
                    AppConfig.getInstance().setLastYtdlpCheck(System.currentTimeMillis());
                    AppConfig.getInstance().save();

                    boolean ok = didUpdate(saida) || isUpToDate(saida);
                    if (ok) {
                        JOptionPane.showMessageDialog(parent,
                                (didUpdate(saida)
                                        ? "✅ yt-dlp atualizado para " + getVersion()
                                        : "✅ O yt-dlp já está na última versão."),
                                "Atualizar yt-dlp", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        avisarFalha(parent, saida);
                    }
                } catch (Exception e) {
                    avisarFalha(parent, e.getMessage());
                } finally {
                    if (onDone != null) onDone.run();
                }
            }
        };
        w.execute();
    }

    private void avisarFalha(JFrame parent, String detalhe) {
        JOptionPane.showMessageDialog(parent,
                "<html><b>Não foi possível atualizar o yt-dlp.</b><br><br>" +
                "Causa mais comum: o arquivo está numa pasta protegida.<br>" +
                "Feche o YouDown e abra como <b>Administrador</b>.<br><br>" +
                "<font color='#888888'>Detalhe: " +
                (detalhe == null ? "desconhecido" : detalhe.replace("\n", "<br>")) +
                "</font></html>",
                "Atualizar yt-dlp", JOptionPane.WARNING_MESSAGE);
    }
}
