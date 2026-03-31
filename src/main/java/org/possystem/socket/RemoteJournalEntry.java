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
     * Supports multiple formats:
     * - Journal Server: {"type":"PRODUCT_ADDED","timestamp":1774877029.297165000,"terminalId":"TERMINAL_MAC","payload":{...}}
     * - Swings POS: {"type":"AUDIT_EVENT","timestamp":1711832629297,"eventType":"ITEM_ADDED","details":"..."}
     * - Legacy: {"eventTimestamp":"2026-03-16T21:14:29.90482","eventType":"ITEM_ADDED","details":"..."}
     */
    private static RemoteJournalEntry parseJsonFormat(String line, String posName, String ipAddress, boolean isLocal) {
        try {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();

            System.out.println("DEBUG: Parsing JSON from " + posName + " - Keys: " + json.keySet());

            // Extract timestamp (multiple format support)
            LocalDateTime timestamp;
            if (json.has("eventTimestamp")) {
                // Swings POS format: ISO-8601 string
                String timestampStr = json.get("eventTimestamp").getAsString();
                System.out.println("DEBUG: Found eventTimestamp: " + timestampStr);
                // Parse ISO format: 2026-03-30T14:30:29.297
                timestamp = LocalDateTime.parse(timestampStr.substring(0, Math.min(23, timestampStr.length())),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            } else if (json.has("timestamp")) {
                // Handle both milliseconds (long) and seconds with decimal (double)
                if (json.get("timestamp").isJsonPrimitive()) {
                    try {
                        // Try as long first (milliseconds)
                        long epochMilli = json.get("timestamp").getAsLong();
                        System.out.println("DEBUG: Found timestamp (long): " + epochMilli);
                        timestamp = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(epochMilli),
                            java.time.ZoneId.systemDefault()
                        );
                    } catch (NumberFormatException e) {
                        // Try as double (seconds with decimal - Journal Server format)
                        double epochSeconds = json.get("timestamp").getAsDouble();
                        System.out.println("DEBUG: Found timestamp (double seconds): " + epochSeconds);
                        long epochMilli = (long)(epochSeconds * 1000);
                        timestamp = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(epochMilli),
                            java.time.ZoneId.systemDefault()
                        );
                    }
                } else {
                    timestamp = LocalDateTime.now();
                }
            } else {
                System.out.println("DEBUG: No timestamp found, using current time");
                timestamp = LocalDateTime.now();
            }

            // Extract event type (action) - support multiple field names
            String action = "UNKNOWN";
            if (json.has("eventType")) {
                // Swings POS: "eventType":"ITEM_ADDED"
                action = json.get("eventType").getAsString();
                System.out.println("DEBUG: Found eventType: " + action);
                action = normalizeEventType(action);
            } else if (json.has("type")) {
                String typeValue = json.get("type").getAsString();
                System.out.println("DEBUG: Found type: " + typeValue);
                // Check if it's a wrapper type (like "AUDIT_EVENT")
                if ("AUDIT_EVENT".equals(typeValue) || "HANDSHAKE_REQUEST".equals(typeValue)
                    || "HANDSHAKE_RESPONSE".equals(typeValue)) {
                    // For Swings POS AUDIT_EVENT, eventType should be present
                    if (json.has("eventType")) {
                        action = json.get("eventType").getAsString();
                        action = normalizeEventType(action);
                    } else {
                        action = typeValue; // Use the wrapper type
                    }
                } else {
                    // Regular event type (Journal Server format: "PRODUCT_ADDED")
                    action = normalizeEventType(typeValue);
                }
            }

            // Extract details
            String details = "";
            if (json.has("details")) {
                details = json.get("details").getAsString();
                System.out.println("DEBUG: Found details: " + details);
            } else if (json.has("payload")) {
                // Journal Server format: payload contains details
                JsonObject payload = json.getAsJsonObject("payload");
                System.out.println("DEBUG: Found payload: " + payload);
                StringBuilder payloadDetails = new StringBuilder();
                payload.entrySet().forEach(entry -> {
                    payloadDetails.append(entry.getKey()).append(":").append(entry.getValue()).append("|");
                });
                details = payloadDetails.toString();
            }

            // Add additional info if available
            if (json.has("cashierName")) {
                String cashier = json.get("cashierName").getAsString();
                details = "Cashier:" + cashier + "|" + details;
            }

            if (json.has("transactionId") && json.get("transactionId").isJsonPrimitive()) {
                String txnId = json.get("transactionId").getAsString();
                details = details + "|TXN:" + txnId;
            }

            if (json.has("amount") && !json.get("amount").isJsonNull()) {
                double amount = json.get("amount").getAsDouble();
                details = details + "|Amount:$" + String.format("%.2f", amount);
            }

            System.out.println("DEBUG: Parsed JSON successfully - Action: " + action + ", Details: " + details);

            return new RemoteJournalEntry(timestamp, posName, ipAddress, action, details, isLocal);

        } catch (Exception e) {
            System.err.println("ERROR: Failed to parse JSON format: " + e.getMessage());
            System.err.println("ERROR: JSON line was: " + line);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Normalize event types from different POS systems
     * Maps various event type names to standardized internal format
     */
    private static String normalizeEventType(String eventType) {
        // Map common variations to standard format
        return switch (eventType) {
            // Swings POS events
            case "ITEM_ADDED" -> "ITEM_ADD";
            case "ITEM_VOIDED" -> "ITEM_VOID";
            case "TRANSACTION_CREATED" -> "TX_START";
            case "TRANSACTION_COMPLETED" -> "PAYMENT_COMPLETE";
            case "TRANSACTION_CANCELLED" -> "TX_VOID";
            case "PAYMENT_COMPLETED" -> "PAYMENT_COMPLETE";
            case "PAYMENT_INITIATED" -> "PAYMENT_START";
            case "PAYMENT_FAILED" -> "PAYMENT_ERROR";
            case "PAYMENT_CANCELLED" -> "PAYMENT_CANCEL";
            case "QUANTITY_UPDATED" -> "QTY_CHANGE";
            case "CART_CLEARED" -> "BASKET_VOID";
            case "CART_LOCKED" -> "TX_FINALIZE";
            case "DISCOUNT_APPLIED" -> "DISCOUNT_ADD";
            case "CASH_DRAWER_OPENED" -> "DRAWER_OPEN";
            case "BARCODE_LOOKUP" -> "BARCODE_SCAN";
            case "PRODUCT_SEARCH" -> "SEARCH";

            // Journal Server events (if they use different naming)
            case "PRODUCT_ADDED" -> "ITEM_ADD";
            case "LINE_ITEM_VOIDED" -> "ITEM_VOID";
            case "BASKET_VOIDED" -> "BASKET_VOID";
            case "QUANTITY_CHANGED" -> "QTY_CHANGE";
            case "TRANSACTION_FINALIZED" -> "TX_FINALIZE";
            case "PAYMENT_PROCESSED" -> "PAYMENT_COMPLETE";

            // Legacy formats
            case "ITEM_REMOVED" -> "ITEM_VOID";
            case "TRANSACTION_VOIDED" -> "TX_VOID";
            case "AUDIT_EVENT" -> "AUDIT";

            // Keep as-is if not in mapping
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
