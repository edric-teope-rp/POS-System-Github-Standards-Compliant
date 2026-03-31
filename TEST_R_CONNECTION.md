# Quick Test: Is R Sending Messages?

## Step 1: Check Console Debug Output

1. Start your POS system
2. Open Socket Configuration
3. Connect to R's system
4. Open Live Journal Viewer
5. **Keep console visible**

## Step 2: Have R Add an Item

Ask R to add an item to their cart.

## Step 3: Check Console - Which Scenario?

### ✅ Scenario A: Connection Successful, Messages Arriving
```
DEBUG: Received journal entry from R (192.168.x.x): {"type":"...","timestamp":...}
DEBUG: Parsing JSON from R - Keys: [type, timestamp, ...]
DEBUG: Found timestamp (long): ...
DEBUG: Found type: ...
DEBUG: Parsed JSON successfully - Action: ITEM_ADD, Details: ...
```
**Status:** Parser working! Issue is in display layer.
**Action:** We can fix this on your end.

---

### ⚠️ Scenario B: Connection Successful, Parsing Fails
```
DEBUG: Received journal entry from R (192.168.x.x): {"weird":"format","unknown":"fields"}
DEBUG: Parsing JSON from R - Keys: [weird, unknown]
ERROR: Failed to parse JSON format: ...
ERROR: JSON line was: {"weird":"format",...}
```
**Status:** Parser can't understand R's format.
**Action:** Copy the JSON and share with me. We'll update parser.

---

### ❌ Scenario C: No Messages Received
```
(nothing appears when R adds item)
```
**Status:** R's server not broadcasting to clients.
**Action:** R needs to configure their system to broadcast.

---

### 🔌 Scenario D: Connection Failed
```
Connection refused / timeout errors
```
**Status:** Network issue or R's server not running.
**Action:** Check R's IP, port, firewall, server status.

---

## Step 4: Share Results

Copy and paste what you see in the console here, then we'll know exactly what to fix!
