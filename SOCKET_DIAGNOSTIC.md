# Socket Connection Diagnostic Guide

## Issue: Not Seeing Available Sockets

Let's debug this step by step:

---

## Step 1: Check Console for Error Messages

When you open Socket Configuration dialog, check the console for these messages:

**Expected messages:**
```
INFO - Discovery started
INFO - Server started on port 9000
```

**If you see errors like:**
```
ERROR - Failed to start discovery
ERROR - Failed to start server on port 9000
```

Then we have a port conflict or permission issue.

---

## Step 2: Test on Same Machine First

If testing on one machine with two POS instances:

**Terminal 1 (POS 1):**
```bash
./gradlew run
```
- Settings → Socket Configuration
- POS Name: `Counter-1`
- Server Port: `9000` ← **IMPORTANT: Use 9000**
- Click "Start Server"
- **Watch console** for "Server started on port 9000"

**Terminal 2 (POS 2):**
```bash
./gradlew run
```
- Settings → Socket Configuration
- POS Name: `Counter-2`
- Server Port: `9001` ← **IMPORTANT: Use 9001 (different port!)**
- Click "Start Server"
- **Watch console** for "Server started on port 9001"

**Then in EITHER POS:**
- Click "Refresh" button
- **Watch console** for "Discovered POS: Counter-X at 127.0.0.1:XXXX"

---

## Step 3: Manual Connection Test

If auto-discovery doesn't work, try manual connection:

**In POS 1 (listening on port 9000):**
1. Keep server running
2. Note the IP: `127.0.0.1` (or `localhost`)

**In POS 2:**
1. Click "+ Add Manual"
2. Fill in:
   - POS Name: `Counter-1`
   - IP Address: `127.0.0.1`
   - Port: `9000` ← **Must match POS 1's server port**
3. Click OK
4. **Watch console for:**
   - Success: "Connected to remote POS: Counter-1 (127.0.0.1:9000)"
   - Failure: "Failed to connect to 127.0.0.1:9000" + error details

---

## Step 4: Check Actual IP Address

On macOS, find your actual IP:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

You should see something like: `inet 192.168.1.100`

Try using this IP instead of `127.0.0.1` when manually connecting.

---

## Step 5: Common Issues

### Issue A: "Port already in use"
**Symptom:** Server won't start, or immediate crash
**Solution:** Another app is using port 9000
- Try port 9001, 9002, etc.
- Or kill the process using the port

### Issue B: "No POS discovered after Refresh"
**Symptom:** Table stays empty after clicking Refresh
**Possible causes:**
1. **Discovery not started** - Check console for "Discovery started"
2. **Firewall blocking UDP port 9999** - Try disabling firewall temporarily
3. **Running in different networks** - Both POS must be on same network
4. **Server not broadcasting** - Make sure "Start Server" was clicked

### Issue C: "Manual connection fails"
**Symptom:** Connection attempt but no success dialog
**Possible causes:**
1. **Wrong IP address** - Use 127.0.0.1 for same machine, or actual IP for different machines
2. **Wrong port** - Must match the **server port** of the target POS
3. **Server not running** - Target POS must have server started first
4. **Firewall blocking TCP connection** - Try disabling firewall

---

## Step 6: Enable Debug Logging

We can add more verbose logging. Let me know if none of the above works and I'll add debug statements to:
1. Show when UDP broadcasts are sent
2. Show when UDP broadcasts are received
3. Show TCP connection attempts in detail
4. Show discovery table updates

---

## Quick Test Checklist

Try this in order:

- [ ] **POS 1**: Start server on port 9000 → See "Server started" in console
- [ ] **POS 2**: Start server on port 9001 → See "Server started" in console
- [ ] **POS 2**: Click Refresh → Wait 10 seconds → Check console for "Discovered POS"
- [ ] **POS 2**: If nothing, try manual: "+ Add Manual" → 127.0.0.1:9000
- [ ] **Check console** in both POS for connection success/failure messages

---

## What to Tell Me

Please provide:
1. **Exact console output** from both POS systems (copy/paste)
2. **Which step fails?** (server start, discovery, manual connection)
3. **Any error messages** you see
4. **Testing on:** Same machine or different machines?
5. **Operating system:** macOS, Windows, Linux?

This will help me pinpoint the exact issue!
