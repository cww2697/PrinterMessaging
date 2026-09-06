package net.canyonwolf.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigManager {
    private static final String APP_DIR_NAME = ".printermessaging";
    private static final String CONFIG_FILE_NAME = "config.properties";

    private final File configFile;
    private AppConfig currentConfig;

    public ConfigManager() {
        this(getDefaultConfigFile());
    }

    public ConfigManager(File configFile) {
        this.configFile = configFile;
        this.currentConfig = loadConfig();
    }

    public static File getDefaultConfigFile() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.trim().isEmpty()) {
            userHome = ".";
        }
        File configDir = new File(userHome, APP_DIR_NAME);
        return new File(configDir, CONFIG_FILE_NAME);
    }

    public synchronized AppConfig loadConfig() {
        AppConfig config = new AppConfig();
        if (configFile != null && configFile.exists() && configFile.isFile()) {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
                config = AppConfig.fromProperties(props);
            } catch (IOException e) {
                System.err.println("Warning: Unable to read config file from " + configFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        this.currentConfig = config;
        return config;
    }

    public synchronized boolean saveConfig(AppConfig config) {
        if (config == null) return false;
        this.currentConfig = config;
        if (configFile == null) return false;

        try {
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            Properties props = config.toProperties();
            try (OutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "PrinterMessaging Configuration");
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error saving config file to " + configFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    public AppConfig getConfig() {
        if (currentConfig == null) {
            currentConfig = loadConfig();
        }
        return currentConfig;
    }

    public File getConfigFile() {
        return configFile;
    }
}
