x# POS Message Format Response

**Generated:** 2026-03-30
**POS System Name:** POS Client (pos-client)
**Analyzed By:** Claude Code

---

## 1. SYSTEM IDENTIFICATION

- **POS System Name:** POS Client - Java Swing Point of Sale
- **Version:** 1.0.0
- **Broadcast IP:Port:** Configurable, currently `localhost:9999` and `192.168.8.186:9000`
- **Network Protocol:** TCP (persistent socket connection)
- **Message Format:** JSON (one JSON object per line)

---

## 2. MESSAGE STRUCTURE

### Top-Level Fields

| Field Name | Data Type | Required | Description | Example Value |
|------------|-----------|----------|-------------|---------------|
| `type` | String | Yes | Event type identifier | `"PRODUCT_ADDED"` |
| `timestamp` | String (ISO-8601 Instant) | Yes | Event timestamp in UTC | `"2026-03-30T14:17:03.123456Z"` |
| `terminalId` | String | Yes | Unique terminal identifier | `"TERMINAL_MAC"` |
| `transactionId` | String | Yes (null for HEARTBEAT) | Current transaction ID | `"TXN-1743382623-1001"` |
| `payload` | Object | Yes | Event-specific data (varies by type) | `{ "upc": "...", ... }` |

### Message Record Definition

**File:** `src/main/java/com/company/pos/client/journal/JournalClient.java` (line 447)

```java
private record JournalMessage(
    String type,
    Instant timestamp,
    String terminalId,
    String transactionId,
    Map<String, Object> payload
) {}
```

---

## 3. ACTUAL MESSAGE EXAMPLES

### Example 1: Transaction Started

```json
{
  "type": "TRANSACTION_STARTED",
  "timestamp": "2026-03-30T14:17:00.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "cashierName": "Demo Cashier"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `ensureTransactionStarted()` (line 168)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `cashierName` | String | Name of the cashier | `"Demo Cashier"` |

---

### Example 2: Item Added to Cart

```json
{
  "type": "PRODUCT_ADDED",
  "timestamp": "2026-03-30T14:17:03.123456Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "upc": "028400325059",
    "name": "CS HOT FRIE",
    "price": "4.99",
    "quantity": 1
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onProductAdded()` (line 36)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `upc` | String | Product UPC/barcode | `"028400325059"` |
| `name` | String | Product name | `"CS HOT FRIE"` |
| `price` | String (decimal) | Unit price | `"4.99"` |
| `quantity` | Integer | Quantity in basket for this line item | `1` |

---

### Example 3: Quantity Changed

```json
{
  "type": "QUANTITY_CHANGED",
  "timestamp": "2026-03-30T14:17:04.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "lineItemIndex": 0,
    "newQuantity": 3,
    "upc": "028400325059",
    "name": "CS HOT FRIE"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onQuantityChanged()` (line 118)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `lineItemIndex` | Integer | Index of item in basket | `0` |
| `newQuantity` | Integer | Updated quantity | `3` |
| `upc` | String | Product UPC | `"028400325059"` |
| `name` | String | Product name | `"CS HOT FRIE"` |

---

### Example 4: Line Item Voided

```json
{
  "type": "LINE_ITEM_VOIDED",
  "timestamp": "2026-03-30T14:17:05.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "lineItemIndex": 2,
    "upc": "028400325059",
    "name": "CS HOT FRIE"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onLineItemVoided()` (line 50)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `lineItemIndex` | Integer | Index of voided item | `2` |
| `upc` | String | Product UPC | `"028400325059"` |
| `name` | String | Product name | `"CS HOT FRIE"` |

---

### Example 5: Basket Voided

```json
{
  "type": "BASKET_VOIDED",
  "timestamp": "2026-03-30T14:17:06.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "reason": "Basket voided by cashier"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onBasketVoided()` (line 63)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `reason` | String | Reason for voiding | `"Basket voided by cashier"` |

---

### Example 6: Transaction Finalized (Total Pressed)

```json
{
  "type": "TRANSACTION_FINALIZED",
  "timestamp": "2026-03-30T14:18:00.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "subtotal": "24.95",
    "tax": "2.19",
    "total": "27.14",
    "itemCount": 5
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onTransactionFinalized()` (line 77)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `subtotal` | String (decimal) | Pre-tax subtotal | `"24.95"` |
| `tax` | String (decimal) | Tax amount | `"2.19"` |
| `total` | String (decimal) | Total with tax | `"27.14"` |
| `itemCount` | Integer | Number of line items | `5` |

---

### Example 7: Transaction Cancelled

```json
{
  "type": "TRANSACTION_CANCELLED",
  "timestamp": "2026-03-30T14:18:01.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "reason": "Tendering cancelled by cashier"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onTransactionCancelled()` (line 91)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `reason` | String | Reason for cancellation | `"Tendering cancelled by cashier"` |

---

### Example 8: Payment Processed

```json
{
  "type": "PAYMENT_PROCESSED",
  "timestamp": "2026-03-30T14:18:10.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "paymentType": "CASH",
    "amount": "30.00",
    "change": "2.86"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onPaymentProcessed()` (line 102)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `paymentType` | String (enum name) | Payment method | `"CASH"`, `"DEBIT_CARD"`, `"CREDIT_CARD"`, `"E_WALLET"` |
| `amount` | String (decimal) | Amount tendered | `"30.00"` |
| `change` | String (decimal) | Change given back | `"2.86"` |

---

### Example 9: Discount Applied

```json
{
  "type": "DISCOUNT_APPLIED",
  "timestamp": "2026-03-30T14:17:03.500000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": "TXN-1743382620-1001",
  "payload": {
    "discountDescription": "10% bulk discount on snacks",
    "discountAmount": "2.49",
    "originalSubtotal": "24.95",
    "totalDiscountCount": 2,
    "appliedRuleNames": "10% bulk discount on snacks, $5 off orders over $50",
    "itemLevelDiscount": "2.49",
    "basketLevelDiscount": "5.00",
    "discountedSubtotal": "17.46",
    "discountBreakdown": {
      "itemLevel": "2.49",
      "basketLevel": "5.00",
      "total": "7.49"
    }
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Method:** `onDiscountApplied()` (line 135)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `discountDescription` | String | Human-readable discount description | `"10% bulk discount on snacks"` |
| `discountAmount` | String (decimal) | Discount delta amount | `"2.49"` |
| `originalSubtotal` | String (decimal) | Subtotal before discounts | `"24.95"` |
| `totalDiscountCount` | Integer | Number of applied discount rules | `2` |
| `appliedRuleNames` | String | Comma-separated rule names | `"10% bulk discount on snacks, $5 off orders over $50"` |
| `itemLevelDiscount` | String (decimal) | Item-level discount total | `"2.49"` |
| `basketLevelDiscount` | String (decimal) | Basket-level discount total | `"5.00"` |
| `discountedSubtotal` | String (decimal) | Subtotal after all discounts | `"17.46"` |
| `discountBreakdown` | Object | Nested breakdown object | See below |
| `discountBreakdown.itemLevel` | String (decimal) | Item-level portion | `"2.49"` |
| `discountBreakdown.basketLevel` | String (decimal) | Basket-level portion | `"5.00"` |
| `discountBreakdown.total` | String (decimal) | Total discount amount | `"7.49"` |

---

### Example 10: Heartbeat

```json
{
  "type": "HEARTBEAT",
  "timestamp": "2026-03-30T14:18:37.000000Z",
  "terminalId": "TERMINAL_MAC",
  "transactionId": null,
  "payload": {
    "status": "ACTIVE"
  }
}
```

**File:** `src/main/java/com/company/pos/client/journal/JournalClient.java`
**Method:** `sendHeartbeat()` (line 214)

**Payload Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `status` | String | Terminal status | `"ACTIVE"` |

---

## 4. CODE SNIPPETS

### Message Creation Code

**File:** `src/main/java/com/company/pos/client/journal/JournalClient.java`
**Lines:** 194-209

```java
public void sendMessage(String type, String transactionId, Map<String, Object> payload) {
    JournalMessage message = new JournalMessage(
        type,
        Instant.now(),
        terminalId,
        transactionId,
        payload
    );

    try {
        messageQueue.offer(message, 1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        logger.warn("Failed to queue message: {}", type);
        Thread.currentThread().interrupt();
    }
}
```

---

### Message Broadcasting Code

**File:** `src/main/java/com/company/pos/client/journal/JournalClient.java`
**Lines:** 262-319

```java
private boolean sendMessageSync(JournalMessage message) {
    boolean sentToAny = false;

    try {
        String json = objectMapper.writeValueAsString(message);

        // Send to all connected servers
        for (ServerConnection conn : connections.values()) {
            if (!conn.connected || conn.out == null || conn.socket == null) {
                continue;
            }

            try {
                if (conn.socket.isClosed() || !conn.socket.isConnected()) {
                    conn.connected = false;
                    continue;
                }

                conn.out.println(json);
                conn.out.flush();

                if (conn.out.checkError()) {
                    conn.connected = false;
                    try {
                        conn.socket.close();
                    } catch (IOException closeEx) {
                        // logged at debug
                    }
                } else {
                    sentToAny = true;
                }
            } catch (Exception e) {
                conn.connected = false;
            }
        }

        return sentToAny;

    } catch (Exception e) {
        logger.error("Failed to serialize message: {}", e.getMessage());
        return false;
    }
}
```

---

### Event Handler Code (converts POS events to journal messages)

**File:** `src/main/java/com/company/pos/client/journal/JournalEventHandler.java`
**Lines:** 36-47 (example: PRODUCT_ADDED)

```java
@Override
public void onProductAdded(ProductAddedEvent event) {
    ensureTransactionStarted();

    Map<String, Object> payload = new HashMap<>();
    payload.put("upc", event.getProduct().upc());
    payload.put("name", event.getProduct().name());
    payload.put("price", event.getProduct().price().toString());
    payload.put("quantity", event.getLineItem().getQuantity());

    journalClient.sendMessage("PRODUCT_ADDED", currentTransactionId, payload);
}
```

---

## 5. PROTOCOL DETAILS

### Serialization

- **Library Used:** Jackson (`com.fasterxml.jackson.databind.ObjectMapper`) with `JavaTimeModule`
- **Configuration:** `new ObjectMapper().registerModule(new JavaTimeModule())`
- **Date/Time Format:** ISO-8601 Instant (e.g., `"2026-03-30T14:17:03.123456Z"`) — UTC, nanosecond precision

### Network Transmission

- **Line Termination:** `\n` (via `PrintWriter.println()`)
- **Character Encoding:** UTF-8 (Java default for `PrintWriter` over socket `OutputStream`)
- **Message Delimiter:** Newline — one complete JSON object per line (JSONL / newline-delimited JSON)
- **Maximum Message Size:** No explicit limit; queue capacity is 1000 messages

### Special Formatting

- **Prefixes:** None — raw JSON on each line
- **Wrappers:** None
- **Escaping:** Standard Jackson JSON escaping
- **Compression:** None

### Connection Details

- **Socket Type:** `java.net.Socket` (TCP, blocking)
- **Auto-flush:** `new PrintWriter(socket.getOutputStream(), true)` — auto-flush enabled
- **Heartbeat Interval:** Every 30 seconds
- **Reconnect Interval:** 5000ms (configurable via `journal.reconnect-interval-ms`)
- **Message Queue Capacity:** 1000 messages (overflow drops new messages)
- **Multi-Server:** Broadcasts to ALL connected servers simultaneously

---

## 6. EVENT TYPES

| Event Type String | Description | Frequency |
|-------------------|-------------|-----------|
| `TRANSACTION_STARTED` | Fired once when first item is added to a new basket | Common (once per transaction) |
| `PRODUCT_ADDED` | Product added to basket (via Quick Add, search, or barcode scan) | Very Common |
| `QUANTITY_CHANGED` | Line item quantity updated | Common |
| `LINE_ITEM_VOIDED` | Single line item removed from basket | Occasional |
| `BASKET_VOIDED` | Entire basket cleared | Occasional |
| `TRANSACTION_FINALIZED` | Total button pressed, entering tendering phase | Common (once per transaction) |
| `TRANSACTION_CANCELLED` | Tendering cancelled, returns to basket editing | Occasional |
| `PAYMENT_PROCESSED` | Final payment completed, transaction done | Common (once per transaction) |
| `DISCOUNT_APPLIED` | Discount engine applied/updated discounts | Common (when discount engine enabled) |
| `HEARTBEAT` | Periodic health check (every 30 seconds) | Continuous |

### Typical Transaction Event Sequence

```
TRANSACTION_STARTED → PRODUCT_ADDED → PRODUCT_ADDED → ... → DISCOUNT_APPLIED → TRANSACTION_FINALIZED → PAYMENT_PROCESSED
```

---

## 7. SAMPLE LOG OUTPUT

### From: logs/pos-client.log

```
2026-03-30 22:17:03 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Sending message type=PRODUCT_ADDED to localhost:9999
2026-03-30 22:17:03 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Successfully sent to localhost:9999
2026-03-30 22:17:03 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Sending message type=PRODUCT_ADDED to 192.168.8.186:9000
2026-03-30 22:17:03 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Successfully sent to 192.168.8.186:9000
2026-03-30 22:17:03 [AWT-EventQueue-0] DEBUG c.c.p.c.journal.JournalEventHandler - Logged DISCOUNT_APPLIED event: $5 off orders over $50 (Item: 0, Basket: 5.00)
2026-03-30 22:17:06 [AWT-EventQueue-0] DEBUG c.c.p.c.journal.JournalEventHandler - Logged DISCOUNT_APPLIED event: 10% bulk discount on snacks (Item: 2.49, Basket: 5.00)
2026-03-30 22:18:37 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Sending message type=HEARTBEAT to localhost:9999
2026-03-30 22:18:37 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Successfully sent to localhost:9999
2026-03-30 22:18:37 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Sending message type=HEARTBEAT to 192.168.8.186:9000
2026-03-30 22:18:37 [pool-2-thread-1] DEBUG c.c.pos.client.journal.JournalClient - Successfully sent to 192.168.8.186:9000
```

---

## 8. TIMESTAMP FORMAT DETAILS

- **Field Name:** `timestamp`
- **Data Type:** `java.time.Instant` (serialized by Jackson's `JavaTimeModule`)
- **Format:** ISO-8601 instant in UTC with nanosecond precision
- **Examples:**
    - `"2026-03-30T14:17:03.123456Z"` — March 30, 2026 at 14:17:03.123456 UTC
    - `"2026-03-30T22:18:37.000000Z"` — March 30, 2026 at 22:18:37 UTC
- **Notes:** The `Z` suffix indicates UTC. Jackson's `JavaTimeModule` serializes `Instant` as a decimal number by default (e.g., `1743382623.123456`) unless configured with `WRITE_DATES_AS_TIMESTAMPS = false`. This system uses default Jackson settings, so **timestamps are serialized as decimal epoch seconds** (e.g., `1743382623.123456`).

**Actual wire format is decimal epoch seconds:**
```json
"timestamp": 1743382623.123456
```

Not ISO-8601 string. The `JavaTimeModule` default serializes `Instant` as a `double` (seconds since Unix epoch with fractional nanoseconds).

---

## 9. COMPATIBILITY NOTES

### Known Behaviors

- **TRANSACTION_STARTED is implicit:** Not triggered by a user action. It fires automatically on the first event of a new transaction (via `ensureTransactionStarted()`). If no items are added, no TRANSACTION_STARTED is sent.
- **transactionId is null for HEARTBEAT:** The `transactionId` field is `null` in heartbeat messages.
- **Monetary values are strings:** All prices, amounts, and totals are serialized as `String` (e.g., `"4.99"` not `4.99`) to preserve decimal precision. The exception is `quantity` and `itemCount` which are integers.
- **DISCOUNT_APPLIED fires on every basket change:** When the discount engine is connected, this event fires after each product add/remove, not just once.
- **Messages re-queue on failure:** If no server is connected, non-heartbeat messages are re-queued and retried when connection is restored.
- **Multi-server broadcast:** Every message goes to ALL connected servers. There is no server-specific routing.

### Dependencies

- **Jackson Databind:** `com.fasterxml.jackson.databind.ObjectMapper` for JSON serialization
- **Jackson JavaTimeModule:** `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule` for `Instant` support
- **Java 21+** runtime

---

## 10. TESTING INFORMATION

### How to Test Connection

```bash
# Listen for messages on port 9999 (acts as a simple journal server)
nc -l 9999

# Or use ncat for a keep-alive listener
ncat -lk 9999
```

Then in the POS application:
1. Go to Settings (navigation bar)
2. Enter server address: `localhost:9999`
3. Click "Test Connection" to verify TCP connectivity
4. Click "Connect" to start receiving messages
5. Add products to basket — you'll see JSON messages appear in the nc/ncat terminal

### Sample Client Code

```java
import java.io.*;
import java.net.*;

public class JournalListener {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(9999);
        System.out.println("Listening on port 9999...");

        while (true) {
            Socket client = server.accept();
            System.out.println("POS connected: " + client.getRemoteSocketAddress());

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received: " + line);
                    }
                } catch (IOException e) {
                    System.out.println("Client disconnected");
                }
            }).start();
        }
    }
}
```

---

## APPENDIX: File Locations

| File | Purpose |
|------|---------|
| `src/main/java/com/company/pos/client/journal/JournalClient.java` | Socket client, message queuing, broadcasting, reconnection |
| `src/main/java/com/company/pos/client/journal/JournalEventHandler.java` | Converts POS events to journal messages (payload construction) |
| `src/main/java/com/company/pos/client/config/JournalConfig.java` | Configuration loading from properties file |
| `src/main/java/com/company/pos/client/ui/panels/JournalSettingsPanel.java` | UI for configuring and connecting to journal servers |
| `config/journal-client.properties` | Runtime configuration (servers, terminal ID, reconnect interval) |
| `src/main/resources/application.properties` | Default/fallback configuration |
| `src/main/java/com/company/pos/client/ui/events/PosEventListener.java` | Event listener interface (defines all event hooks) |

---

**End of Report**
**Ready for Integration Analysis**