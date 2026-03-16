# Phase 2: Multi-POS Journal Viewer - Quick Start Guide

## 🚀 Quick Test (Single POS)

1. **Start POS:**
   ```bash
   ./gradlew run
   ```

2. **Open Socket Configuration:**
   - Click **Settings** (gear icon in top-right)
   - Click **Socket Configuration** button

3. **Configure This POS:**
   - POS Name: `Main-Register` (optional)
   - Server Port: `9000`
   - Click **Start Server**

4. **Test Broadcasting:**
   - Go back to main POS screen
   - Add items to cart
   - Complete a transaction
   - Watch your **console** for green LOCAL journal entries

5. **View Live Journal:**
   - Return to Socket Configuration dialog
   - Check "Live Journal Viewer" at the bottom
   - Should show all your transaction logs

---

## 🔗 Multi-POS Test (2 POS Systems)

### Terminal 1 - POS #1
```bash
./gradlew run
```
**In UI:**
1. Settings → Socket Configuration
2. POS Name: `Counter-1`
3. Server Port: `9000`
4. Click **Start Server**
5. Leave dialog open

### Terminal 2 - POS #2
```bash
./gradlew run
```
**In UI:**
1. Settings → Socket Configuration
2. POS Name: `Counter-2`
3. Server Port: `9001`
4. Click **Start Server**
5. Leave dialog open

### Connect Them
**In POS #1:**
1. Click **Refresh** button
2. See `Counter-2` appear in table
3. Click **Connect** button next to it

**In POS #2:**
1. Click **Refresh** button
2. See `Counter-1` appear in table
3. Click **Connect** button next to it

### Test Real-Time Sync
1. **In POS #1:** Add item → Total → Pay with Cash
2. **In POS #2:** Immediately check:
   - Console shows **blue** colored entry from Counter-1
   - Live Journal Viewer shows Counter-1's transaction
3. **In POS #2:** Add item → Total → Pay with Card
4. **In POS #1:** See **yellow** colored entry from Counter-2

---

## 📊 What You Should See

### Console Output (Color-Coded)
```
[GREEN]  2025-01-15 10:45:20.123|LOCAL|TX_CREATE|TX_ID:1
[GREEN]  2025-01-15 10:45:21.456|LOCAL|ITEM_ADD|TX_ID:1|ITEM_ID:1|UPC:001|NAME:Coffee|...
[BLUE]   2025-01-15 10:45:22.789|Counter-2 (192.168.1.100)|TX_CREATE|TX_ID:2
[GREEN]  2025-01-15 10:45:23.012|LOCAL|TX_COMPLETE|TX_ID:1|TENDER:CASH|...
[BLUE]   2025-01-15 10:45:24.345|Counter-2 (192.168.1.100)|ITEM_ADD|...
```

### Socket Configuration Dialog
**This POS Configuration:**
- Status: `● Broadcasting on port 9000` (green indicator)

**Available POS Systems Table:**
| POS Name  | IP Address      | Port | Status    | Last Log            | Action     |
|-----------|-----------------|------|-----------|---------------------|------------|
| Counter-2 | 192.168.1.100   | 9001 | Connected | 2025-01-15 10:45:24 | Disconnect |
| Counter-3 | 192.168.1.101   | 9000 | Available | N/A                 | Connect    |

**Live Journal Viewer:**
```
2025-01-15 10:45:20.123|LOCAL|TX_CREATE|TX_ID:1
2025-01-15 10:45:21.456|LOCAL|ITEM_ADD|TX_ID:1|ITEM_ID:1|...
2025-01-15 10:45:22.789|Counter-2 (192.168.1.100)|TX_CREATE|TX_ID:2
2025-01-15 10:45:23.012|LOCAL|TX_COMPLETE|TX_ID:1|...
2025-01-15 10:45:24.345|Counter-2 (192.168.1.100)|ITEM_ADD|...
```

---

## 🔧 Manual Connection

If auto-discovery doesn't work:

1. Click **+ Add Manual** button
2. Fill in:
   - POS Name: `Remote-Store`
   - IP Address: `192.168.1.100` (actual IP of remote POS)
   - Port: `9000` (port remote POS is broadcasting on)
3. Click **OK**
4. Connection established!

---

## 📁 Check Remote Journals

Remote journals are automatically saved to:
```
logs/
└── remote-journals/
    ├── Counter-1_192.168.1.100/
    │   └── journal.log
    └── Counter-2_192.168.1.101/
        └── journal.log
```

**View a remote journal:**
```bash
tail -f logs/remote-journals/Counter-2_192.168.1.100/journal.log
```

---

## ⚠️ Common Issues

### "Port 9000 is in use"
**Solution:** Use a different port
- Change Server Port to `9001` or `9002`
- Click Start Server

### "No POS systems discovered"
**Solution 1:** Manual refresh
- Click **Refresh** button

**Solution 2:** Manual connection
- Click **+ Add Manual**
- Enter IP and Port manually

**Solution 3:** Check network
- Ensure both POS systems are on same network
- Ping the other POS: `ping 192.168.1.100`

### "Connection lost"
**Solution:** Reconnect
- Remote POS may have restarted
- Click **Connect** button again

---

## 🎯 Testing Checklist

- [ ] Single POS: Start server, see local logs
- [ ] Two POS: Both servers started
- [ ] Auto-discovery: Both POS appear in each other's table
- [ ] Connect: Both POS connected to each other
- [ ] Real-time sync: Transactions from one POS appear on the other
- [ ] Console colors: GREEN for local, BLUE/YELLOW for remote
- [ ] Live Journal Viewer: Shows merged logs
- [ ] Remote journal files: Check `logs/remote-journals/` directory
- [ ] Manual connection: Add a POS manually by IP:Port
- [ ] Persistence: Close POS, reopen, saved connections still listed
- [ ] Disconnect: Click Disconnect button, status changes to "Available"
- [ ] Stop server: Click Stop Server, status shows "Server Stopped"

---

## 💡 Tips

1. **Always start servers first** before trying to connect
2. **Wait a few seconds** after starting server for discovery to work
3. **Check console** for color-coded real-time logs
4. **Keep Socket Configuration dialog open** to monitor connections
5. **Use different ports** (9000, 9001, 9002) for each POS on same machine

---

## 🎉 Success Indicators

✅ Server status shows green "● Broadcasting"
✅ Remote POS appears in table after clicking Refresh
✅ Connection status changes to "Connected"
✅ Console shows color-coded entries from multiple POS
✅ Live Journal Viewer displays merged logs in chronological order
✅ Remote journal files are created in `logs/remote-journals/`

---

**Ready to test!** Start with the Single POS test, then move to Multi-POS test once comfortable.
