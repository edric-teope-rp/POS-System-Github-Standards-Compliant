package org.possystem.ui;

import org.possystem.socket.PosSystemInfo;
import org.possystem.socket.RemoteJournalEntry;
import org.possystem.socket.SocketService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket Configuration Dialog - Phase 2.4
 * UI for managing multi-POS journal connections
 */
public class SocketConfigDialog extends JDialog {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Frame parentFrame;
    private final SocketService socketService;
    private final org.possystem.service.AuthService authService;

    // Dynamic font scaling based on screen resolution
    private final float fontScale;
    private final int baseFontSize = 14;
    private final int headerFontSize;
    private final int labelFontSize;
    private final int buttonFontSize;
    private final int tableFontSize;
    private final int titleFontSize;

    // This POS Configuration
    private JTextField posNameField;
    private JTextField hostnameField;
    private JTextField ipAddressField;
    private JTextField serverPortField;
    private JLabel serverStatusLabel;
    private JButton serverToggleButton;

    // Active Clients Table
    private JTable activeClientsTable;
    private DefaultTableModel activeClientsTableModel;
    private JLabel activeClientsCountLabel;

    // Available POS Systems Table
    private JTable posSystemsTable;
    private DefaultTableModel tableModel;
    private final Map<String, Integer> rowMap = new ConcurrentHashMap<>(); // identifier -> row

    // Live Journal Viewer
    private JTextArea journalViewer;
    private JScrollPane journalScrollPane;
    private PinnedJournalViewerWindow pinnedWindow;

    // Color mapping for POS systems
    private final Map<String, Color> posColors = new ConcurrentHashMap<>();
    private int colorIndex = 0;
    private final Color[] COLORS = {
            new Color(34, 139, 34),   // Green (local)
            new Color(30, 144, 255),  // Blue
            new Color(255, 215, 0),   // Yellow
            new Color(218, 112, 214), // Magenta
            new Color(64, 224, 208),  // Cyan
            new Color(220, 20, 60)    // Red
    };

    public SocketConfigDialog(Frame parent, SocketService socketService, org.possystem.service.AuthService authService) {
        super(parent, "Socket Configuration", Dialog.ModalityType.MODELESS);
        this.parentFrame = parent;
        this.socketService = socketService;
        this.authService = authService;

        // Calculate font scaling based on screen resolution
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;

        // Base scale on 1080p (1920x1080) as reference
        // Scale factor: 720p=0.85, 1080p=1.0, 1440p=1.25, 4K=1.75
        if (screenHeight <= 768) {
            fontScale = 0.85f;
        } else if (screenHeight <= 1080) {
            fontScale = 1.0f;
        } else if (screenHeight <= 1440) {
            fontScale = 1.25f;
        } else {
            fontScale = 1.75f;
        }

        // Calculate scaled font sizes
        headerFontSize = Math.round(24 * fontScale);
        labelFontSize = Math.round(baseFontSize * fontScale);
        buttonFontSize = Math.round(14 * fontScale);
        tableFontSize = Math.round(14 * fontScale);
        titleFontSize = Math.round(13 * fontScale);

        initializeDialog();
        createUI();
        wireListeners();
        loadConfiguration();
        refreshPosSystemsTable();
    }

    private void initializeDialog() {
        // Personalized dialog styling
        setUndecorated(true);
        setResizable(false);
        setSize(1400, 800);  // Wider for 2x2 grid layout
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void createUI() {
        // Custom Header Panel with drag functionality
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main panel with 2x2 grid layout (each zone = 25% of screen)
        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        // Upper Left: POS Configuration (25%)
        mainPanel.add(createThisPOSPanel());

        // Upper Right: Available POS Systems (25%)
        mainPanel.add(createPosSystemsPanel());

        // Lower Left: Live Journal Viewer (25%)
        mainPanel.add(createJournalViewerPanel());

        // Lower Right: Active Clients (25%)
        mainPanel.add(createActiveClientsPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Create custom header panel with title and close button
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180));  // Steel blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Title label with username
        String username = authService.getCurrentUsername();
        String titleText = "Socket Configuration - Logged in as: " + (username != null ? username : "Unknown");
        JLabel headerLabel = new JLabel(titleText);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Right panel for logout and close buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(new Color(70, 130, 180));

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, Math.round(12 * fontScale)));
        logoutButton.setBackground(new Color(220, 53, 69)); // Red
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(Math.round(110 * fontScale), Math.round(35 * fontScale)));
        applyRoundedStyleWithBlackOutline(logoutButton);
        logoutButton.addActionListener(e -> {
            authService.logout();
            dispose();
        });
        rightPanel.add(logoutButton);

        // Close button (X) with custom icon
        JButton closeButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(3));

                // Set color based on hover state
                Boolean hovered = (Boolean) getClientProperty("hovered");
                if (hovered != null && hovered) {
                    g2.setColor(new Color(255, 100, 100)); // Lighter red on hover
                } else {
                    g2.setColor(new Color(220, 53, 69)); // Red
                }

                int padding = 10;
                int x1 = padding;
                int y1 = padding;
                int x2 = getWidth() - padding;
                int y2 = getHeight() - padding;

                // Draw X shape
                g2.drawLine(x1, y1, x2, y2);
                g2.drawLine(x2, y1, x1, y2);

                g2.dispose();
            }
        };
        closeButton.setBackground(new Color(70, 130, 180));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(40, 40));
        closeButton.putClientProperty("hovered", false);
        closeButton.addActionListener(e -> dispose());
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                ((JButton)e.getSource()).putClientProperty("hovered", true);
                e.getComponent().repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                ((JButton)e.getSource()).putClientProperty("hovered", false);
                e.getComponent().repaint();
            }
        });
        rightPanel.add(closeButton);

        headerPanel.add(rightPanel, BorderLayout.EAST);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        return headerPanel;
    }

    /**
     * Create "This POS Configuration" panel
     */
    private JPanel createThisPOSPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder("This POS Configuration");
        titledBorder.setTitleFont(new Font("Arial", Font.BOLD, titleFontSize));
        panel.setBorder(titledBorder);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // POS Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel posNameLabel = new JLabel("POS Name:");
        posNameLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        panel.add(posNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        posNameField = new JTextField(20);
        posNameField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        posNameField.setToolTipText("Optional custom name for this POS (Double-click for on-screen keyboard)");
        posNameField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click opens on-screen keyboard
                    openAlphanumericKeyboard(posNameField, "POS Name");
                }
                // Single click just focuses the field for physical keyboard input
            }
        });
        panel.add(posNameField, gbc);

        // Hostname (read-only but selectable)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel hostnameLabel = new JLabel("Hostname:");
        hostnameLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        panel.add(hostnameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String hostname = getHostname();
        hostnameField = new JTextField(hostname);
        hostnameField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        hostnameField.setEditable(false);
        hostnameField.setForeground(Color.GRAY);
        hostnameField.setBackground(Color.WHITE);
        hostnameField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        hostnameField.setToolTipText("Click to select and copy");
        panel.add(hostnameField, gbc);

        // IP Address (read-only but selectable)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel ipLabel = new JLabel("IP Address:");
        ipLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        panel.add(ipLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String ipAddress = getLocalIPAddress();
        ipAddressField = new JTextField(ipAddress);
        ipAddressField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        ipAddressField.setEditable(false);
        ipAddressField.setForeground(Color.GRAY);
        ipAddressField.setBackground(Color.WHITE);
        ipAddressField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        ipAddressField.setToolTipText("Click to select and copy");
        panel.add(ipAddressField, gbc);

        // Server Port
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        JLabel portLabel = new JLabel("Server Port:");
        portLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        panel.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverPortField = new JTextField(10);
        serverPortField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        serverPortField.setToolTipText("Server Port (Double-click for on-screen keypad)");
        serverPortField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click opens on-screen keypad
                    openNumericKeypad(serverPortField, "Server Port", false);
                }
                // Single click just focuses the field for physical keyboard input
            }
        });
        panel.add(serverPortField, gbc);

        // Server Status and Toggle Button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        panel.add(statusLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverStatusLabel = new JLabel("Server Stopped");
        serverStatusLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        serverStatusLabel.setForeground(Color.RED);
        statusPanel.add(serverStatusLabel);

        serverToggleButton = new JButton("Start Server");
        serverToggleButton.setBackground(new Color(40, 167, 69));  // Green
        serverToggleButton.setForeground(Color.WHITE);
        serverToggleButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        applyRoundedStyle(serverToggleButton);
        serverToggleButton.addActionListener(e -> toggleServer());
        statusPanel.add(serverToggleButton);

        panel.add(statusPanel, gbc);

        return panel;
    }

    /**
     * Create "Active Clients Connected to This Server" panel
     */
    private JPanel createActiveClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder("🔌 Active Clients Connected to This Server");
        titledBorder.setTitleFont(new Font("Arial", Font.BOLD, titleFontSize));
        panel.setBorder(titledBorder);

        // Table
        String[] columnNames = {"IP Address", "Connected At", "Duration", "Status"};
        activeClientsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        activeClientsTable = new JTable(activeClientsTableModel);
        activeClientsTable.setFont(new Font("Arial", Font.PLAIN, tableFontSize));
        activeClientsTable.setRowHeight(Math.round(25 * fontScale));
        activeClientsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, tableFontSize));
        JScrollPane scrollPane = new JScrollPane(activeClientsTable);
        scrollPane.setPreferredSize(new Dimension(0, 100)); // Compact height
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status label
        activeClientsCountLabel = new JLabel("Total: 0 active clients");
        activeClientsCountLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        activeClientsCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(activeClientsCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create "Available POS Systems" panel with table
     */
    private JPanel createPosSystemsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder("Available POS Systems");
        titledBorder.setTitleFont(new Font("Arial", Font.BOLD, titleFontSize));
        panel.setBorder(titledBorder);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(70, 130, 180));  // Steel blue
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(refreshButton);
        refreshButton.addActionListener(e -> refreshPosSystemsTable());
        toolbar.add(refreshButton);

        JButton addManualButton = new JButton("+ Add Manual");
        addManualButton.setBackground(new Color(108, 117, 125));  // Gray
        addManualButton.setForeground(Color.WHITE);
        addManualButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(addManualButton);
        addManualButton.addActionListener(e -> showAddManualDialog());
        toolbar.add(addManualButton);

        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"POS Name", "IP Address", "Port", "Status", "Last Log", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only Action column is editable (button)
            }
        };

        posSystemsTable = new JTable(tableModel);
        posSystemsTable.setFont(new Font("Arial", Font.PLAIN, tableFontSize));
        posSystemsTable.setRowHeight(Math.round(30 * fontScale));
        posSystemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, tableFontSize));
        posSystemsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        posSystemsTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(posSystemsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("Connected: 0");
        statusBar.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(statusBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create "Live Journal Viewer" panel
     */
    private JPanel createJournalViewerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder("Live Journal Viewer");
        titledBorder.setTitleFont(new Font("Arial", Font.BOLD, titleFontSize));
        panel.setBorder(titledBorder);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Pin Button
        JButton pinButton = new JButton("📌 Pin");
        pinButton.setBackground(new Color(40, 167, 69));  // Green
        pinButton.setForeground(Color.WHITE);
        pinButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(pinButton);
        pinButton.addActionListener(e -> togglePinnedWindow());
        toolbar.add(pinButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setBackground(new Color(220, 53, 69));  // Red
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(clearButton);
        clearButton.addActionListener(e -> journalViewer.setText(""));
        toolbar.add(clearButton);

        JButton exportButton = new JButton("Export");
        exportButton.setBackground(new Color(23, 162, 184));  // Teal
        exportButton.setForeground(Color.WHITE);
        exportButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(exportButton);
        exportButton.addActionListener(e -> exportJournal());
        toolbar.add(exportButton);

        panel.add(toolbar, BorderLayout.NORTH);

        // Journal text area
        journalViewer = new JTextArea();
        journalViewer.setEditable(false);
        journalViewer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        journalScrollPane = new JScrollPane(journalViewer);
        panel.add(journalScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Wire up SocketService listeners
     */
    private void wireListeners() {
        System.out.println("DEBUG: Wiring socket service listeners");

        // Journal entry listener
        socketService.addJournalListener(entry -> SwingUtilities.invokeLater(() -> {
            appendJournalEntry(entry);
        }));

        // Discovery listener
        socketService.addDiscoveryListener(sysInfo -> {
            System.out.println("DEBUG: Discovery event - New POS discovered: " + sysInfo.getDisplayName() + " at " + sysInfo.getIpAddress() + ":" + sysInfo.getPort());
            SwingUtilities.invokeLater(() -> {
                addOrUpdatePosSystem(sysInfo);
            });
        });

        // Connection listener
        socketService.addConnectionListener(new SocketService.ConnectionListener() {
            @Override
            public void onConnected(String posName, String ipAddress, int port) {
                System.out.println("DEBUG: Connection event - Connected to " + posName + " (" + ipAddress + ":" + port + ")");
                SwingUtilities.invokeLater(() -> updateConnectionStatus(ipAddress + ":" + port, true));
            }

            @Override
            public void onDisconnected(String posName, String ipAddress, int port) {
                System.out.println("DEBUG: Connection event - Disconnected from " + posName + " (" + ipAddress + ":" + port + ")");
                SwingUtilities.invokeLater(() -> updateConnectionStatus(ipAddress + ":" + port, false));
            }
        });

        // Server client listener (clients connecting to THIS server)
        socketService.addServerClientListener(new SocketService.ServerClientListener() {
            @Override
            public void onClientConnected(String ipAddress, java.time.LocalDateTime connectedAt) {
                System.out.println("DEBUG: Server client connected: " + ipAddress);
                SwingUtilities.invokeLater(() -> refreshActiveClientsTable());
            }

            @Override
            public void onClientDisconnected(String ipAddress) {
                System.out.println("DEBUG: Server client disconnected: " + ipAddress);
                SwingUtilities.invokeLater(() -> refreshActiveClientsTable());
            }
        });

        // Start discovery automatically
        if (!socketService.isDiscoveryRunning()) {
            System.out.println("DEBUG: Starting discovery service...");
            socketService.startDiscovery();
        } else {
            System.out.println("DEBUG: Discovery service already running");
        }
    }

    /**
     * Load configuration from SocketService
     */
    private void loadConfiguration() {
        posNameField.setText(socketService.getConfig().getPosName());
        serverPortField.setText(String.valueOf(socketService.getConfig().getServerPort()));
        updateServerStatus();
        refreshActiveClientsTable();
    }

    /**
     * Toggle server on/off
     */
    private void toggleServer() {
        if (socketService.isServerRunning()) {
            System.out.println("DEBUG: Stopping server");
            socketService.stopServer();
            serverToggleButton.setText("Start Server");
            serverToggleButton.setBackground(new Color(40, 167, 69));  // Green
        } else {
            try {
                int port = Integer.parseInt(serverPortField.getText().trim());
                System.out.println("DEBUG: Attempting to start server on port " + port);

                if (socketService.startServer(port)) {
                    serverToggleButton.setText("Stop Server");
                    serverToggleButton.setBackground(new Color(220, 53, 69));  // Red

                    // Save POS name
                    String posName = posNameField.getText().trim();
                    socketService.getConfig().setPosName(posName);
                    socketService.getConfig().save("config/socket-config.json");

                    System.out.println("DEBUG: Server started successfully on port " + port);
                    System.out.println("DEBUG: POS Name: " + (posName.isEmpty() ? "Not set" : posName));
                } else {
                    System.out.println("DEBUG: Failed to start server");
                    showErrorDialog(
                            "Server Start Failed",
                            "Failed to start server on port " + port,
                            "Possible causes:<br>" +
                            "• Port is already in use by another application<br>" +
                            "• Try a different port (e.g., 9001, 9002)<br><br>" +
                            "Check the console for detailed error messages."
                    );
                }
            } catch (NumberFormatException e) {
                showErrorDialog(
                        "Error",
                        "Invalid Port Number",
                        "Please enter a valid port number (e.g., 9000, 9001)."
                );
            }
        }
        updateServerStatus();
    }

    /**
     * Update server status label
     */
    private void updateServerStatus() {
        if (socketService.isServerRunning()) {
            serverStatusLabel.setText("● Broadcasting on port " + socketService.getConfig().getServerPort());
            serverStatusLabel.setForeground(new Color(40, 167, 69));
        } else {
            serverStatusLabel.setText("○ Server Stopped");
            serverStatusLabel.setForeground(Color.RED);
        }
    }

    /**
     * Refresh POS systems table from discovered systems
     */
    private void refreshPosSystemsTable() {
        System.out.println("DEBUG: Refreshing POS systems table");
        tableModel.setRowCount(0);
        rowMap.clear();

        int count = 0;
        for (PosSystemInfo sysInfo : socketService.getDiscoveredSystems()) {
            System.out.println("DEBUG: Found POS - " + sysInfo.getDisplayName() + " at " + sysInfo.getIpAddress() + ":" + sysInfo.getPort());
            addOrUpdatePosSystem(sysInfo);
            count++;
        }
        System.out.println("DEBUG: Total discovered POS systems: " + count);

        if (count == 0) {
            System.out.println("DEBUG: No POS systems discovered. Make sure:");
            System.out.println("  1. Other POS has server started");
            System.out.println("  2. Both POS are on the same network");
            System.out.println("  3. Firewall is not blocking UDP port 9999");
            System.out.println("  4. Wait 5-10 seconds after starting server, then click Refresh again");
        }
    }

    /**
     * Add or update a POS system in the table
     */
    private void addOrUpdatePosSystem(PosSystemInfo sysInfo) {
        String identifier = sysInfo.getIdentifier();
        Integer rowIndex = rowMap.get(identifier);

        String status = sysInfo.isConnected() ? "Connected" : "Available";
        String lastLog = sysInfo.getLastLogTimestamp() != null ?
                sysInfo.getLastLogTimestamp().format(TIME_FORMATTER) : "N/A";
        String action = sysInfo.isConnected() ? "Disconnect" : "Connect";

        if (rowIndex == null) {
            // Add new row
            tableModel.addRow(new Object[]{
                    sysInfo.getDisplayName(),
                    sysInfo.getIpAddress(),
                    sysInfo.getPort(),
                    status,
                    lastLog,
                    action
            });
            rowMap.put(identifier, tableModel.getRowCount() - 1);
        } else {
            // Update existing row
            tableModel.setValueAt(sysInfo.getDisplayName(), rowIndex, 0);
            tableModel.setValueAt(status, rowIndex, 3);
            tableModel.setValueAt(lastLog, rowIndex, 4);
            tableModel.setValueAt(action, rowIndex, 5);
        }
    }

    /**
     * Update connection status in table
     */
    private void updateConnectionStatus(String identifier, boolean connected) {
        Integer rowIndex = rowMap.get(identifier);
        if (rowIndex != null) {
            String status = connected ? "Connected" : "Available";
            String action = connected ? "Disconnect" : "Connect";
            tableModel.setValueAt(status, rowIndex, 3);
            tableModel.setValueAt(action, rowIndex, 5);
        }
    }

    /**
     * Refresh active clients table with current connected clients
     */
    private void refreshActiveClientsTable() {
        activeClientsTableModel.setRowCount(0);

        java.util.List<org.possystem.socket.SocketService.ConnectedClientInfo> clients =
            socketService.getConnectedClientsList();

        int activeCount = 0;
        for (org.possystem.socket.SocketService.ConnectedClientInfo client : clients) {
            String ipAddress = client.getIpAddress();
            String connectedAt = client.getConnectedAt().format(TIME_FORMATTER);
            String duration = calculateDuration(client);
            String status = client.isConnected() ? "Connected" : "Disconnected";

            if (client.isConnected()) {
                activeCount++;
            }

            activeClientsTableModel.addRow(new Object[]{
                ipAddress,
                connectedAt,
                duration,
                status
            });
        }

        activeClientsCountLabel.setText("Total: " + activeCount + " active client" + (activeCount == 1 ? "" : "s") +
                                       " (" + clients.size() + " total)");
    }

    /**
     * Calculate duration for client connection
     */
    private String calculateDuration(org.possystem.socket.SocketService.ConnectedClientInfo client) {
        java.time.LocalDateTime startTime = client.getConnectedAt();
        java.time.LocalDateTime endTime;

        if (client.isConnected()) {
            // Still connected - calculate duration until now
            endTime = java.time.LocalDateTime.now();
        } else {
            // Disconnected - calculate duration until disconnection
            endTime = client.getDisconnectedAt() != null ? client.getDisconnectedAt() : startTime;
        }

        java.time.Duration duration = java.time.Duration.between(startTime, endTime);
        long seconds = Math.abs(duration.getSeconds());

        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    /**
     * Show dialog to manually add a POS connection
     */
    private void showAddManualDialog() {
        // Create custom dialog for better positioning control
        JDialog addManualDialog = new JDialog(this, "Add Manual Connection", Dialog.ModalityType.APPLICATION_MODAL);
        addManualDialog.setUndecorated(true);
        addManualDialog.setResizable(false);
        addManualDialog.setLayout(new BorderLayout(0, 0));
        addManualDialog.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 2));

        // Header panel with title and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(60, 63, 65));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        JLabel titleLabel = new JLabel("Add Manual Connection");
        titleLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        titleLabel.setForeground(Color.WHITE);

        JButton closeBtn = createCloseButton();
        closeBtn.addActionListener(e -> addManualDialog.dispose());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        // Make dialog draggable by header
        final Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                addManualDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
            }
        });

        // Main content panel
        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);

        // POS Name field with double-click for keyboard
        JTextField posNameField = new JTextField();
        posNameField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        posNameField.setToolTipText("Double-click for on-screen keyboard");
        posNameField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openAlphanumericKeyboard(posNameField, "Remote POS Name");
                }
            }
        });

        // IP Address field with double-click for keypad
        JTextField ipField = new JTextField();
        ipField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        ipField.setToolTipText("Double-click for on-screen keypad");
        ipField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openNumericKeypad(ipField, "IP Address", true);
                }
            }
        });

        // Port field with double-click for keypad
        JTextField portField = new JTextField("9000");
        portField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        portField.setToolTipText("Double-click for on-screen keypad");
        portField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openNumericKeypad(portField, "Port", false);
                }
            }
        });

        JLabel posNameLabel = new JLabel("POS Name:");
        posNameLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel ipLabel = new JLabel("IP Address:");
        ipLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));

        contentPanel.add(posNameLabel);
        contentPanel.add(posNameField);
        contentPanel.add(ipLabel);
        contentPanel.add(ipField);
        contentPanel.add(portLabel);
        contentPanel.add(portField);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        final boolean[] confirmed = {false};

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setBackground(new Color(40, 167, 69));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        applyRoundedStyle(okButton);
        okButton.addActionListener(e -> {
            confirmed[0] = true;
            addManualDialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        applyRoundedStyle(cancelButton);
        cancelButton.addActionListener(e -> addManualDialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        addManualDialog.add(headerPanel, BorderLayout.NORTH);
        addManualDialog.add(contentPanel, BorderLayout.CENTER);
        addManualDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Size and position at upper center of screen
        addManualDialog.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = addManualDialog.getWidth();
        int dialogHeight = addManualDialog.getHeight();

        // Position at upper center (20% from top)
        int x = (screenSize.width - dialogWidth) / 2;
        int y = (int) (screenSize.height * 0.20); // 20% from top

        addManualDialog.setLocation(x, y);
        addManualDialog.setVisible(true);

        // Process result after dialog closes
        int result = confirmed[0] ? JOptionPane.OK_OPTION : JOptionPane.CANCEL_OPTION;

        if (result == JOptionPane.OK_OPTION) {
            try {
                String posName = posNameField.getText().trim();
                String ip = ipField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());

                System.out.println("DEBUG: Attempting to connect to " + posName + " at " + ip + ":" + port);

                if (socketService.connectToRemotePOS(ip, port, posName)) {
                    // Save connection
                    socketService.getConfig().getSavedConnections().add(
                            new org.possystem.socket.SocketConfig.SavedConnection(posName, ip, port)
                    );
                    socketService.getConfig().save("config/socket-config.json");

                    System.out.println("DEBUG: Successfully connected to " + posName);
                    showSuccessDialog(
                            "Success",
                            "Connection Successful",
                            "Connected to " + posName + " (" + ip + ":" + port + ")"
                    );

                    // Refresh table to show connection
                    refreshPosSystemsTable();
                } else {
                    System.out.println("DEBUG: Failed to connect to " + posName);
                    showErrorDialog(
                            "Connection Failed",
                            "Connection Failed",
                            "Failed to connect to <b>" + ip + ":" + port + "</b><br><br>" +
                            "Possible causes:<br>" +
                            "• Remote POS server is not started<br>" +
                            "• Wrong IP address or port number<br>" +
                            "• Firewall blocking the connection<br>" +
                            "• Remote POS is not reachable<br><br>" +
                            "Check the console for detailed error messages."
                    );
                }
            } catch (NumberFormatException e) {
                showErrorDialog(
                        "Error",
                        "Invalid Port Number",
                        "Please enter a valid port number (e.g., 9000, 9001)."
                );
            }
        }
    }

    /**
     * Append journal entry to viewer with color
     */
    private void appendJournalEntry(RemoteJournalEntry entry) {
        String displayLine = entry.toDisplayString() + "\n";
        journalViewer.append(displayLine);

        // Auto-scroll to bottom
        journalViewer.setCaretPosition(journalViewer.getDocument().getLength());
    }

    /**
     * Export journal to file
     */
    private void exportJournal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Journal");
        fileChooser.setSelectedFile(new java.io.File("exported-journal.log"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.nio.file.Files.writeString(
                        fileChooser.getSelectedFile().toPath(),
                        journalViewer.getText()
                );
                showSuccessDialog(
                        "Success",
                        "Export Successful",
                        "Journal exported successfully to:<br>" + fileChooser.getSelectedFile().getName()
                );
            } catch (Exception e) {
                showErrorDialog(
                        "Error",
                        "Export Failed",
                        "Failed to export journal:<br>" + e.getMessage()
                );
            }
        }
    }

    /**
     * Toggle pinned journal viewer window
     */
    private void togglePinnedWindow() {
        System.out.println("DEBUG: togglePinnedWindow called. Current state: pinnedWindow=" + pinnedWindow + ", visible=" + (pinnedWindow != null && pinnedWindow.isVisible()));

        if (pinnedWindow == null || !pinnedWindow.isVisible()) {
            // Create and show pinned window
            System.out.println("DEBUG: Creating new PinnedJournalViewerWindow...");
            pinnedWindow = new PinnedJournalViewerWindow(parentFrame, socketService);

            // Position pinned window so its bottom aligns with the bottom of the Current Sale table
            // (just before the subtotal section)
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int windowHeight = pinnedWindow.getHeight();

            // Current Sale totals panel height estimation:
            // - 3 labels: subtotal (18pt ~25px), tax (18pt ~25px), total (22pt ~30px)
            // - 2 gaps of 5px
            // - Top/bottom padding: 10px each
            // Total: ~110px
            int totalsHeight = 110;

            // Calculate Y position so bottom of window aligns with bottom of table
            int yPosition = screenSize.height - totalsHeight - windowHeight;

            // X position: slightly to the left (at the edge of screen or with small padding)
            int xPosition = 10; // 10px padding from left edge

            pinnedWindow.setLocation(xPosition, yPosition);
            System.out.println("DEBUG: Set pinnedWindow location to: (" + xPosition + ", " + yPosition + ") - bottom aligns with Current Sale table");

            pinnedWindow.setVisible(true);
            System.out.println("DEBUG: Called setVisible(true) on pinnedWindow. IsVisible: " + pinnedWindow.isVisible());

            // Ensure Socket Configuration Dialog stays on top of the pinned window
            this.toFront();
            System.out.println("DEBUG: Brought Socket Configuration Dialog to front");

            System.out.println("DEBUG: Pinned Journal Viewer opened aligned with Current Sale table");
        } else {
            // Close existing pinned window
            System.out.println("DEBUG: Hiding existing pinned window");
            pinnedWindow.setVisible(false);
            System.out.println("DEBUG: Pinned Journal Viewer hidden");
        }
    }

    /**
     * Get hostname
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Get local IP address (the same IP that other POS systems will see)
     * This uses the same method as UDP discovery - creates a connection to determine
     * which network interface is actually used for external communication.
     */
    private String getLocalIPAddress() {
        try {
            // Create a temporary socket to determine which IP is used for network communication
            // We don't actually need to connect - just creating the socket shows us the route
            try (DatagramSocket socket = new DatagramSocket()) {
                // Connect to a remote address (Google DNS) to determine local IP
                // Note: This doesn't actually send data, just determines the route
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();

                // Verify it's not localhost
                if (!ip.startsWith("127.")) {
                    return ip;
                }
            }

            // Fallback: try to get from InetAddress
            InetAddress localHost = InetAddress.getLocalHost();
            String ipAddress = localHost.getHostAddress();

            if (!ipAddress.startsWith("127.")) {
                return ipAddress;
            }

            // Last resort: iterate through network interfaces
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                java.net.NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();

                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                java.util.Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }

            return ipAddress;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Apply rounded button style (matches search engine keyboard)
     */
    private void applyRoundedStyle(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12;

                Color buttonColor = btn.getBackground();
                Color displayColor;
                Color textColor;

                if (btn.isEnabled()) {
                    displayColor = buttonColor;
                    textColor = btn.getForeground();
                } else {
                    displayColor = new Color(
                        (int)(buttonColor.getRed() * 0.5),
                        (int)(buttonColor.getGreen() * 0.5),
                        (int)(buttonColor.getBlue() * 0.5)
                    );
                    textColor = new Color(180, 180, 180);
                }

                // Fill button background
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw darker outline border (like search engine keyboard)
                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw button text
                String text = btn.getText();
                FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                int textWidth = fm.stringWidth(text);
                int x = (btn.getWidth() - textWidth) / 2;
                int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setFont(btn.getFont());
                g2d.setColor(textColor);
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        });
    }

    private void applyRoundedStyleWithBlackOutline(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12;

                Color buttonColor = btn.getBackground();
                Color displayColor;
                Color textColor;

                if (btn.isEnabled()) {
                    displayColor = buttonColor;
                    textColor = btn.getForeground();
                } else {
                    displayColor = new Color(
                        (int)(buttonColor.getRed() * 0.5),
                        (int)(buttonColor.getGreen() * 0.5),
                        (int)(buttonColor.getBlue() * 0.5)
                    );
                    textColor = new Color(180, 180, 180);
                }

                // Fill button background
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw black outline border
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw button text
                String text = btn.getText();
                FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                int textWidth = fm.stringWidth(text);
                int x = (btn.getWidth() - textWidth) / 2;
                int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setFont(btn.getFont());
                g2d.setColor(textColor);
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        });
    }

    /**
     * Open alphanumeric QWERTY keyboard for text input
     */
    private void openAlphanumericKeyboard(JTextField targetField, String title) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(800, screenSize.width - 100);
        int dialogHeight = Math.min(500, screenSize.height - 100);

        JDialog keyboardDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        keyboardDialog.setUndecorated(true);
        keyboardDialog.setResizable(false);
        keyboardDialog.setSize(dialogWidth, dialogHeight);

        // Position at lower center of Socket Configuration dialog
        int parentX = getX();
        int parentY = getY();
        int parentWidth = getWidth();
        int parentHeight = getHeight();

        int keyboardX = parentX + (parentWidth - dialogWidth) / 2;
        int keyboardY = parentY + parentHeight - dialogHeight - 20; // 20px padding from bottom

        keyboardDialog.setLocation(keyboardX, keyboardY);
        keyboardDialog.setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Close button
        JButton closeButton = createCloseButton();
        closeButton.addActionListener(e -> keyboardDialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Drag functionality
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    keyboardDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        keyboardDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.setBackground(Color.WHITE);

        // QWERTY Keyboard (no input field - directly updates target field)
        JPanel keyboardPanel = createQWERTYKeyboard(targetField, keyboardDialog);
        keyboardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keyboardPanel);

        keyboardDialog.add(centerPanel, BorderLayout.CENTER);

        // Select all text in the actual target field when keyboard opens
        SwingUtilities.invokeLater(() -> {
            targetField.selectAll();
            targetField.requestFocus();
        });

        keyboardDialog.setVisible(true);
    }

    /**
     * Create QWERTY keyboard panel
     */
    private JPanel createQWERTYKeyboard(JTextField targetField, JDialog dialog) {
        JPanel keyboardPanel = new JPanel(new GridBagLayout());
        keyboardPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // State for uppercase/lowercase toggle
        final boolean[] isUpperCase = {true}; // Start with uppercase for first letter
        final boolean[] isFirstChar = {true}; // Track if this is the first character
        final boolean[] shiftPressed = {false}; // Track if shift was manually pressed (for one letter only)

        // Track all letter buttons for case toggling
        java.util.List<JButton> letterButtons = new java.util.ArrayList<>();

        // Track shift button reference for updating its appearance
        final JButton[] shiftButton = {null};

        // Row 0: Numbers
        String[] row0 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        gbc.gridy = 0;
        for (int i = 0; i < row0.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createKeyboardKey(row0[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            keyboardPanel.add(key, gbc);
        }

        // Row 1: QWERTYUIOP (start uppercase)
        String[] row1 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
        gbc.gridy = 1;
        for (int i = 0; i < row1.length; i++) {
            gbc.gridx = i;
            JButton key = createKeyboardKey(row1[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            letterButtons.add(key);
            keyboardPanel.add(key, gbc);
        }

        // Row 2: ASDFGHJKL (start uppercase)
        String[] row2 = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
        gbc.gridy = 2;
        for (int i = 0; i < row2.length; i++) {
            gbc.gridx = i;
            JButton key = createKeyboardKey(row2[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            letterButtons.add(key);
            keyboardPanel.add(key, gbc);
        }

        // Backspace
        gbc.gridx = 9;
        JButton backspaceBtn = createKeyboardKey("←", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        backspaceBtn.setForeground(Color.WHITE);
        applyRoundedStyle(backspaceBtn);
        keyboardPanel.add(backspaceBtn, gbc);

        // Row 3: Shift + ZXCVBNM
        gbc.gridy = 3;

        // Shift button (left side)
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JButton shiftBtn = new JButton("ABC"); // Start with uppercase indicator
        shiftBtn.setFont(new Font("Arial", Font.BOLD, Math.round(14 * fontScale)));
        shiftBtn.setFocusPainted(false);
        // Start with uppercase appearance (gray background for first letter)
        shiftBtn.setBackground(new Color(108, 117, 125)); // Gray when active
        shiftBtn.setForeground(Color.WHITE);
        applyRoundedStyle(shiftBtn); // Apply rounded style for consistent visibility
        shiftBtn.addActionListener(e -> {
            // Toggle shift (one letter only, not caps lock)
            isUpperCase[0] = !isUpperCase[0];
            shiftPressed[0] = isUpperCase[0]; // Mark that shift was manually pressed
            isFirstChar[0] = false; // Manual toggle disables auto-lowercase for first char

            // Update all letter button labels
            for (JButton btn : letterButtons) {
                String currentText = btn.getText();
                if (currentText.length() == 1 && Character.isLetter(currentText.charAt(0))) {
                    if (isUpperCase[0]) {
                        btn.setText(currentText.toUpperCase());
                    } else {
                        btn.setText(currentText.toLowerCase());
                    }
                }
            }

            // Update shift button appearance and label
            if (isUpperCase[0]) {
                shiftBtn.setText("ABC"); // Uppercase indicator
                shiftBtn.setBackground(new Color(108, 117, 125)); // Gray when active
                shiftBtn.setForeground(Color.WHITE);
            } else {
                shiftBtn.setText("abc"); // Lowercase indicator
                shiftBtn.setBackground(new Color(248, 249, 250)); // Light gray when inactive
                shiftBtn.setForeground(Color.BLACK);
            }
            shiftBtn.repaint();
        });
        shiftButton[0] = shiftBtn; // Store reference
        keyboardPanel.add(shiftBtn, gbc);

        // ZXCVBNM letters (start uppercase)
        String[] row3 = {"Z", "X", "C", "V", "B", "N", "M"};
        for (int i = 0; i < row3.length; i++) {
            gbc.gridx = i + 1;
            JButton key = createKeyboardKey(row3[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            letterButtons.add(key);
            keyboardPanel.add(key, gbc);
        }

        // Clear button
        gbc.gridx = 8;
        gbc.gridwidth = 2;
        JButton clearBtn = createKeyboardKey("Clear", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        applyRoundedStyle(clearBtn);
        keyboardPanel.add(clearBtn, gbc);

        // Row 4: Space + Enter
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 7;
        JButton spaceBtn = createKeyboardKey("Space", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        keyboardPanel.add(spaceBtn, gbc);

        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton enterBtn = createKeyboardKey("Enter", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        enterBtn.setBackground(new Color(40, 167, 69)); // Green
        enterBtn.setForeground(Color.WHITE);
        applyRoundedStyle(enterBtn);
        keyboardPanel.add(enterBtn, gbc);

        return keyboardPanel;
    }

    /**
     * Create keyboard key button
     */
    private JButton createKeyboardKey(String key, JTextField targetField, JDialog dialog,
                                      boolean[] isUpperCase, boolean[] isFirstChar,
                                      java.util.List<JButton> letterButtons, JButton[] shiftButton, boolean[] shiftPressed) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, Math.round(16 * fontScale)));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250));
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = targetField.getText();
            String newText = currentText;

            // Check if text is selected (for replacement behavior)
            boolean hasSelection = targetField.getSelectionStart() != targetField.getSelectionEnd();

            if (key.equals("Clear")) {
                newText = "";
                // Reset to uppercase for first character after clear
                isFirstChar[0] = true;
                isUpperCase[0] = true;
                shiftPressed[0] = false;
                // Update all letter buttons to uppercase
                for (JButton btn : letterButtons) {
                    String btnText = btn.getText();
                    if (btnText.length() == 1 && Character.isLetter(btnText.charAt(0))) {
                        btn.setText(btnText.toUpperCase());
                    }
                }
                // Update shift button appearance
                if (shiftButton[0] != null) {
                    shiftButton[0].setText("ABC"); // Uppercase indicator
                    shiftButton[0].setBackground(new Color(108, 117, 125));
                    shiftButton[0].setForeground(Color.WHITE);
                    shiftButton[0].repaint();
                }
            } else if (key.equals("←")) {
                if (hasSelection) {
                    // If text is selected, delete the selection
                    int start = targetField.getSelectionStart();
                    int end = targetField.getSelectionEnd();
                    newText = currentText.substring(0, start) + currentText.substring(end);
                } else if (currentText.length() > 0) {
                    // Otherwise, delete last character
                    newText = currentText.substring(0, currentText.length() - 1);
                }
                // If we deleted everything, reset to uppercase for first character
                if (newText.isEmpty()) {
                    isFirstChar[0] = true;
                    isUpperCase[0] = true;
                    shiftPressed[0] = false;
                    // Update all letter buttons to uppercase
                    for (JButton btn : letterButtons) {
                        String btnText = btn.getText();
                        if (btnText.length() == 1 && Character.isLetter(btnText.charAt(0))) {
                            btn.setText(btnText.toUpperCase());
                        }
                    }
                    // Update shift button appearance
                    if (shiftButton[0] != null) {
                        shiftButton[0].setText("ABC"); // Uppercase indicator
                        shiftButton[0].setBackground(new Color(108, 117, 125));
                        shiftButton[0].setForeground(Color.WHITE);
                        shiftButton[0].repaint();
                    }
                }
            } else if (key.equals("Space")) {
                if (hasSelection) {
                    // Replace selection with space
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + " " + currentText.substring(targetField.getSelectionEnd());
                } else {
                    newText = currentText + " ";
                }
            } else if (key.equals("Enter")) {
                // Just close the dialog
                dialog.dispose();
                return;
            } else {
                // For letter or number keys, use current case from button text
                String keyText = keyButton.getText();

                if (hasSelection) {
                    // Replace selected text with the key
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + keyText + currentText.substring(targetField.getSelectionEnd());
                } else {
                    // Append to current text
                    newText = currentText + keyText;
                }

                // Auto-switch to lowercase after:
                // 1. First letter is typed (sentence capitalization)
                // 2. One letter is typed after shift was pressed (shift behavior, not caps lock)
                if (keyText.length() == 1 && Character.isLetter(keyText.charAt(0))) {
                    if (isFirstChar[0] || shiftPressed[0]) {
                        isFirstChar[0] = false;
                        isUpperCase[0] = false;
                        shiftPressed[0] = false; // Reset shift after one letter
                        // Update all letter buttons to lowercase
                        for (JButton btn : letterButtons) {
                            String btnText = btn.getText();
                            if (btnText.length() == 1 && Character.isLetter(btnText.charAt(0))) {
                                btn.setText(btnText.toLowerCase());
                            }
                        }
                        // Update shift button appearance
                        if (shiftButton[0] != null) {
                            shiftButton[0].setText("abc"); // Lowercase indicator
                            shiftButton[0].setBackground(new Color(248, 249, 250));
                            shiftButton[0].setForeground(Color.BLACK);
                            shiftButton[0].repaint();
                        }
                    }
                }
            }

            targetField.setText(newText);
        });

        return keyButton;
    }

    /**
     * Open numeric keypad for numeric input
     */
    private void openNumericKeypad(JTextField targetField, String title, boolean includeDot) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(450, screenSize.width - 100);
        int dialogHeight = Math.min(450, screenSize.height - 100);

        JDialog keypadDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        keypadDialog.setUndecorated(true);
        keypadDialog.setResizable(false);
        keypadDialog.setSize(dialogWidth, dialogHeight);

        // Position at lower center of Socket Configuration dialog
        int parentX = getX();
        int parentY = getY();
        int parentWidth = getWidth();
        int parentHeight = getHeight();

        int keypadX = parentX + (parentWidth - dialogWidth) / 2;
        int keypadY = parentY + parentHeight - dialogHeight - 20; // 20px padding from bottom

        keypadDialog.setLocation(keypadX, keypadY);
        keypadDialog.setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Close button
        JButton closeButton = createCloseButton();
        closeButton.addActionListener(e -> keypadDialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Drag functionality
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    keypadDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        keypadDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.setBackground(Color.WHITE);

        // Numeric Keypad (no input field - directly updates target field)
        JPanel keypadPanel = createNumericKeypad(targetField, keypadDialog, includeDot);
        keypadPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keypadPanel);

        keypadDialog.add(centerPanel, BorderLayout.CENTER);

        // Select all text in the actual target field when keypad opens
        SwingUtilities.invokeLater(() -> {
            targetField.selectAll();
            targetField.requestFocus();
        });

        keypadDialog.setVisible(true);
    }

    /**
     * Create numeric keypad panel
     */
    private JPanel createNumericKeypad(JTextField targetField, JDialog dialog, boolean includeDot) {
        JPanel keypadPanel = new JPanel(new GridBagLayout());
        keypadPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Row 1: 7, 8, 9, Backspace (spans 2 rows)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 1;
        keypadPanel.add(createKeypadButton("7", targetField, dialog, includeDot), gbc);

        gbc.gridx = 1;
        keypadPanel.add(createKeypadButton("8", targetField, dialog, includeDot), gbc);

        gbc.gridx = 2;
        keypadPanel.add(createKeypadButton("9", targetField, dialog, includeDot), gbc);

        gbc.gridx = 3; gbc.gridheight = 2;
        JButton backspaceBtn = createKeypadButton("←", targetField, dialog, includeDot);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        backspaceBtn.setForeground(Color.WHITE);
        applyRoundedStyle(backspaceBtn);
        keypadPanel.add(backspaceBtn, gbc);

        // Row 2: 4, 5, 6
        gbc.gridheight = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        keypadPanel.add(createKeypadButton("4", targetField, dialog, includeDot), gbc);

        gbc.gridx = 1;
        keypadPanel.add(createKeypadButton("5", targetField, dialog, includeDot), gbc);

        gbc.gridx = 2;
        keypadPanel.add(createKeypadButton("6", targetField, dialog, includeDot), gbc);

        // Row 3: 1, 2, 3, Enter (spans 2 rows)
        gbc.gridx = 0; gbc.gridy = 2;
        keypadPanel.add(createKeypadButton("1", targetField, dialog, includeDot), gbc);

        gbc.gridx = 1;
        keypadPanel.add(createKeypadButton("2", targetField, dialog, includeDot), gbc);

        gbc.gridx = 2;
        keypadPanel.add(createKeypadButton("3", targetField, dialog, includeDot), gbc);

        gbc.gridx = 3; gbc.gridheight = 2;
        JButton enterBtn = createKeypadButton("Enter", targetField, dialog, includeDot);
        enterBtn.setBackground(new Color(40, 167, 69)); // Green
        enterBtn.setForeground(Color.WHITE);
        applyRoundedStyle(enterBtn);
        keypadPanel.add(enterBtn, gbc);

        // Row 4: Clear, 0, . (if needed)
        gbc.gridheight = 1;
        gbc.gridx = 0; gbc.gridy = 3;
        JButton clearBtn = createKeypadButton("Clear", targetField, dialog, includeDot);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        applyRoundedStyle(clearBtn);
        keypadPanel.add(clearBtn, gbc);

        gbc.gridx = 1;
        keypadPanel.add(createKeypadButton("0", targetField, dialog, includeDot), gbc);

        gbc.gridx = 2;
        if (includeDot) {
            keypadPanel.add(createKeypadButton(".", targetField, dialog, includeDot), gbc);
        } else {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(Color.WHITE);
            keypadPanel.add(emptyPanel, gbc);
        }

        return keypadPanel;
    }

    /**
     * Create keypad button
     */
    private JButton createKeypadButton(String key, JTextField targetField, JDialog dialog, boolean allowDot) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, Math.round(18 * fontScale)));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250));
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = targetField.getText();
            String newText = currentText;

            // Check if text is selected (for replacement behavior)
            boolean hasSelection = targetField.getSelectionStart() != targetField.getSelectionEnd();

            if (key.equals("Clear")) {
                newText = "";
            } else if (key.equals("←")) {
                if (hasSelection) {
                    // If text is selected, delete the selection
                    int start = targetField.getSelectionStart();
                    int end = targetField.getSelectionEnd();
                    newText = currentText.substring(0, start) + currentText.substring(end);
                } else if (currentText.length() > 0) {
                    // Otherwise, delete last character
                    newText = currentText.substring(0, currentText.length() - 1);
                }
            } else if (key.equals(".")) {
                if (allowDot) {
                    if (hasSelection) {
                        // Replace selection with dot
                        int start = targetField.getSelectionStart();
                        newText = currentText.substring(0, start) + "." + currentText.substring(targetField.getSelectionEnd());
                    } else {
                        newText = currentText + ".";
                    }
                }
            } else if (key.equals("Enter")) {
                dialog.dispose();
                return;
            } else {
                // Number button (0-9)
                if (hasSelection) {
                    // Replace selected text with the number
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + key + currentText.substring(targetField.getSelectionEnd());
                } else {
                    // Append to current text
                    newText = currentText + key;
                }
            }

            targetField.setText(newText);
        });

        return keyButton;
    }

    /**
     * Create close button for dialog header
     */
    private JButton createCloseButton() {
        JButton closeButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(3));

                Boolean hovered = (Boolean) getClientProperty("hovered");
                if (hovered != null && hovered) {
                    g2.setColor(new Color(255, 100, 100));
                } else {
                    g2.setColor(new Color(220, 53, 69));
                }

                int padding = 10;
                int x1 = padding;
                int y1 = padding;
                int x2 = getWidth() - padding;
                int y2 = getHeight() - padding;

                g2.drawLine(x1, y1, x2, y2);
                g2.drawLine(x2, y1, x1, y2);

                g2.dispose();
            }
        };
        closeButton.setBackground(new Color(23, 162, 184));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(40, 40));
        closeButton.putClientProperty("hovered", false);
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                ((JButton)e.getSource()).putClientProperty("hovered", true);
                e.getComponent().repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                ((JButton)e.getSource()).putClientProperty("hovered", false);
                e.getComponent().repaint();
            }
        });
        return closeButton;
    }

    /**
     * Button renderer for table action column
     */
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, tableFontSize));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Connect" : value.toString());
            return this;
        }
    }

    /**
     * Button editor for table action column
     */
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean clicked;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Arial", Font.PLAIN, tableFontSize));
            button.addActionListener(e -> {
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            label = (value == null) ? "Connect" : value.toString();
            button.setText(label);
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (clicked) {
                String posName = (String) tableModel.getValueAt(row, 0);
                String ipAddress = (String) tableModel.getValueAt(row, 1);
                int port = (int) tableModel.getValueAt(row, 2);

                if ("Connect".equals(label)) {
                    socketService.connectToRemotePOS(ipAddress, port, posName);
                } else {
                    socketService.disconnectFromRemotePOS(ipAddress, port);
                }
            }
            clicked = false;
            return label;
        }
    }

    /**
     * Show personalized error dialog
     */
    private void showErrorDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        // Fixed width (same as before)
        int dialogWidth = (int) (screenSize.width * 0.25);

        JDialog errorDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        errorDialog.setUndecorated(true);
        errorDialog.setResizable(false);
        errorDialog.setLayout(new BorderLayout());

        // Header Panel with red error color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(220, 53, 69));  // Error red
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    errorDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        errorDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: left; width: " + (dialogWidth - 80) + "px;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        errorDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(220, 53, 69));  // Red to match error theme
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> errorDialog.dispose());

        buttonPanel.add(okButton);

        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Pack to fit content, then apply size constraints
        errorDialog.pack();

        // Ensure minimum size
        int minWidth = 400;
        int minHeight = 250;
        if (errorDialog.getWidth() < minWidth) {
            errorDialog.setSize(minWidth, errorDialog.getHeight());
        }
        if (errorDialog.getHeight() < minHeight) {
            errorDialog.setSize(errorDialog.getWidth(), minHeight);
        }

        // Ensure maximum size
        int maxHeight = (int) (screenSize.height * 0.6);
        if (errorDialog.getHeight() > maxHeight) {
            errorDialog.setSize(errorDialog.getWidth(), maxHeight);
        }

        errorDialog.setLocationRelativeTo(this);
        errorDialog.setVisible(true);
    }

    /**
     * Show personalized success dialog
     */
    private void showSuccessDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        // Fixed width (same as before)
        int dialogWidth = (int) (screenSize.width * 0.25);

        JDialog successDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        successDialog.setUndecorated(true);
        successDialog.setResizable(false);
        successDialog.setLayout(new BorderLayout());

        // Header Panel with green success color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(40, 167, 69));  // Success green
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    successDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        successDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: left; width: " + (dialogWidth - 80) + "px;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        successDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(40, 167, 69));  // Green to match success theme
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> successDialog.dispose());

        buttonPanel.add(okButton);

        successDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Pack to fit content, then apply size constraints
        successDialog.pack();

        // Ensure minimum size
        int minWidth = 400;
        int minHeight = 250;
        if (successDialog.getWidth() < minWidth) {
            successDialog.setSize(minWidth, successDialog.getHeight());
        }
        if (successDialog.getHeight() < minHeight) {
            successDialog.setSize(successDialog.getWidth(), minHeight);
        }

        // Ensure maximum size
        int maxHeight = (int) (screenSize.height * 0.6);
        if (successDialog.getHeight() > maxHeight) {
            successDialog.setSize(successDialog.getWidth(), maxHeight);
        }

        successDialog.setLocationRelativeTo(this);
        successDialog.setVisible(true);
    }

    /**
     * Show personalized info dialog
     */
    private void showInfoDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        // Fixed width (same as before)
        int dialogWidth = (int) (screenSize.width * 0.25);

        JDialog infoDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        infoDialog.setUndecorated(true);
        infoDialog.setResizable(false);
        infoDialog.setLayout(new BorderLayout());

        // Header Panel with blue info color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));  // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    infoDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        infoDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: left; width: " + (dialogWidth - 80) + "px;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        infoDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(40, 167, 69));  // Green
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> infoDialog.dispose());

        buttonPanel.add(okButton);

        infoDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Pack to fit content, then apply size constraints
        infoDialog.pack();

        // Ensure minimum size
        int minWidth = 400;
        int minHeight = 250;
        if (infoDialog.getWidth() < minWidth) {
            infoDialog.setSize(minWidth, infoDialog.getHeight());
        }
        if (infoDialog.getHeight() < minHeight) {
            infoDialog.setSize(infoDialog.getWidth(), minHeight);
        }

        // Ensure maximum size
        int maxHeight = (int) (screenSize.height * 0.6);
        if (infoDialog.getHeight() > maxHeight) {
            infoDialog.setSize(infoDialog.getWidth(), maxHeight);
        }

        infoDialog.setLocationRelativeTo(this);
        infoDialog.setVisible(true);
    }
}
