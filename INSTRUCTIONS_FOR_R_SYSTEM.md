# Instructions for R System (Journal Server - Port 9999)

**Issue:** Your POS system accepts connections but doesn't broadcast transaction events to connected clients.

**Root Cause:** Your `JournalServer` receives and logs messages FROM clients, but your own POS transactions aren't being broadcast TO clients.

---

## 🔧 Required Changes

### Problem: Missing Integration

Your codebase has:
- ✅ `JournalServer.java` - Accepts connections
- ✅ `ClientHandler.java` - Handles client messages
- ✅ `JournalLogger.java` - Logs to files/console
- ❌ **NO integration between YOUR POS transactions and the broadcast system**

When YOUR cashier adds an item to the cart, that event:
- ✅ Gets logged to `logs/journal.log`
- ✅ Shows in your GUI console
- ❌ **Does NOT get sent to connected clients**

---

## ✅ Solution: Wire POS Events to Broadcast

### Step 1: Add Broadcasting to JournalLogger

**File:** `src/main/java/com/company/pos/journal/logger/JournalLogger.java`

**Find the `log()` method around line 115-139.**

**Current code:**
```java
public void log(JournalMessage message, String ipAddress) {
    // ... writes to file
    // ... writes to console
    // ... notifies listeners (GUI only)
}
```

**Add this:** Broadcast to connected clients via JournalServer

**Update the `log()` method:**

```java
public void log(JournalMessage message, String ipAddress) {
    lock.lock();
    try {
        // Existing file logging
        String formatted = formatMessage(message, ipAddress);
        writer.write(formatted);
        writer.write(System.lineSeparator());
        writer.flush();

        // Existing console logging
        if (!message.heartbeat()) {
            System.out.println(formatted);
        }

        // Existing listener notification (GUI)
        for (LogListener listener : listeners) {
            listener.onLogEntry(formatted);
        }

        // ✨ NEW: Broadcast to connected clients
        if (journalServer != null && !message.heartbeat()) {
            journalServer.broadcastToClients(message);
        }

    } finally {
        lock.unlock();
    }
}
```

---

### Step 2: Add JournalServer Reference to JournalLogger

**File:** `src/main/java/com/company/pos/journal/logger/JournalLogger.java`

**Add field:**
```java
private JournalServer journalServer;
```

**Add setter method:**
```java
/**
 * Sets the journal server for broadcasting messages to clients.
 *
 * @param journalServer The journal server instance
 */
public void setJournalServer(JournalServer journalServer) {
    this.journalServer = journalServer;
}
```

---

### Step 3: Add Broadcasting Method to JournalServer

**File:** `src/main/java/com/company/pos/journal/server/JournalServer.java`

**Add this method:**

```java
/**
 * Broadcasts a journal message to all connected clients.
 * Called when local POS transactions occur.
 *
 * @param message The journal message to broadcast
 */
public void broadcastToClients(JournalMessage message) {
    if (connectedClients.isEmpty()) {
        return; // No clients connected
    }

    try {
        // Serialize message to JSON
        String json = new Gson().toJson(message);

        // Send to all connected clients
        synchronized (connectedClients) {
            for (ClientHandler client : connectedClients) {
                client.sendMessage(json);
            }
        }

        logger.debug("Broadcasted message to {} client(s): {}",
                     connectedClients.size(), message.type);

    } catch (Exception e) {
        logger.error("Failed to broadcast message", e);
    }
}
```

---

### Step 4: Add Send Method to ClientHandler

**File:** `src/main/java/com/company/pos/journal/server/ClientHandler.java`

**Add field for output:**
```java
private PrintWriter out;
```

**Initialize in constructor/run():**
```java
@Override
public void run() {
    try {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
        );

        // ✨ ADD THIS:
        out = new PrintWriter(
            new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8),
            true // auto-flush
        );

        // ... rest of existing code
    }
}
```

**Add send method:**
```java
/**
 * Sends a message to this client.
 *
 * @param json JSON message to send
 */
public void sendMessage(String json) {
    if (out != null && !clientSocket.isClosed()) {
        try {
            out.println(json); // Newline-delimited

            if (out.checkError()) {
                logger.warn("Error writing to client: {}", ipAddress);
                closeConnection();
            }
        } catch (Exception e) {
            logger.error("Failed to send message to client: {}", ipAddress, e);
            closeConnection();
        }
    }
}
```

---

### Step 5: Wire Up in Main Application

**File:** `src/main/java/com/company/pos/journal/JournalServerApplication.java`

**Update bean configuration:**

```java
@Bean
public JournalLogger journalLogger(JournalLogProperties props) {
    return new JournalLogger(props);
}

@Bean
public JournalServer journalServer(JournalServerProperties props,
                                   JournalLogger logger) {
    JournalServer server = new JournalServer(props.getPort(), logger, props);

    // ✨ ADD THIS: Wire logger to server for broadcasting
    logger.setJournalServer(server);

    return server;
}
```

---

## 🧪 Testing After Changes

### Step 1: Rebuild
```bash
./gradlew clean build
```

### Step 2: Run Your Server
```bash
./gradlew bootRun
```

### Step 3: Verify Startup
Console should show:
```
[JOURNAL] Server started on port 9999
```

### Step 4: Have Client Connect
The remote POS system (requesting your logs) should connect to port 9999.

Console should show:
```
[JOURNAL] New connection from: 192.168.x.x
```

### Step 5: Add Item to Cart
Add an item in YOUR POS system.

Console should show:
```
[JOURNAL] Broadcasted message to 1 client(s): PRODUCT_ADDED
```

### Step 6: Verify on Remote System
The remote system should now see the transaction in their Live Journal Viewer.

---

## 📋 Alternative: Quick Test Without Code Changes

If you want to test the concept first, you can manually broadcast from console:

**Create test file:** `TestBroadcast.java`

```java
import com.company.pos.journal.server.JournalServer;
import com.company.pos.journal.protocol.JournalMessage;
import com.company.pos.journal.protocol.MessageType;

public class TestBroadcast {
    public static void main(String[] args) {
        // Get reference to your running server
        // (You'll need to expose it or use Spring context)

        JournalMessage testMsg = new JournalMessage(
            MessageType.PRODUCT_ADDED.name(),
            System.currentTimeMillis() / 1000.0,
            "TEST_TERMINAL",
            "TEST_TXN_123",
            Map.of("name", "Test Product", "price", "9.99")
        );

        // journalServer.broadcastToClients(testMsg);
        System.out.println("Test broadcast sent");
    }
}
```

---

## 📊 Summary

**What's Wrong:**
Your server accepts connections but doesn't send your transaction events to clients.

**What You Need to Do:**
1. Add `broadcastToClients()` method to `JournalServer`
2. Add `sendMessage()` method to `ClientHandler`
3. Wire `JournalLogger` to call `broadcastToClients()` when logging events
4. Add `setJournalServer()` to `JournalLogger`
5. Connect them in `JournalServerApplication` bean configuration

**Time Estimate:** 30-45 minutes

**Complexity:** Medium - straightforward integration

---

## 🆘 Need Help?

If you have questions or run into issues:

1. Share console error messages
2. Share which step you're stuck on
3. Share relevant code snippets

We can provide more detailed guidance or alternative approaches!

---

**After making these changes, your transactions will broadcast to all connected clients in real-time!** 🚀
