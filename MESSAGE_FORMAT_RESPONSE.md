# POS Message Format Response

**Generated:** 2026-03-30
**POS System Name:** Swings POS - Virtual Journal System
**Analyzed By:** Claude Code

---

## 1. SYSTEM IDENTIFICATION

- **POS System Name:** Swings POS Virtual Journal System
- **Version:** 3.1.0 (release/3.1.0 branch)
- **Broadcast IP:Port:** 0.0.0.0:8765 (default, configurable via JOURNAL_PORT environment variable)
- **Network Protocol:** TCP
- **Message Format:** JSON (newline-delimited)

---

## 2. MESSAGE STRUCTURE

### Top-Level Fields

All messages extend the base `JournalMessage` class with these fields:

| Field Name | Data Type | Required | Description | Example Value |
|------------|-----------|----------|-------------|---------------|
| type | String (Enum) | Yes | Message protocol type | "AUDIT_EVENT" |
| timestamp | Long | Yes | Message timestamp in milliseconds | 1711832629297 |

### Message Types (JournalProtocol Enum)

| Protocol Type | Description |
|---------------|-------------|
| HANDSHAKE_REQUEST | Client sends identity during connection setup |
| HANDSHAKE_RESPONSE | Server responds with identity to complete handshake |
| AUDIT_EVENT | Real-time audit event streaming (main message type) |
| PING | Keep-alive ping message |
| PONG | Keep-alive pong response |
| DISCONNECT | Graceful connection shutdown notification |
| ERROR | Error notification |

### Nested Objects/Payload Structure

**AuditEventMessage Fields (extends JournalMessage):**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| type | String | Always "AUDIT_EVENT" | "AUDIT_EVENT" |
| timestamp | Long | Message creation time (milliseconds) | 1711832629297 |
| eventId | String | Unique event identifier | "AUD-123e4567-e89b-12d3-a456-426614174000" |
| eventTimestamp | String | Event occurrence time (ISO-8601) | "2026-03-30T14:30:29.297" |
| eventType | String | Type of audit event | "ITEM_ADDED" |
| cashierId | Long | Cashier ID | 101 |
| cashierName | String | Cashier name | "John Smith" |
| transactionId | String | Transaction ID (nullable) | "TXN-2026-03-30-001" |
| details | String | Event description | "Added: Coca Cola 12oz" |
| amount | BigDecimal | Amount involved (nullable) | 1.99 |

**HandshakeMessage Fields (extends JournalMessage):**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| type | String | "HANDSHAKE_REQUEST" or "HANDSHAKE_RESPONSE" | "HANDSHAKE_REQUEST" |
| timestamp | Long | Message creation time (milliseconds) | 1711832629297 |
| ipAddress | String | IP address of sender | "192.168.1.100" |
| machineName | String | Machine name/identifier | "POS-Terminal-01" |
| status | String | Response status (nullable, only in RESPONSE) | "OK" |

---

## 3. ACTUAL MESSAGE EXAMPLES

### Example 1: Item Added to Cart

```json
{
  "type": "AUDIT_EVENT",
  "timestamp": 1711832629297,
  "eventId": "AUD-123e4567-e89b-12d3-a456-426614174000",
  "eventTimestamp": "2026-03-30T14:30:29.297",
  "eventType": "ITEM_ADDED",
  "cashierId": 101,
  "cashierName": "John Smith",
  "transactionId": "TXN-2026-03-30-001",
  "details": "Added: Coca Cola 12oz",
  "amount": 1.99
}
```

**File:** `src/main/java/com/swings/pos/domain/journal/network/JournalServer.java`
**Method:** `broadcast(AuditEventMessage event)` at line 166

---

### Example 2: Transaction Started

```json
{
  "type": "AUDIT_EVENT",
  "timestamp": 1711832620000,
  "eventId": "AUD-223e4567-e89b-12d3-a456-426614174001",
  "eventTimestamp": "2026-03-30T14:30:20.000",
  "eventType": "TRANSACTION_CREATED",
  "cashierId": 101,
  "cashierName": "John Smith",
  "transactionId": "TXN-2026-03-30-001",
  "details": "New transaction initiated",
  "amount": null
}
```

**File:** `src/main/java/com/swings/pos/domain/audit/AuditLogger.java`
**Method:** `logEvent()` at line 49, then broadcast via listener pattern

---

### Example 3: Payment Processed

```json
{
  "type": "AUDIT_EVENT",
  "timestamp": 1711832650123,
  "eventId": "AUD-323e4567-e89b-12d3-a456-426614174002",
  "eventTimestamp": "2026-03-30T14:30:50.123",
  "eventType": "PAYMENT_COMPLETED",
  "cashierId": 101,
  "cashierName": "John Smith",
  "transactionId": "TXN-2026-03-30-001",
  "details": "Payment completed: Cash",
  "amount": 25.47
}
```

**File:** `src/main/java/com/swings/pos/domain/audit/AuditLogger.java`
**Method:** `logEvent()` at line 49

---

### Example 4: Transaction Voided

```json
{
  "type": "AUDIT_EVENT",
  "timestamp": 1711832640000,
  "eventId": "AUD-423e4567-e89b-12d3-a456-426614174003",
  "eventTimestamp": "2026-03-30T14:30:40.000",
  "eventType": "TRANSACTION_CANCELLED",
  "cashierId": 101,
  "cashierName": "John Smith",
  "transactionId": "TXN-2026-03-30-001",
  "details": "Transaction cancelled by cashier",
  "amount": null
}
```

**File:** `src/main/java/com/swings/pos/domain/audit/AuditLogger.java`
**Method:** `logEvent()` at line 49

---

### Example 5: Handshake Request (Connection Establishment)

```json
{
  "type": "HANDSHAKE_REQUEST",
  "timestamp": 1711832600000,
  "ipAddress": "192.168.1.100",
  "machineName": "POS-Terminal-01"
}
```

**File:** `src/main/java/com/swings/pos/domain/journal/network/JournalClient.java`
**Method:** `performHandshake()` at line 138

---

### Example 6: Handshake Response

```json
{
  "type": "HANDSHAKE_RESPONSE",
  "timestamp": 1711832600100,
  "ipAddress": "192.168.1.50",
  "machineName": "POS-Server",
  "status": "OK"
}
```

**File:** `src/main/java/com/swings/pos/domain/journal/network/ClientConnection.java`
**Method:** `performHandshake()` at line 139

---

## 4. CODE SNIPPETS

### Message Creation Code

**File:** `src/main/java/com/swings/pos/domain/journal/model/AuditEventMessage.java`
**Lines:** 32-42

```java
/**
 * Creates an audit event message from an AuditEvent.
 *
 * @param event The audit event to wrap
 */
public AuditEventMessage(AuditEvent event) {
    super(JournalProtocol.AUDIT_EVENT);
    this.eventId = event.getEventId();
    this.eventTimestamp = event.getTimestamp().format(FORMATTER);
    this.eventType = event.getEventType().name();
    this.cashierId = event.getCashierId();
    this.cashierName = event.getCashierName();
    this.transactionId = event.getTransactionId();
    this.details = event.getDetails();
    this.amount = event.getAmount();
}
```

---

### Message Broadcasting Code

**File:** `src/main/java/com/swings/pos/domain/journal/network/JournalServer.java`
**Lines:** 162-170

```java
/**
 * Broadcasts an audit event to all connected clients.
 *
 * @param event Event to broadcast
 */
public void broadcast(AuditEventMessage event) {
    for (ClientConnection connection : activeConnections) {
        connection.sendMessage(event);
    }
}
```

**File:** `src/main/java/com/swings/pos/domain/journal/network/ClientConnection.java`
**Lines:** 293-315

```java
/**
 * Sends a message to the remote client.
 *
 * @param message Message to send
 */
public void sendMessage(Object message) {
    if (!running || writer == null) {
        return;
    }

    try {
        String json = gson.toJson(message);
        writer.println(json);

        if (writer.checkError()) {
            System.err.println("[JOURNAL] Error writing to client: " + connectionInfo.getRemoteIp());
            close();
        }
    } catch (Exception e) {
        System.err.println("[JOURNAL] Failed to send message: " + e.getMessage());
        close();
    }
}
```

---

### Message Class/Record Definition

**File:** `src/main/java/com/swings/pos/domain/journal/model/JournalMessage.java`

```java
package com.swings.pos.domain.journal.model;

/**
 * Base message for Virtual Journal protocol.
 * All messages transmitted between POS machines extend this class.
 */
public class JournalMessage {

    private final JournalProtocol type;
    private final long timestamp;

    /**
     * Creates a journal message.
     *
     * @param type Message type
     * @param timestamp Message timestamp in milliseconds
     */
    public JournalMessage(JournalProtocol type, long timestamp) {
        if (type == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        this.type = type;
        this.timestamp = timestamp;
    }

    /**
     * Creates a journal message with current timestamp.
     *
     * @param type Message type
     */
    public JournalMessage(JournalProtocol type) {
        this(type, System.currentTimeMillis());
    }

    public JournalProtocol getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
```

---

## 5. PROTOCOL DETAILS

### Serialization

- **Library Used:** Gson (com.google.gson)
- **Configuration:** Custom type adapters for flexible timestamp parsing and protocol type handling
- **Date/Time Format:**
  - `timestamp` field: Long milliseconds (e.g., 1711832629297)
  - `eventTimestamp` field: ISO-8601 LocalDateTime (e.g., "2026-03-30T14:30:29.297")

**Special Gson Configuration:**

```java
// From JournalGsonFactory.java
public static Gson create() {
    return new GsonBuilder()
            .registerTypeAdapter(JournalMessage.class, new JournalMessageDeserializer())
            .registerTypeAdapter(Long.class, new FlexibleLongDeserializer())
            .registerTypeAdapter(long.class, new FlexibleLongDeserializer())
            .create();
}
```

**Flexible Timestamp Handling:**
- Accepts timestamps as both milliseconds (long) and seconds with decimal (double)
- Automatically converts seconds to milliseconds if value < 10,000,000,000

### Network Transmission

- **Line Termination:** `\n` (newline character)
- **Character Encoding:** UTF-8
- **Message Delimiter:** Newline (`\n`) - each message is one complete line
- **Maximum Message Size:** No explicit limit (limited by TCP buffer size)

### Special Formatting

- **Prefixes:** None - pure JSON
- **Wrappers:** None - each message is a standalone JSON object
- **Escaping:** Standard JSON escaping handled by Gson
- **Compression:** None

### Socket Configuration

- **Auto-flush:** Enabled on PrintWriter
- **Socket Timeout:** 60,000ms (1 minute) for read operations
- **Handshake Timeout:** 30,000ms (30 seconds)
- **Keep-alive:** PING/PONG messages every 30,000ms (30 seconds)

---

## 6. EVENT TYPES

All possible `AuditEventType` values that can appear in messages:

| Event Type String | Description | Frequency |
|-------------------|-------------|-----------|
| ITEM_ADDED | Item added to cart | Common |
| ITEM_VOIDED | Item voided from cart | Common |
| QUANTITY_UPDATED | Item quantity changed | Common |
| CART_CLEARED | All items removed from cart | Common |
| CART_LOCKED | Cart locked for payment | Common |
| DISCOUNT_APPLIED | Discount applied to cart or item | Common |
| MANAGER_OVERRIDE | Manager authorization required | Rare |
| AGE_RESTRICTED_SALE | Age-restricted product sold | Common |
| AGE_VERIFICATION_APPROVED | Age verification approved | Common |
| AGE_VERIFICATION_DENIED | Age verification denied | Rare |
| TRANSACTION_CREATED | New transaction initiated | Common |
| TRANSACTION_COMPLETED | Transaction finalized | Common |
| TRANSACTION_CANCELLED | Transaction cancelled | Common |
| PAYMENT_INITIATED | Payment process started | Common |
| PAYMENT_COMPLETED | Payment successful | Common |
| PAYMENT_FAILED | Payment failed | Rare |
| PAYMENT_CANCELLED | Payment cancelled by user | Rare |
| CASH_DRAWER_OPENED | Cash drawer opened | Common |
| RECEIPT_GENERATED | Receipt data generated | Common |
| RECEIPT_PRINTED | Receipt printed | Common |
| LOGIN | Cashier logged in | Rare |
| LOGOUT | Cashier logged out | Rare |
| PRODUCT_SEARCH | Product search performed | Common |
| BARCODE_LOOKUP | Barcode scanned | Common |
| ADVERTISEMENT_CREATED | Advertisement created | Rare |
| ADVERTISEMENT_UPDATED | Advertisement updated | Rare |
| ADVERTISEMENT_DELETED | Advertisement deleted | Rare |
| ADVERTISEMENT_PREVIEWED | Advertisement previewed | Rare |

---

## 7. SAMPLE LOG OUTPUT

### Console Output Sample

The system logs to console with `[JOURNAL]` and `[AUDIT]` prefixes:

```
[JOURNAL] Server started on port 8765
[JOURNAL] New connection from: 192.168.1.100
[JOURNAL] Received first message: {"type":"HANDSHAKE_REQUEST","timestamp":1711832600000,"ipAddress":"192.168.1.100","machineName":"POS-Terminal-01"}
[JOURNAL] Sending handshake response: {"type":"HANDSHAKE_RESPONSE","timestamp":1711832600100,"ipAddress":"192.168.1.50","machineName":"POS-Server","status":"OK"}
[JOURNAL] JSON handshake complete: 192.168.1.100 (POS-Terminal-01)
[AUDIT] [2026-03-30T14:30:29.297165] John Smith - ITEM_ADDED - Cashier: John Smith (101) - Added: Coca Cola 12oz - Amount: $1.99 - TXN: TXN-2026-03-30-001
[AUDIT] [2026-03-30T14:30:50.123456] John Smith - PAYMENT_COMPLETED - Cashier: John Smith (101) - Payment completed: Cash - Amount: $25.47 - TXN: TXN-2026-03-30-001
[JOURNAL] Connection closed: 192.168.1.100 (POS-Terminal-01)
```

---

## 8. TIMESTAMP FORMAT DETAILS

**Critical for parsing!**

### Two Timestamp Fields in AuditEventMessage:

**Field 1: `timestamp` (on base JournalMessage)**
- **Data Type:** Long (primitive)
- **Format:** Milliseconds since Unix epoch
- **Examples:**
  - `1711832629297` = March 30, 2026 14:30:29.297 UTC
  - `1711832600000` = March 30, 2026 14:30:00.000 UTC

**Field 2: `eventTimestamp` (on AuditEventMessage)**
- **Data Type:** String
- **Format:** ISO-8601 LocalDateTime format (yyyy-MM-dd'T'HH:mm:ss.SSS)
- **Examples:**
  - `"2026-03-30T14:30:29.297"` = March 30, 2026 at 2:30:29.297 PM
  - `"2026-03-30T14:30:00.000"` = March 30, 2026 at 2:30:00.000 PM

### Flexible Parsing (Incoming Messages)

The system can accept timestamps in multiple formats for compatibility:

1. **Milliseconds (long):** `1711832629297`
2. **Seconds with decimal (double):** `1711832629.297`
   - Automatically converted to milliseconds: value × 1000
   - Detection threshold: if value < 10,000,000,000, treat as seconds

---

## 9. COMPATIBILITY NOTES

### Known Issues

- **Legacy POS Support:** System includes a `LegacyFormatParser` to support pipe-delimited formats from older POS systems
- **Unknown Event Types:** If a client sends an unknown `eventType` string, it defaults to `CART_CLEARED` to prevent crash
- **Unknown Protocol Types:** Unknown `type` values are treated as `ERROR` protocol

### Dependencies

- **Gson:** 2.10 or later (com.google.gson)
- **Java Version:** Java 25 (but compatible with Java 17+)
- **No External Libraries:** Pure TCP sockets using java.net.Socket

### Backward Compatibility

The system handles:
- Legacy pipe-delimited format (auto-detected by `LegacyFormatParser.isLegacyFormat()`)
- Mixed timestamp formats (seconds vs milliseconds)
- Missing optional fields (transactionId, amount, status)

---

## 10. TESTING INFORMATION

### How to Test Connection

```bash
# 1. Start the Journal Server (from POS application or standalone)
# Server listens on 0.0.0.0:8765 by default

# 2. Test basic connectivity
telnet localhost 8765

# 3. Send a handshake request (after connecting)
# Type this JSON and press Enter:
{"type":"HANDSHAKE_REQUEST","timestamp":1711832600000,"ipAddress":"127.0.0.1","machineName":"TestClient"}

# 4. You should receive a handshake response:
{"type":"HANDSHAKE_RESPONSE","timestamp":1711832600100,"ipAddress":"192.168.1.50","machineName":"POS-Server","status":"OK"}

# 5. You will then receive AUDIT_EVENT messages in real-time as transactions occur
```

### Sample Client Code

```java
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal client to connect and receive messages from Swings POS Virtual Journal.
 */
public class SimpleJournalClient {

    public static void main(String[] args) throws IOException {
        // Connect to server
        Socket socket = new Socket("localhost", 8765);

        // Create streams
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
        PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
            true // auto-flush
        );

        // Send handshake
        String handshake = "{\"type\":\"HANDSHAKE_REQUEST\"," +
                          "\"timestamp\":" + System.currentTimeMillis() + "," +
                          "\"ipAddress\":\"127.0.0.1\"," +
                          "\"machineName\":\"TestClient\"}";
        writer.println(handshake);

        // Read response
        String response = reader.readLine();
        System.out.println("Handshake response: " + response);

        // Read and print all incoming messages
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Received: " + line);

            // Parse as JSON if needed
            // Gson gson = new Gson();
            // Map<String, Object> message = gson.fromJson(line, Map.class);
            // String type = (String) message.get("type");
            // ... process message ...
        }

        socket.close();
    }
}
```

### Python Client Example

```python
import socket
import json
import time

# Connect to server
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('localhost', 8765))

# Send handshake
handshake = {
    "type": "HANDSHAKE_REQUEST",
    "timestamp": int(time.time() * 1000),
    "ipAddress": "127.0.0.1",
    "machineName": "PythonClient"
}
sock.sendall((json.dumps(handshake) + '\n').encode('utf-8'))

# Receive messages
file = sock.makefile('r', encoding='utf-8')
for line in file:
    message = json.loads(line)
    print(f"Received {message['type']}: {line}")

    # Process specific message types
    if message['type'] == 'AUDIT_EVENT':
        print(f"  Event: {message['eventType']}")
        print(f"  Cashier: {message['cashierName']}")
        print(f"  Details: {message['details']}")
```

---

## APPENDIX: File Locations

All relevant files analyzed:

- `src/main/java/com/swings/pos/domain/journal/network/JournalServer.java` - Server implementation
- `src/main/java/com/swings/pos/domain/journal/network/JournalClient.java` - Client implementation
- `src/main/java/com/swings/pos/domain/journal/network/ClientConnection.java` - Per-client connection handler
- `src/main/java/com/swings/pos/domain/journal/model/JournalMessage.java` - Base message class
- `src/main/java/com/swings/pos/domain/journal/model/AuditEventMessage.java` - Audit event message structure
- `src/main/java/com/swings/pos/domain/journal/model/HandshakeMessage.java` - Handshake message structure
- `src/main/java/com/swings/pos/domain/journal/model/JournalProtocol.java` - Protocol type enum
- `src/main/java/com/swings/pos/domain/journal/serialization/JournalGsonFactory.java` - Gson configuration
- `src/main/java/com/swings/pos/domain/audit/model/AuditEvent.java` - Core audit event model
- `src/main/java/com/swings/pos/domain/audit/model/AuditEventType.java` - Event type enum
- `src/main/java/com/swings/pos/domain/audit/AuditLogger.java` - Audit logging service
- `src/main/java/com/swings/pos/config/JournalConfig.java` - Configuration constants
- `src/main/java/com/swings/pos/commons/constants/JournalConstants.java` - System constants

---

**End of Report**
**Ready for Integration Analysis**

---

## QUICK INTEGRATION CHECKLIST

For integrating with this POS system:

✅ **Connection:** TCP socket to port 8765 (or configured port)
✅ **Encoding:** UTF-8 with `\n` line terminators
✅ **Handshake:** Send HANDSHAKE_REQUEST, expect HANDSHAKE_RESPONSE with status "OK"
✅ **Message Format:** One JSON object per line
✅ **Primary Message Type:** AUDIT_EVENT with eventType field indicating specific event
✅ **Timestamps:** Parse `timestamp` as milliseconds or `eventTimestamp` as ISO-8601 string
✅ **Keep-alive:** Respond to PING with PONG
✅ **Graceful Shutdown:** Send DISCONNECT message before closing socket

**Integration is straightforward - standard JSON-over-TCP with well-defined message structure!**
