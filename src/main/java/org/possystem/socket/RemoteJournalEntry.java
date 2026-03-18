package org.possystem.socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data model for a journal entry (local or remote)
 */
public class RemoteJournalEntry implements Comparable<RemoteJournalEntry> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private LocalDateTime timestamp;
    private String posName;           // Source POS name
    private String ipAddress;         // Source IP
    private String action;            // ACTION from journal
    private String details;           // DETAILS from journal
    private boolean isLocal;          // True if from local POS

    public RemoteJournalEntry(LocalDateTime timestamp, String posName, String ipAddress,
                              String action, String details, boolean isLocal) {
        this.timestamp = timestamp;
        this.posName = posName;
        this.ipAddress = ipAddress;
        this.action = action;
        this.details = details;
        this.isLocal = isLocal;
    }

    /**
     * Parse a journal line: Supports both JSON and pipe-delimited formats
     * - JSON: {"eventTimestamp":"...", "eventType":"...", "details":"..."}
     * - Pipe-delimited: YYYY-MM-DD HH:mm:ss.SSS|ACTION|DETAILS
     */
    public static RemoteJournalEntry parseJournalLine(String line, String posName, String ipAddress, boolean isLocal) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        line = line.trim();

        // Detect format: JSON starts with "{"
        if (line.startsWith("{")) {
            return parseJsonFormat(line, posName, ipAddress, isLocal);
        } else {
            return parsePipeDelimitedFormat(line, posName, ipAddress, isLocal);
        }
    }

    /**
     * Parse pipe-delimited format: YYYY-MM-DD HH:mm:ss.SSS|ACTION|DETAILS
     */
    private static RemoteJournalEntry parsePipeDelimitedFormat(String line, String posName, String ipAddress, boolean isLocal) {
        try {
            String[] parts = line.split("\\|", 3);
            if (parts.length >= 2) {
                LocalDateTime timestamp = LocalDateTime.parse(parts[0], FORMATTER);
                String action = parts[1];
                String details = parts.length >= 3 ? parts[2] : "";
                return new RemoteJournalEntry(timestamp, posName, ipAddress, action, details, isLocal);
            }
        } catch (Exception e) {
            // Invalid format
            System.err.println("ERROR: Failed to parse pipe-delimited format: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse JSON format from external POS systems
     * Example: {"eventTimestamp":"2026-03-16T21:14:29.90482","eventType":"ITEM_ADDED","details":"..."}
     */
    private static RemoteJournalEntry parseJsonFormat(String line, String posName, String ipAddress, boolean isLocal) {
        try {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();

            // Extract timestamp
            LocalDateTime timestamp;
            if (json.has("eventTimestamp")) {
                String timestampStr = json.get("eventTimestamp").getAsString();
                // Parse ISO format: 2026-03-16T21:14:29.90482
                timestamp = LocalDateTime.parse(timestampStr.substring(0, 23),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            } else if (json.has("timestamp")) {
                long epochMilli = json.get("timestamp").getAsLong();
                timestamp = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(epochMilli),
                    java.time.ZoneId.systemDefault()
                );
            } else {
                timestamp = LocalDateTime.now();
            }

            // Extract event type (action)
            String action = "UNKNOWN";
            if (json.has("eventType")) {
                action = json.get("eventType").getAsString();
                // Normalize: ITEM_ADDED → ITEM_ADD
                action = normalizeEventType(action);
            } else if (json.has("type")) {
                action = json.get("type").getAsString();
            }

            // Extract details
            String details = "";
            if (json.has("details")) {
                details = json.get("details").getAsString();
            }

            // Add additional info if available
            if (json.has("cashierName")) {
                String cashier = json.get("cashierName").getAsString();
                details = "Cashier:" + cashier + "|" + details;
            }

            if (json.has("amount")) {
                double amount = json.get("amount").getAsDouble();
                details = details + "|Amount:" + String.format("%.2f", amount);
            }

            System.out.println("DEBUG: Parsed JSON - Action: " + action + ", Details: " + details);

            return new RemoteJournalEntry(timestamp, posName, ipAddress, action, details, isLocal);

        } catch (Exception e) {
            System.err.println("ERROR: Failed to parse JSON format: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Normalize event types from different POS systems
     */
    private static String normalizeEventType(String eventType) {
        // Map common variations to standard format
        return switch (eventType) {
            case "ITEM_ADDED" -> "ITEM_ADD";
            case "ITEM_REMOVED" -> "ITEM_VOID";
            case "TRANSACTION_COMPLETED" -> "PAYMENT_COMPLETE";
            case "TRANSACTION_VOIDED" -> "TX_VOID";
            case "AUDIT_EVENT" -> "AUDIT";
            default -> eventType;
        };
    }

    /**
     * Format as journal line: YYYY-MM-DD HH:mm:ss.SSS|ACTION|DETAILS
     */
    public String toJournalLine() {
        return timestamp.format(FORMATTER) + "|" + action + "|" + details;
    }

    /**
     * Format for display with POS name prefix (for console and Live Journal Viewer)
     */
    public String toDisplayString() {
        String prefix = isLocal ? "LOCAL" : (posName + " (" + ipAddress + ")");
        return prefix + "|" + timestamp.format(FORMATTER) + "|" + action + "|" + details;
    }

    // Getters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPosName() {
        return posName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public int compareTo(RemoteJournalEntry other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
