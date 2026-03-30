package org.possystem.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for socket connections (persisted to JSON)
 */
public class SocketConfig {
    private String posName;           // Custom POS name
    private int serverPort;           // Server port (default 9000)
    private boolean autoStartServer;  // Auto-start server on launch
    private List<SavedConnection> savedConnections; // Manual connections

    public SocketConfig() {
        this.posName = "";
        this.serverPort = 8080;  // Default port changed from 9000 to 8080
        this.autoStartServer = false;
        this.savedConnections = new ArrayList<>();
    }

    public static class SavedConnection {
        private String posName;
        private String ipAddress;
        private int port;

        public SavedConnection(String posName, String ipAddress, int port) {
            this.posName = posName;
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public String getPosName() {
            return posName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }
    }

    // Getters and Setters
    public String getPosName() {
        return posName;
    }

    public void setPosName(String posName) {
        this.posName = posName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public boolean isAutoStartServer() {
        return autoStartServer;
    }

    public void setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
    }

    public List<SavedConnection> getSavedConnections() {
        return savedConnections;
    }

    public void setSavedConnections(List<SavedConnection> savedConnections) {
        this.savedConnections = savedConnections;
    }

    /**
     * Save configuration to JSON file
     */
    public void save(String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs(); // Create directories if needed

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save socket config: " + e.getMessage());
        }
    }

    /**
     * Load configuration from JSON file
     */
    public static SocketConfig load(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(file)) {
                    return gson.fromJson(reader, SocketConfig.class);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load socket config: " + e.getMessage());
        }
        return new SocketConfig(); // Return default if load fails
    }
}
