package YouDown;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;

public class AppConfig {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".youdown";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";

    private static AppConfig instance;

    private String defaultDownloadDir;
    private String ytdlpPath;
    private String cookiesPath;
    private boolean useCookies;
    private int maxConcurrentDownloads;
    private boolean darkTheme;
    private String defaultFormat;

    private AppConfig() {
        // Valores padrão
        defaultDownloadDir = System.getProperty("user.home") + File.separator + "Downloads";
        ytdlpPath = "yt-dlp";
        cookiesPath = "youtube_cookies.txt";
        useCookies = true;
        maxConcurrentDownloads = 2;
        darkTheme = true;
        defaultFormat = "VIDEO_MP4";
        load();
    }

    public static AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    public void load() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) return;

            String content = new String(Files.readAllBytes(configFile.toPath()));
            JSONObject json = new JSONObject(content);

            defaultDownloadDir = json.optString("defaultDownloadDir", defaultDownloadDir);
            ytdlpPath = json.optString("ytdlpPath", ytdlpPath);
            cookiesPath = json.optString("cookiesPath", cookiesPath);
            useCookies = json.optBoolean("useCookies", useCookies);
            maxConcurrentDownloads = json.optInt("maxConcurrentDownloads", maxConcurrentDownloads);
            darkTheme = json.optBoolean("darkTheme", darkTheme);
            defaultFormat = json.optString("defaultFormat", defaultFormat);
        } catch (Exception e) {
            // usa defaults se falhar
        }
    }

    public void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            JSONObject json = new JSONObject();
            json.put("defaultDownloadDir", defaultDownloadDir);
            json.put("ytdlpPath", ytdlpPath);
            json.put("cookiesPath", cookiesPath);
            json.put("useCookies", useCookies);
            json.put("maxConcurrentDownloads", maxConcurrentDownloads);
            json.put("darkTheme", darkTheme);
            json.put("defaultFormat", defaultFormat);

            Files.write(Paths.get(CONFIG_FILE), json.toString(2).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters e Setters
    public String getDefaultDownloadDir() { return defaultDownloadDir; }
    public void setDefaultDownloadDir(String v) { this.defaultDownloadDir = v; }

    public String getYtdlpPath() { return ytdlpPath; }
    public void setYtdlpPath(String v) { this.ytdlpPath = v; }

    public String getCookiesPath() { return cookiesPath; }
    public void setCookiesPath(String v) { this.cookiesPath = v; }

    public boolean isUseCookies() { return useCookies; }
    public void setUseCookies(boolean v) { this.useCookies = v; }

    public int getMaxConcurrentDownloads() { return maxConcurrentDownloads; }
    public void setMaxConcurrentDownloads(int v) { this.maxConcurrentDownloads = v; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean v) { this.darkTheme = v; }

    public String getDefaultFormat() { return defaultFormat; }
    public void setDefaultFormat(String v) { this.defaultFormat = v; }
}
