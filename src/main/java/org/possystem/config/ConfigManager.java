package org.possystem.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration Manager - Handles persistent application settings
 * Stores configuration in ~/.possystem/config.properties
 */
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.possystem";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";

    // Default values
    private static final String DEFAULT_DISCOUNT_API_URL = "http://localhost:8080/api/discounts";

    // Property keys
    private static final String KEY_DISCOUNT_API_URL = "api.discount.url";

    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    /**
     * Load configuration from properties file
     * Creates file with defaults if it doesn't exist
     */
    public void loadConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);

            // Create config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                System.out.println("Created config directory: " + CONFIG_DIR);
            }

            // Load existing config or create with defaults
            if (Files.exists(configPath)) {
                try (InputStream input = new FileInputStream(CONFIG_FILE)) {
                    properties.load(input);
                    System.out.println("Loaded configuration from: " + CONFIG_FILE);
                }
            } else {
                // Create default configuration
                setDefaults();
                saveConfig();
                System.out.println("Created default configuration: " + CONFIG_FILE);
            }

        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            // Fall back to defaults
            setDefaults();
        }
    }

    /**
     * Save configuration to properties file
     */
    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "POS System Configuration");
            System.out.println("Configuration saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set default values for all properties
     */
    private void setDefaults() {
        properties.setProperty(KEY_DISCOUNT_API_URL, DEFAULT_DISCOUNT_API_URL);
    }

    /**
     * Get the Discount Engine API URL
     */
    public String getDiscountApiUrl() {
        return properties.getProperty(KEY_DISCOUNT_API_URL, DEFAULT_DISCOUNT_API_URL);
    }

    /**
     * Set the Discount Engine API URL and save to file
     */
    public void setDiscountApiUrl(String url) {
        properties.setProperty(KEY_DISCOUNT_API_URL, url);
        saveConfig();
    }

    /**
     * Get the configuration file path (for display purposes)
     */
    public String getConfigFilePath() {
        return CONFIG_FILE;
    }

    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults() {
        setDefaults();
        saveConfig();
    }
}
