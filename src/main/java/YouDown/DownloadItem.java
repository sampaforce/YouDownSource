package YouDown;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DownloadItem {

    public enum Status {
        PENDING("Na fila"),
        DOWNLOADING("Baixando"),
        CONVERTING("Convertendo"),
        COMPLETED("Concluído"),
        ERROR("Erro"),
        CANCELLED("Cancelado");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Format {
        VIDEO_MP4("Vídeo MP4"),
        VIDEO_BEST("Vídeo Melhor Qualidade"),
        AUDIO_MP3("Áudio MP3"),
        AUDIO_M4A("Áudio M4A"),
        AUDIO_WAV("Áudio WAV");

        private final String label;
        Format(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private String url;
    private String title;
    private String outputDir;
    private Format format;
    private Status status;
    private int progress;
    private String speed;
    private String eta;
    private String fileSize;
    private String errorMessage;
    private LocalDateTime addedAt;
    private LocalDateTime completedAt;
    private String savedFilePath;

    public DownloadItem(String url, Format format, String outputDir) {
        this.url = url;
        this.format = format;
        this.outputDir = outputDir;
        this.status = Status.PENDING;
        this.progress = 0;
        this.title = "Obtendo informações...";
        this.addedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public Format getFormat() { return format; }
    public void setFormat(Format format) { this.format = format; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getAddedAt() { return addedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getSavedFilePath() { return savedFilePath; }
    public void setSavedFilePath(String savedFilePath) { this.savedFilePath = savedFilePath; }

    public String getFormattedAddedAt() {
        return addedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public boolean isAudio() {
        return format == Format.AUDIO_MP3 || format == Format.AUDIO_M4A || format == Format.AUDIO_WAV;
    }
}
