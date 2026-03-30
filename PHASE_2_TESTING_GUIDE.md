# Phase 2 Testing Guide - Multi-POS Journal Viewer

## Overview
This guide shows you how to test Phase 2 by running two POS instances locally that communicate via socket connections.

---

## Quick Start

### Step 1: Build the JAR (One Time)
```bash
./gradlew jar
```

This creates: `build/libs/possystem-1.0-SNAPSHOT-all.jar` (~29MB with all dependencies)

---

### Step 2: Run Two Instances

#### Instance 1 - From IntelliJ IDE

1. **Edit Run Configuration** in IntelliJ:
   - Go to: Run → Edit Configurations
   - Select your `Main` configuration
   - In **Program arguments**, add:
     ```
     --db=possystemdb_pos1 --name=POS-1 --port=8080
     ```
   - Click **OK**

2. **Run** the application from IntelliJ

**Expected Console Output:**
```
=== POS Configuration ===
Database: possystemdb_pos1
POS Name: POS-1
Server Port: 8080
=========================
H2 Console available at: http://localhost:8082
Database connected successfully!
...
DEBUG: SocketService initialized - POS Name: POS-1, Server Port: 8080
```

---

#### Instance 2 - From Terminal (JAR)

Open a new terminal and run:
```bash
java --enable-preview -jar build/libs/possystem-1.0-SNAPSHOT-all.jar --db=possystemdb_pos2 --name=POS-2 --port=8081
```

**Expected Console Output:**
```
=== POS Configuration ===
Database: possystemdb_pos2
POS Name: POS-2
Server Port: 8081
=========================
H2 Console available at: http://localhost:8083
Database connected successfully!
...
DEBUG: SocketService initialized - POS Name: POS-2, Server Port: 8081
```

---

## Step 3: Test Socket Communication

### Auto-Discovery Test

1. On **POS-1**:
   - Click **Settings** → **Socket Configuration**
   - Login (admin/admin)
   - Click **Start Server** (should already be on port 8080)
   - Click **Start Discovery**

2. On **POS-2**:
   - Click **Settings** → **Socket Configuration**
   - Login (admin/admin)
   - Click **Start Server** (should already be on port 8081)
   - Click **Start Discovery**

3. **Wait 5-10 seconds** for UDP discovery to work

4. In **"Available POS Systems"** table:
   - POS-1 should see **POS-2** at `localhost:8081`
   - POS-2 should see **POS-1** at `localhost:8080`

---

### Manual Connection Test

If auto-discovery doesn't work, try manual connection:

1. On **POS-1**:
   - In Socket Configuration, enter:
     - POS Name: `POS-2`
     - IP Address: `localhost`
     - Port: `8081`
   - Click **Connect**

2. On **POS-2**:
   - In Socket Configuration, enter:
     - POS Name: `POS-1`
     - IP Address: `localhost`
     - Port: `8080`
   - Click **Connect**

---

### Journal Synchronization Test

1. On **POS-1**:
   - Add some items to cart
   - Click **Total** → **Pay Cash** → Complete transaction
   - **Check console**: You should see GREEN colored journal entries (local)

2. On **POS-2**:
   - Open **Socket Configuration** → **Live Journal Viewer** tab
   - **You should see POS-1's transaction** in BLUE/YELLOW color
   - Remote journal saved to: `logs/remote-journals/POS-1_[ip]/journal.log`

3. On **POS-2**:
   - Make a transaction
   - **Check console**: You should see GREEN colored journal entries (local)

4. On **POS-1**:
   - Open **Socket Configuration** → **Live Journal Viewer** tab
   - **You should see POS-2's transaction** in BLUE/YELLOW color

---

## Command-Line Arguments Reference

| Argument | Description | Default |
|----------|-------------|---------|
| `--db=NAME` | Database name (stored in `~/NAME.mv.db`) | `possystemdb` |
| `--name=NAME` | POS system name for identification | hostname |
| `--port=PORT` | TCP server port for journal broadcasting | `8080` |

### Examples

**Default instance:**
```bash
./gradlew run
# Uses: possystemdb, hostname, port 8080
```

**Custom instance:**
```bash
java --enable-preview -jar build/libs/possystem-1.0-SNAPSHOT-all.jar --db=store2 --name=Store-2 --port=9000
```

**Multiple instances:**
```bash
# Instance 1 (Terminal 1)
java --enable-preview -jar build/libs/possystem-1.0-SNAPSHOT-all.jar --db=pos1 --name=POS-1 --port=8080

# Instance 2 (Terminal 2)
java --enable-preview -jar build/libs/possystem-1.0-SNAPSHOT-all.jar --db=pos2 --name=POS-2 --port=8081

# Instance 3 (Terminal 3)
java --enable-preview -jar build/libs/possystem-1.0-SNAPSHOT-all.jar --db=pos3 --name=POS-3 --port=8082
```

---

## Troubleshooting

### Issue: "Port already in use"
**Solution**: Make sure each instance uses a different `--port` value (8080, 8081, 8082, etc.)

### Issue: "H2 Console port 8082 already in use"
**Solution**: This is automatic now! Each database gets its own H2 console port:
- `possystemdb` → port 8082
- Other databases → port 8083+

### Issue: Auto-discovery not working
**Possible causes:**
1. Firewall blocking UDP port 9999
2. Both instances started but discovery not started
3. Both instances on different networks

**Solution**: Use manual connection instead

### Issue: Connection drops immediately
**Check:**
1. Both instances have servers running
2. Correct IP and port entered
3. No firewall blocking the ports

---

## Testing Checklist

- [ ] Build JAR successfully
- [ ] Run Instance 1 from IntelliJ with custom args
- [ ] Run Instance 2 from terminal JAR with custom args
- [ ] Both instances start without errors
- [ ] Both instances have different database files
- [ ] Both instances show different POS names
- [ ] Auto-discovery finds both instances
- [ ] Manual connection works
- [ ] Transaction on POS-1 appears in POS-2's journal
- [ ] Transaction on POS-2 appears in POS-1's journal
- [ ] Console shows colored journal entries (GREEN local, BLUE remote)
- [ ] Active Clients section shows connected clients
- [ ] Disconnect button works

---

## File Locations

### Databases
- Instance 1: `~/possystemdb_pos1.mv.db`
- Instance 2: `~/possystemdb_pos2.mv.db`

### Configuration
- Instance 1: `config/socket-config.json` (shared or separate)
- Instance 2: `config/socket-config.json` (shared or separate)

### Local Journals
- Instance 1: `logs/transactions/journal.log`
- Instance 2: `logs/transactions/journal.log`

### Remote Journals
- POS-1 receiving from POS-2: `logs/remote-journals/POS-2_[ip]/journal.log`
- POS-2 receiving from POS-1: `logs/remote-journals/POS-1_[ip]/journal.log`

---

## Clean Up

To reset and start fresh:

```bash
# Delete all database files
rm ~/possystemdb*.mv.db

# Delete configuration
rm config/socket-config.json

# Delete all journals
rm -rf logs/
```

---

## Success Criteria

✅ **Phase 2 is working correctly if:**
1. Both instances start without conflicts
2. UDP discovery finds both systems
3. Manual connection works as fallback
4. Transactions from one POS appear in the other's journal viewer
5. Console shows color-coded journal entries
6. Remote journals are saved to disk
7. Active Clients section shows connected clients

---

## Next Steps

Once Phase 2 is working:
1. Test with 3+ instances
2. Test disconnect/reconnect scenarios
3. Test network interruptions (kill connection)
4. Test with actual different machines (not localhost)
5. Debug any remaining connection issues from COPY_PASTE_PROMPT.md

---

**Happy Testing! 🚀**
