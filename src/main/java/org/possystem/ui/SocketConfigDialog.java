package org.possystem.ui;

import org.possystem.socket.PosSystemInfo;
import org.possystem.socket.RemoteJournalEntry;
import org.possystem.socket.SocketService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

    private final SocketService socketService;

    // This POS Configuration
    private JTextField posNameField;
    private JLabel hostnameLabel;
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

    public SocketConfigDialog(Frame parent, SocketService socketService) {
        super(parent, "Socket Configuration", Dialog.ModalityType.MODELESS);
        this.socketService = socketService;

        initializeDialog();
        createUI();
        wireListeners();
        loadConfiguration();
        refreshPosSystemsTable();
    }

    private void initializeDialog() {
        setSize(900, 700);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void createUI() {
        // Main panel with sections
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top section: This POS Configuration + Active Clients
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(createThisPOSPanel());
        topSection.add(Box.createVerticalStrut(10));
        topSection.add(createActiveClientsPanel());
        mainPanel.add(topSection, BorderLayout.NORTH);

        // Center: Split between POS Systems Table and Journal Viewer
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(createPosSystemsPanel());
        splitPane.setBottomComponent(createJournalViewerPanel());
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.4);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Create "This POS Configuration" panel
     */
    private JPanel createThisPOSPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("This POS Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // POS Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("POS Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        posNameField = new JTextField(20);
        posNameField.setToolTipText("Optional custom name for this POS");
        panel.add(posNameField, gbc);

        // Hostname (read-only)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Hostname:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String hostname = getHostname();
        hostnameLabel = new JLabel(hostname);
        hostnameLabel.setForeground(Color.GRAY);
        panel.add(hostnameLabel, gbc);

        // Server Port
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Server Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serverPortField = new JTextField(10);
        panel.add(serverPortField, gbc);

        // Server Status and Toggle Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Status:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverStatusLabel = new JLabel("Server Stopped");
        serverStatusLabel.setForeground(Color.RED);
        statusPanel.add(serverStatusLabel);

        serverToggleButton = new JButton("Start Server");
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
        panel.setBorder(BorderFactory.createTitledBorder("🔌 Active Clients Connected to This Server"));

        // Table
        String[] columnNames = {"IP Address", "Connected At", "Duration"};
        activeClientsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        activeClientsTable = new JTable(activeClientsTableModel);
        activeClientsTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(activeClientsTable);
        scrollPane.setPreferredSize(new Dimension(0, 100)); // Compact height
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status label
        activeClientsCountLabel = new JLabel("Total: 0 active clients");
        activeClientsCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(activeClientsCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create "Available POS Systems" panel with table
     */
    private JPanel createPosSystemsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Available POS Systems"));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshPosSystemsTable());
        toolbar.add(refreshButton);

        JButton addManualButton = new JButton("+ Add Manual");
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
        posSystemsTable.setRowHeight(30);
        posSystemsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        posSystemsTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(posSystemsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("Connected: 0");
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(statusBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create "Live Journal Viewer" panel
     */
    private JPanel createJournalViewerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Live Journal Viewer"));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> journalViewer.setText(""));
        toolbar.add(clearButton);

        JButton exportButton = new JButton("Export");
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
        } else {
            try {
                int port = Integer.parseInt(serverPortField.getText().trim());
                System.out.println("DEBUG: Attempting to start server on port " + port);

                if (socketService.startServer(port)) {
                    serverToggleButton.setText("Stop Server");

                    // Save POS name
                    String posName = posNameField.getText().trim();
                    socketService.getConfig().setPosName(posName);
                    socketService.getConfig().save("config/socket-config.json");

                    System.out.println("DEBUG: Server started successfully on port " + port);
                    System.out.println("DEBUG: POS Name: " + (posName.isEmpty() ? "Not set" : posName));
                } else {
                    System.out.println("DEBUG: Failed to start server");
                    JOptionPane.showMessageDialog(this,
                            "Failed to start server on port " + port + ".\n\n" +
                            "Possible causes:\n" +
                            "• Port is already in use by another application\n" +
                            "• Try a different port (e.g., 9001, 9002)\n\n" +
                            "Check the console for detailed error messages.",
                            "Server Start Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Invalid port number",
                        "Error", JOptionPane.ERROR_MESSAGE);
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

        for (org.possystem.socket.SocketService.ConnectedClientInfo client : clients) {
            String ipAddress = client.getIpAddress();
            String connectedAt = client.getConnectedAt().format(TIME_FORMATTER);
            String duration = calculateDuration(client.getConnectedAt());

            activeClientsTableModel.addRow(new Object[]{
                ipAddress,
                connectedAt,
                duration
            });
        }

        activeClientsCountLabel.setText("Total: " + clients.size() + " active client" + (clients.size() == 1 ? "" : "s"));
    }

    /**
     * Calculate duration since connection time
     */
    private String calculateDuration(java.time.LocalDateTime connectedAt) {
        java.time.Duration duration = java.time.Duration.between(connectedAt, java.time.LocalDateTime.now());
        long seconds = duration.getSeconds();

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
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField posNameField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField portField = new JTextField("9000");

        panel.add(new JLabel("POS Name:"));
        panel.add(posNameField);
        panel.add(new JLabel("IP Address:"));
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Manual Connection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

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
                    JOptionPane.showMessageDialog(this,
                            "Connected to " + posName + " (" + ip + ":" + port + ")",
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    // Refresh table to show connection
                    refreshPosSystemsTable();
                } else {
                    System.out.println("DEBUG: Failed to connect to " + posName);
                    JOptionPane.showMessageDialog(this,
                            "Failed to connect to " + ip + ":" + port + "\n\n" +
                            "Possible causes:\n" +
                            "• Remote POS server is not started\n" +
                            "• Wrong IP address or port number\n" +
                            "• Firewall blocking the connection\n" +
                            "• Remote POS is not reachable\n\n" +
                            "Check the console for detailed error messages.",
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Invalid port number",
                        "Error", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this,
                        "Journal exported successfully",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export journal: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
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
     * Button renderer for table action column
     */
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
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
}
