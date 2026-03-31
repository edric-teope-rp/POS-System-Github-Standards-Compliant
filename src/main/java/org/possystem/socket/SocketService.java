package org.possystem.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Core service for multi-POS journal synchronization
 * Handles server broadcasting, client connections, and UDP discovery
 */
public class SocketService {
    private static final Logger logger = LoggerFactory.getLogger(SocketService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Configuration
    private static final int UDP_BROADCAST_PORT = 9999;
    private static final int DISCOVERY_INTERVAL_MS = 5000;
    private static final int STALE_TIMEOUT_MS = 30000;
    private static final String REMOTE_JOURNAL_DIR = "logs/remote-journals/";

    private SocketConfig config;
    private String configPath;

    // Server
    private ServerSocket serverSocket;
    private boolean serverRunning;
    private final Set<ClientHandler> connectedClients = ConcurrentHashMap.newKeySet();
    private final Map<String, ConnectedClientInfo> clientHistory = new ConcurrentHashMap<>();

    // Client connections (where we SEND our logs TO)
    private final Map<String, ClientConnection> activeConnections = new ConcurrentHashMap<>();
    private final List<PrintWriter> outboundWriters = new CopyOnWriteArrayList<>();

    // Discovery
    private DatagramSocket discoverySocket;
    private boolean discoveryRunning;
    private final Map<String, PosSystemInfo> discoveredSystems = new ConcurrentHashMap<>();

    // Journal entries (merged, chronological)
    private final List<RemoteJournalEntry> journalEntries = new CopyOnWriteArrayList<>();

    // Thread pools
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Listeners
    private final List<JournalListener> journalListeners = new CopyOnWriteArrayList<>();
    private final List<DiscoveryListener> discoveryListeners = new CopyOnWriteArrayList<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<ServerClientListener> serverClientListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructor with default config
     */
    public SocketService(String configPath) {
        this(configPath, "", 0);
    }

    /**
     * Constructor with custom POS name and server port
     * @param configPath Path to config file
     * @param posName Custom POS name (empty string means use hostname)
     * @param serverPort Custom server port (0 means use config default)
     */
    public SocketService(String configPath, String posName, int serverPort) {
        this.configPath = configPath;
        this.config = SocketConfig.load(configPath);

        // Override config with command-line arguments if provided
        if (posName != null && !posName.isEmpty()) {
            this.config.setPosName(posName);
        }
        if (serverPort > 0) {
            this.config.setServerPort(serverPort);
        }

        // Save updated config
        this.config.save(configPath);

        System.out.println("DEBUG: SocketService initialized - POS Name: " + this.config.getPosName() + ", Server Port: " + this.config.getServerPort());
    }

    // ==================== Server Methods (Phase 2.1) ====================

    /**
     * Start the journal broadcasting server
     */
    public boolean startServer(int port) {
        if (serverRunning) {
            logger.warn("Server already running on port {}", config.getServerPort());
            return false;
        }

        try {
            serverSocket = new ServerSocket(port);
            serverRunning = true;
            config.setServerPort(port);
            config.save(configPath);

            // Accept client connections
            executorService.submit(() -> {
                logger.info("Server started on port {}", port);
                System.out.println("========================================");
                System.out.println("SERVER LISTENING FOR CONNECTIONS");
                System.out.println("Port: " + port);
                System.out.println("Waiting for Thomas, Raya, or other POS systems to connect...");
                System.out.println("========================================");

                while (serverRunning) {
                    try {
                        System.out.println("DEBUG: Waiting for next client connection...");
                        Socket clientSocket = serverSocket.accept();
                        String clientIp = clientSocket.getInetAddress().getHostAddress();
                        String clientHostname = clientSocket.getInetAddress().getHostName();
                        int clientPort = clientSocket.getPort();

                        System.out.println("========================================");
                        System.out.println("NEW CLIENT CONNECTION ACCEPTED!");
                        System.out.println("Client IP: " + clientIp);
                        System.out.println("Client Hostname: " + clientHostname);
                        System.out.println("Client Port: " + clientPort);
                        System.out.println("Time: " + LocalDateTime.now().format(FORMATTER));

                        // Try to identify the client
                        String identifiedAs = "Unknown";
                        if (clientIp.equals("192.168.8.186")) {
                            identifiedAs = "Thomas";
                        } else if (clientIp.equals("192.168.8.224")) {
                            identifiedAs = "Raya";
                        }
                        System.out.println("Identified as: " + identifiedAs);
                        System.out.println("========================================");

                        ClientHandler handler = new ClientHandler(clientSocket);
                        connectedClients.add(handler);
                        executorService.submit(handler);

                        // Add to client history
                        ConnectedClientInfo clientInfo = new ConnectedClientInfo(clientIp, handler.getConnectedAt(), true, null);
                        clientHistory.put(clientIp, clientInfo);

                        logger.info("Client connected: {}", clientIp);
                        notifyServerClientConnected(clientIp, handler.getConnectedAt());
                    } catch (IOException e) {
                        if (serverRunning) {
                            logger.error("Error accepting client connection", e);
                            System.err.println("ERROR accepting connection: " + e.getMessage());
                        }
                    }
                }
            });

            return true;
        } catch (IOException e) {
            logger.error("Failed to start server on port {}", port, e);
            return false;
        }
    }

    /**
     * Stop the journal broadcasting server
     */
    public void stopServer() {
        if (!serverRunning) {
            return;
        }

        serverRunning = false;
        try {
            // Close all client connections
            for (ClientHandler handler : connectedClients) {
                handler.close();
            }
            connectedClients.clear();
            clientHistory.clear(); // Clear client history on server stop

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            logger.info("Server stopped");
        } catch (IOException e) {
            logger.error("Error stopping server", e);
        }
    }

    /**
     * Broadcast a journal entry to all servers we're connected to (as client)
     * NEW ARCHITECTURE: Clients SEND to servers, not the other way around
     */
    public void broadcastJournalEntry(String journalLine) {
        System.out.println("DEBUG: broadcastJournalEntry called");
        System.out.println("DEBUG: outboundWriters.size() = " + outboundWriters.size());

        if (outboundWriters.isEmpty()) {
            System.out.println("DEBUG: No outbound connections, broadcast skipped");
            return;
        }

        System.out.println("DEBUG: Broadcasting to " + outboundWriters.size() + " server(s): " + journalLine);

        // Clean up dead connections while broadcasting
        List<PrintWriter> deadWriters = new ArrayList<>();

        for (PrintWriter writer : outboundWriters) {
            try {
                writer.println(journalLine);
                writer.flush();

                // Check if write failed (PrintWriter doesn't throw exceptions)
                if (writer.checkError()) {
                    System.out.println("DEBUG: Failed to send to server - marking for removal");
                    deadWriters.add(writer);
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Exception sending to server - marking for removal: " + e.getMessage());
                deadWriters.add(writer);
            }
        }

        // Remove dead writers
        if (!deadWriters.isEmpty()) {
            System.out.println("DEBUG: Removing " + deadWriters.size() + " dead writer(s)");
            outboundWriters.removeAll(deadWriters);
        }

        System.out.println("DEBUG: Active outbound connections after cleanup: " + outboundWriters.size());
    }

    /**
     * Handles individual client connections to the server
     * NEW ARCHITECTURE: Server RECEIVES logs FROM clients
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private final LocalDateTime connectedAt;
        private final String ipAddress;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.connectedAt = LocalDateTime.now();
            this.ipAddress = socket.getInetAddress().getHostAddress();
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                logger.error("Failed to create input/output streams for client", e);
            }
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public LocalDateTime getConnectedAt() {
            return connectedAt;
        }

        @Override
        public void run() {
            // Read incoming journal entries from client
            long connectionStart = System.currentTimeMillis();
            int messagesReceived = 0;

            try {
                String line;
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("CLIENT HANDLER STARTED");
                System.out.println("Client IP: " + ipAddress);
                System.out.println("Time: " + LocalDateTime.now().format(FORMATTER));
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // Send handshake acknowledgment
                if (out != null) {
                    String posName = config.getPosName().isEmpty() ? "SERVER" : config.getPosName();
                    String handshake = String.format("%s (%s)|%s|HANDSHAKE_ACK|Server ready",
                            posName, ipAddress,
                            LocalDateTime.now().format(FORMATTER));
                    out.println(handshake);
                    out.flush();
                    System.out.println("✅ SENT handshake ACK to " + ipAddress);
                } else {
                    System.err.println("⚠️  WARNING: PrintWriter is null, cannot send handshake ACK!");
                }

                System.out.println("📖 READING from client " + ipAddress + "...");
                System.out.println("   (Waiting for messages from client...)");

                while (serverRunning && (line = in.readLine()) != null) {
                    messagesReceived++;
                    long elapsed = (System.currentTimeMillis() - connectionStart) / 1000;

                    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    System.out.println("📨 MESSAGE RECEIVED #" + messagesReceived);
                    System.out.println("From: " + ipAddress);
                    System.out.println("Time: " + LocalDateTime.now().format(FORMATTER));
                    System.out.println("Connection duration: " + elapsed + "s");
                    System.out.println("Content: " + line);
                    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    // Handle handshake requests (acknowledge but don't process as journal entry)
                    if (line.contains("HANDSHAKE_REQUEST")) {
                        System.out.println("👋 Received HANDSHAKE_REQUEST from " + ipAddress);
                        System.out.println("   (Already sent ACK, continuing to read...)");
                        continue;
                    }

                    // Determine POS name from discovered systems or use IP
                    String posName = findPosNameByIp(ipAddress);
                    System.out.println("🔍 Processing as journal entry from: " + posName);
                    handleRemoteJournalEntry(line, posName, ipAddress);
                }

                long totalElapsed = (System.currentTimeMillis() - connectionStart) / 1000;
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("❌ CLIENT DISCONNECTED");
                System.out.println("Client IP: " + ipAddress);
                System.out.println("Total messages received: " + messagesReceived);
                System.out.println("Connection duration: " + totalElapsed + "s");
                System.out.println("Reason: Stream ended (client closed connection)");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            } catch (IOException e) {
                long totalElapsed = (System.currentTimeMillis() - connectionStart) / 1000;
                if (serverRunning) {
                    logger.error("Error reading from client {}", ipAddress, e);
                    System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    System.err.println("❌ ERROR READING FROM CLIENT");
                    System.err.println("Client IP: " + ipAddress);
                    System.err.println("Messages received before error: " + messagesReceived);
                    System.err.println("Connection duration: " + totalElapsed + "s");
                    System.err.println("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            } finally {
                close();
            }
        }

        public void close() {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (!socket.isClosed()) {
                    socket.close();
                }
                connectedClients.remove(this);

                // Update client history to show disconnected
                ConnectedClientInfo clientInfo = clientHistory.get(ipAddress);
                if (clientInfo != null) {
                    clientInfo.setConnected(false);
                    clientInfo.setDisconnectedAt(LocalDateTime.now());
                }

                notifyServerClientDisconnected(ipAddress);
            } catch (IOException e) {
                logger.error("Error closing client connection", e);
            }
        }
    }

    // ==================== Client Methods (Phase 2.2) ====================

    /**
     * Connect to a remote POS system
     */
    public boolean connectToRemotePOS(String ipAddress, int port, String posName) {
        String identifier = ipAddress + ":" + port;

        if (activeConnections.containsKey(identifier)) {
            logger.warn("Already connected to {}", identifier);
            return false;
        }

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), 5000); // 5 second timeout

            ClientConnection connection = new ClientConnection(socket, posName, ipAddress, port);
            activeConnections.put(identifier, connection);
            executorService.submit(connection);

            // Update discovered system status OR create new entry for manual connections
            PosSystemInfo sysInfo = discoveredSystems.get(identifier);
            if (sysInfo != null) {
                // Already discovered via UDP, just update status
                sysInfo.setConnected(true);
            } else {
                // Manual connection - create new PosSystemInfo entry
                String hostname = "Manual"; // We don't have hostname for manual connections
                sysInfo = new PosSystemInfo(posName, hostname, ipAddress, port);
                sysInfo.setConnected(true);
                discoveredSystems.put(identifier, sysInfo);

                // Notify discovery listeners so UI can update
                notifyDiscoveryListeners(sysInfo);

                System.out.println("DEBUG: Added manual connection to discovered systems: " + posName + " at " + ipAddress + ":" + port);
            }

            notifyConnectionListeners(posName, ipAddress, port, true);
            logger.info("Connected to remote POS: {} ({}:{})", posName, ipAddress, port);
            return true;
        } catch (IOException e) {
            logger.error("Failed to connect to {}:{}", ipAddress, port, e);
            return false;
        }
    }

    /**
     * Disconnect from a remote POS system
     */
    public void disconnectFromRemotePOS(String ipAddress, int port) {
        String identifier = ipAddress + ":" + port;
        ClientConnection connection = activeConnections.remove(identifier);

        if (connection != null) {
            connection.close();

            // Update discovered system status
            PosSystemInfo sysInfo = discoveredSystems.get(identifier);
            if (sysInfo != null) {
                sysInfo.setConnected(false);
            }

            notifyConnectionListeners(connection.posName, ipAddress, port, false);
            logger.info("Disconnected from remote POS: {} ({}:{})", connection.posName, ipAddress, port);
        }
    }

    /**
     * Handles connection to a remote POS system (sends our journal entries TO server)
     * NEW ARCHITECTURE: Client SENDS to server, doesn't read
     */
    private class ClientConnection implements Runnable {
        private final Socket socket;
        private final String posName;
        private final String ipAddress;
        private final int port;
        private PrintWriter out;

        public ClientConnection(Socket socket, String posName, String ipAddress, int port) {
            this.socket = socket;
            this.posName = posName;
            this.ipAddress = ipAddress;
            this.port = port;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                // Store writer for broadcasting
                outboundWriters.add(out);
                System.out.println("DEBUG: Client connection established to " + posName + " (" + ipAddress + ":" + port + ") - Writer added to outbound list");
            } catch (IOException e) {
                logger.error("Failed to create output stream for connection to {}:{}", ipAddress, port, e);
            }
        }

        @Override
        public void run() {
            // Keep connection alive, don't read (server will receive our broadcasts)
            try {
                socket.setSoTimeout(0); // No timeout
                System.out.println("DEBUG: Client connection active to " + posName + " - Waiting for disconnect");

                // Just keep connection alive until socket closes
                while (!socket.isClosed()) {
                    Thread.sleep(1000);
                }

                System.out.println("DEBUG: Connection to " + posName + " closed");
            } catch (Exception e) {
                logger.error("Connection lost to {} ({}:{})", posName, ipAddress, port);
            } finally {
                close();
            }
        }

        public void close() {
            try {
                // Remove from outbound writers
                if (out != null) {
                    outboundWriters.remove(out);
                    out.close();
                }
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing connection to {}:{}", ipAddress, port, e);
            }
        }
    }

    /**
     * Handle incoming journal entry from remote POS
     */
    private void handleRemoteJournalEntry(String journalLine, String posName, String ipAddress) {
        System.out.println("DEBUG: Received journal entry from " + posName + " (" + ipAddress + "): " + journalLine);

        // Parse journal entry
        RemoteJournalEntry entry = RemoteJournalEntry.parseJournalLine(journalLine, posName, ipAddress, false);
        if (entry == null) {
            System.err.println("ERROR: Failed to parse journal line from " + posName + ": " + journalLine);
            logger.warn("Invalid journal line from {}: {}", posName, journalLine);
            return;
        }

        System.out.println("DEBUG: Successfully parsed entry - Action: " + entry.getAction());

        // Add to merged journal
        journalEntries.add(entry);
        Collections.sort(journalEntries); // Keep chronological order

        // Update last log timestamp
        String identifier = ipAddress + ":" + activeConnections.values().stream()
                .filter(c -> c.ipAddress.equals(ipAddress))
                .findFirst()
                .map(c -> c.port)
                .orElse(0);
        PosSystemInfo sysInfo = discoveredSystems.get(identifier);
        if (sysInfo != null) {
            sysInfo.setLastLogTimestamp(entry.getTimestamp());
        }

        // Save to file
        saveRemoteJournal(posName, ipAddress, journalLine);

        // Notify listeners
        notifyJournalListeners(entry);

        // Print to console with color
        printColoredJournalEntry(entry);
    }

    /**
     * Save remote journal entry to file
     */
    private void saveRemoteJournal(String posName, String ipAddress, String journalLine) {
        try {
            String sanitizedName = posName.replaceAll("[^a-zA-Z0-9-_]", "_");
            Path journalDir = Paths.get(REMOTE_JOURNAL_DIR, sanitizedName + "_" + ipAddress);
            Files.createDirectories(journalDir);

            Path journalFile = journalDir.resolve("journal.log");
            Files.writeString(journalFile, journalLine + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Failed to save remote journal for {}", posName, e);
        }
    }

    // ==================== Discovery Methods (Phase 2.3) ====================

    /**
     * Start UDP discovery (broadcast and listen)
     */
    public void startDiscovery() {
        if (discoveryRunning) {
            System.out.println("DEBUG: Discovery already running");
            return;
        }

        try {
            System.out.println("DEBUG: Starting discovery on UDP port " + UDP_BROADCAST_PORT);
            discoverySocket = new DatagramSocket(UDP_BROADCAST_PORT);
            discoverySocket.setBroadcast(true);
            discoveryRunning = true;

            // Start broadcast sender
            scheduler.scheduleAtFixedRate(this::sendDiscoveryBroadcast, 0, DISCOVERY_INTERVAL_MS, TimeUnit.MILLISECONDS);
            System.out.println("DEBUG: Discovery broadcast sender scheduled (every " + (DISCOVERY_INTERVAL_MS/1000) + " seconds)");

            // Start broadcast listener
            executorService.submit(this::listenForDiscovery);
            System.out.println("DEBUG: Discovery broadcast listener started");

            // Start stale entry remover
            scheduler.scheduleAtFixedRate(this::removeStaleEntries, STALE_TIMEOUT_MS, STALE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            logger.info("Discovery started");
            System.out.println("DEBUG: Discovery started successfully");
        } catch (IOException e) {
            logger.error("Failed to start discovery", e);
            System.err.println("DEBUG: Failed to start discovery: " + e.getMessage());
            System.err.println("DEBUG: This usually means UDP port " + UDP_BROADCAST_PORT + " is already in use");
            discoveryRunning = false;
        }
    }

    /**
     * Stop UDP discovery
     */
    public void stopDiscovery() {
        if (!discoveryRunning) {
            return;
        }

        discoveryRunning = false;
        if (discoverySocket != null && !discoverySocket.isClosed()) {
            discoverySocket.close();
        }
        logger.info("Discovery stopped");
    }

    /**
     * Send UDP broadcast announcing this POS system
     */
    private void sendDiscoveryBroadcast() {
        if (!discoveryRunning || !serverRunning) {
            return;
        }

        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String posName = config.getPosName().isEmpty() ? hostname : config.getPosName();
            int port = config.getServerPort();

            String message = "POS_DISCOVERY|" + posName + "|" + hostname + "|" + port;
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), UDP_BROADCAST_PORT);
            discoverySocket.send(packet);
            System.out.println("DEBUG: Sent discovery broadcast - " + posName + " on port " + port);
        } catch (IOException e) {
            logger.error("Failed to send discovery broadcast", e);
            System.err.println("DEBUG: Failed to send discovery broadcast: " + e.getMessage());
        }
    }

    /**
     * Listen for UDP broadcasts from other POS systems
     */
    private void listenForDiscovery() {
        byte[] buffer = new byte[1024];
        System.out.println("DEBUG: Discovery listener started, waiting for broadcasts on UDP port " + UDP_BROADCAST_PORT);

        while (discoveryRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                discoverySocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderIP = packet.getAddress().getHostAddress();

                System.out.println("DEBUG: Received UDP broadcast from " + senderIP + ": " + message);

                // Parse discovery message: POS_DISCOVERY|posName|hostname|port
                String[] parts = message.split("\\|");
                if (parts.length == 4 && parts[0].equals("POS_DISCOVERY")) {
                    String posName = parts[1];
                    String hostname = parts[2];
                    int port = Integer.parseInt(parts[3]);

                    // Don't add self (check both IP and port)
                    if (isLocalAddress(senderIP) && port == config.getServerPort()) {
                        System.out.println("DEBUG: Ignoring broadcast from self (" + senderIP + ":" + port + ")");
                        continue;
                    }

                    String identifier = senderIP + ":" + port;
                    PosSystemInfo sysInfo = discoveredSystems.get(identifier);

                    if (sysInfo == null) {
                        // New system discovered
                        System.out.println("DEBUG: New POS discovered - " + posName + " at " + senderIP + ":" + port);
                        sysInfo = new PosSystemInfo(posName, hostname, senderIP, port);
                        discoveredSystems.put(identifier, sysInfo);
                        notifyDiscoveryListeners(sysInfo);
                        logger.info("Discovered POS: {} at {}:{}", posName, senderIP, port);
                    } else {
                        // Update last seen timestamp
                        sysInfo.setLastSeen(LocalDateTime.now());
                    }
                } else {
                    System.out.println("DEBUG: Invalid discovery message format: " + message);
                }
            } catch (IOException e) {
                if (discoveryRunning) {
                    logger.error("Error receiving discovery broadcast", e);
                    System.err.println("DEBUG: Error receiving discovery broadcast: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Remove stale discovered systems
     */
    private void removeStaleEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(STALE_TIMEOUT_MS / 1000);

        discoveredSystems.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastSeen().isBefore(cutoff) && !entry.getValue().isConnected()) {
                logger.info("Removing stale POS: {}", entry.getValue().getDisplayName());
                return true;
            }
            return false;
        });
    }

    /**
     * Check if IP address is local
     */
    private boolean isLocalAddress(String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            return addr.isLoopbackAddress() ||
                   addr.equals(InetAddress.getLocalHost()) ||
                   ipAddress.equals(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Find POS name by IP address from discovered systems
     */
    private String findPosNameByIp(String ipAddress) {
        for (PosSystemInfo sysInfo : discoveredSystems.values()) {
            if (sysInfo.getIpAddress().equals(ipAddress)) {
                return sysInfo.getDisplayName();
            }
        }
        // Fallback to IP if not found
        return ipAddress;
    }

    // ==================== Local Journal Methods ====================

    /**
     * Add local journal entry (called by TransactionService)
     */
    public void addLocalJournalEntry(String journalLine) {
        // Broadcast to connected clients
        broadcastJournalEntry(journalLine);

        // Parse and add to merged journal
        String posName = config.getPosName().isEmpty() ? "LOCAL" : config.getPosName();
        RemoteJournalEntry entry = RemoteJournalEntry.parseJournalLine(journalLine, posName, "local", true);
        if (entry != null) {
            journalEntries.add(entry);
            Collections.sort(journalEntries);
            notifyJournalListeners(entry);
            printColoredJournalEntry(entry);
        }
    }

    /**
     * Print journal entry with ANSI color coding to console
     */
    private void printColoredJournalEntry(RemoteJournalEntry entry) {
        String color = getColorForPOS(entry.getPosName(), entry.isLocal());
        String reset = "\u001B[0m";
        System.out.println(color + entry.toDisplayString() + reset);
    }

    /**
     * Get ANSI color code for POS
     */
    private String getColorForPOS(String posName, boolean isLocal) {
        if (isLocal) {
            return "\u001B[32m"; // Green for local
        }

        // Assign colors to remote POS systems
        int hash = Math.abs(posName.hashCode());
        String[] colors = {
                "\u001B[34m",  // Blue
                "\u001B[33m",  // Yellow
                "\u001B[35m",  // Magenta
                "\u001B[36m",  // Cyan
                "\u001B[31m"   // Red
        };
        return colors[hash % colors.length];
    }

    // ==================== Listener Management ====================

    public void addJournalListener(JournalListener listener) {
        journalListeners.add(listener);
    }

    public void addDiscoveryListener(DiscoveryListener listener) {
        discoveryListeners.add(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void addServerClientListener(ServerClientListener listener) {
        serverClientListeners.add(listener);
    }

    private void notifyJournalListeners(RemoteJournalEntry entry) {
        for (JournalListener listener : journalListeners) {
            listener.onJournalEntry(entry);
        }
    }

    private void notifyDiscoveryListeners(PosSystemInfo sysInfo) {
        for (DiscoveryListener listener : discoveryListeners) {
            listener.onPosDiscovered(sysInfo);
        }
    }

    private void notifyConnectionListeners(String posName, String ipAddress, int port, boolean connected) {
        for (ConnectionListener listener : connectionListeners) {
            if (connected) {
                listener.onConnected(posName, ipAddress, port);
            } else {
                listener.onDisconnected(posName, ipAddress, port);
            }
        }
    }

    private void notifyServerClientConnected(String ipAddress, LocalDateTime connectedAt) {
        for (ServerClientListener listener : serverClientListeners) {
            listener.onClientConnected(ipAddress, connectedAt);
        }
    }

    private void notifyServerClientDisconnected(String ipAddress) {
        for (ServerClientListener listener : serverClientListeners) {
            listener.onClientDisconnected(ipAddress);
        }
    }

    // ==================== Getters ====================

    public boolean isServerRunning() {
        return serverRunning;
    }

    public boolean isDiscoveryRunning() {
        return discoveryRunning;
    }

    public SocketConfig getConfig() {
        return config;
    }

    public Collection<PosSystemInfo> getDiscoveredSystems() {
        return new ArrayList<>(discoveredSystems.values());
    }

    public List<RemoteJournalEntry> getJournalEntries() {
        return new ArrayList<>(journalEntries);
    }

    public int getConnectedClientsCount() {
        return connectedClients.size();
    }

    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }

    /**
     * Get list of clients connected to this server (including disconnected clients)
     */
    public List<ConnectedClientInfo> getConnectedClientsList() {
        return new ArrayList<>(clientHistory.values());
    }

    // ==================== Shutdown ====================

    public void shutdown() {
        logger.info("Shutting down SocketService");
        stopServer();
        stopDiscovery();

        // Close all client connections
        for (ClientConnection connection : activeConnections.values()) {
            connection.close();
        }
        activeConnections.clear();

        executorService.shutdown();
        scheduler.shutdown();
    }

    // ==================== Listener Interfaces ====================

    public interface JournalListener {
        void onJournalEntry(RemoteJournalEntry entry);
    }

    public interface DiscoveryListener {
        void onPosDiscovered(PosSystemInfo sysInfo);
    }

    public interface ConnectionListener {
        void onConnected(String posName, String ipAddress, int port);
        void onDisconnected(String posName, String ipAddress, int port);
    }

    public interface ServerClientListener {
        void onClientConnected(String ipAddress, LocalDateTime connectedAt);
        void onClientDisconnected(String ipAddress);
    }

    // ==================== Data Classes ====================

    /**
     * Information about a client connected to this server
     */
    public static class ConnectedClientInfo {
        private final String ipAddress;
        private final LocalDateTime connectedAt;
        private boolean connected;
        private LocalDateTime disconnectedAt;

        public ConnectedClientInfo(String ipAddress, LocalDateTime connectedAt, boolean connected, LocalDateTime disconnectedAt) {
            this.ipAddress = ipAddress;
            this.connectedAt = connectedAt;
            this.connected = connected;
            this.disconnectedAt = disconnectedAt;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public LocalDateTime getConnectedAt() {
            return connectedAt;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public LocalDateTime getDisconnectedAt() {
            return disconnectedAt;
        }

        public void setDisconnectedAt(LocalDateTime disconnectedAt) {
            this.disconnectedAt = disconnectedAt;
        }
    }
}
