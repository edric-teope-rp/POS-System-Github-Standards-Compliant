package org.possystem.socket;

import java.time.LocalDateTime;

/**
 * Data model representing a discovered POS system on the network
 */
public class PosSystemInfo {
    private String posName;           // Custom name or hostname
    private String hostname;          // System hostname
    private String ipAddress;         // IP address
    private int port;                 // Server port
    private LocalDateTime lastSeen;   // Last discovery timestamp
    private boolean connected;        // Connection status
    private LocalDateTime lastLogTimestamp; // Last received log timestamp

    public PosSystemInfo(String posName, String hostname, String ipAddress, int port) {
        this.posName = posName;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSeen = LocalDateTime.now();
        this.connected = false;
    }

    // Getters and Setters
    public String getPosName() {
        return posName;
    }

    public void setPosName(String posName) {
        this.posName = posName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public LocalDateTime getLastLogTimestamp() {
        return lastLogTimestamp;
    }

    public void setLastLogTimestamp(LocalDateTime lastLogTimestamp) {
        this.lastLogTimestamp = lastLogTimestamp;
    }

    /**
     * Get unique identifier for this POS system
     */
    public String getIdentifier() {
        return ipAddress + ":" + port;
    }

    /**
     * Get display name (POS name if set, otherwise hostname)
     */
    public String getDisplayName() {
        return (posName != null && !posName.trim().isEmpty()) ? posName : hostname;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + ipAddress + ":" + port + ")";
    }
}
