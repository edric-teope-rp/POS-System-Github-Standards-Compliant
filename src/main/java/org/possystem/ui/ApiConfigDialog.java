package org.possystem.ui;

import org.possystem.config.ConfigManager;
import org.possystem.service.DiscountApiClient;

import javax.swing.*;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * API Configuration Dialog - Configure Discount Engine API URL
 */
public class ApiConfigDialog extends JDialog {

    private final Frame parentFrame;
    private final ConfigManager configManager;
    private final DiscountApiClient discountApiClient;
    private final org.possystem.service.AuthService authService;

    private JTextField apiUrlField;
    private JLabel statusLabel;
    private JButton testConnectionButton;
    private JButton saveButton;
    private JButton cancelButton;

    public ApiConfigDialog(Frame parent, ConfigManager configManager, DiscountApiClient discountApiClient, org.possystem.service.AuthService authService) {
        super(parent, "API Configuration", Dialog.ModalityType.APPLICATION_MODAL);

        this.parentFrame = parent;
        this.configManager = configManager;
        this.discountApiClient = discountApiClient;
        this.authService = authService;

        initializeComponents();
        layoutComponents();
        loadCurrentSettings();

        setUndecorated(true);
        setResizable(false);
        setSize(600, 450);
        setMinimumSize(new Dimension(500, 400));

        // Center on parent window
        setLocationRelativeTo(parent);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        // API URL Field
        apiUrlField = new JTextField(40);
        apiUrlField.setFont(new Font("Arial", Font.PLAIN, 14));
        apiUrlField.setToolTipText("Double-click for on-screen keyboard");
        apiUrlField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click opens on-screen keyboard
                    openAlphanumericKeyboard(apiUrlField, "API URL");
                }
            }
        });

        // Status Label
        statusLabel = new JLabel("● Not tested");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setForeground(Color.GRAY);

        // Test Connection Button
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.setFont(new Font("Arial", Font.BOLD, 14));
        testConnectionButton.setBackground(new Color(0, 123, 255));
        testConnectionButton.setForeground(Color.WHITE);
        testConnectionButton.setFocusPainted(false);
        testConnectionButton.setFocusable(false);
        testConnectionButton.setPreferredSize(new Dimension(150, 40));
        testConnectionButton.addActionListener(e -> testConnection());
        applyRoundedStyle(testConnectionButton);

        // Save Button
        saveButton = new JButton("Save");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setBackground(new Color(40, 167, 69));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setFocusable(false);
        saveButton.setPreferredSize(new Dimension(100, 40));
        saveButton.addActionListener(e -> saveSettings());
        applyRoundedStyle(saveButton);

        // Cancel Button
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setFocusable(false);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dispose());
        applyRoundedStyle(cancelButton);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Left side: Title with username
        String username = authService.getCurrentUsername();
        String titleText = "API Configuration - Logged in as: " + (username != null ? username : "Unknown");
        JLabel headerLabel = new JLabel(titleText);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Right panel for logout and close buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(new Color(70, 130, 180));

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 12));
        logoutButton.setBackground(new Color(220, 53, 69)); // Red
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(110, 35));
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

        add(headerPanel, BorderLayout.NORTH);

        // Center Panel with form
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        centerPanel.setBackground(Color.WHITE);

        // API URL Section
        JLabel urlLabel = new JLabel("Discount Engine API URL:");
        urlLabel.setFont(new Font("Arial", Font.BOLD, 14));
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(urlLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        apiUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        apiUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        centerPanel.add(apiUrlField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Examples
        JLabel examplesLabel = new JLabel("<html><i>Examples:</i></html>");
        examplesLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        examplesLabel.setForeground(Color.GRAY);
        examplesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(examplesLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 3)));

        JLabel example1 = new JLabel("  • http://localhost:8080/api/discounts");
        example1.setFont(new Font("Monospaced", Font.PLAIN, 11));
        example1.setForeground(Color.GRAY);
        example1.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(example1);

        JLabel example2 = new JLabel("  • https://api.example.com/discounts");
        example2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        example2.setForeground(Color.GRAY);
        example2.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(example2);

        JLabel example3 = new JLabel("  • https://your-aws-instance.com/api/discounts");
        example3.setFont(new Font("Monospaced", Font.PLAIN, 11));
        example3.setForeground(Color.GRAY);
        example3.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(example3);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Note about API
        JLabel noteLabel = new JLabel("<html><i>Note: The Discount Engine API must be running for test to succeed.</i></html>");
        noteLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        noteLabel.setForeground(new Color(150, 100, 0)); // Brownish color for note
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(noteLabel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Status Section
        JLabel statusTitleLabel = new JLabel("Connection Status:");
        statusTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(statusTitleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Test Connection Button
        testConnectionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        testConnectionButton.setMaximumSize(new Dimension(150, 40));
        centerPanel.add(testConnectionButton);

        centerPanel.add(Box.createVerticalGlue());

        add(centerPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadCurrentSettings() {
        String currentUrl = configManager.getDiscountApiUrl();
        apiUrlField.setText(currentUrl);
    }

    private void testConnection() {
        String urlString = apiUrlField.getText().trim();

        if (urlString.isEmpty()) {
            updateStatus("Please enter a URL", Color.RED);
            return;
        }

        // Validate URL format
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            updateStatus("✗ Invalid URL format (must start with http:// or https://)", Color.RED);
            return;
        }

        // Disable button and show testing status
        testConnectionButton.setEnabled(false);
        updateStatus("● Testing connection...", Color.ORANGE);

        // Run test in background thread to avoid blocking UI
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String errorMessage = "";

            @Override
            protected String doInBackground() {
                try {
                    success = testApiConnection(urlString);
                    return success ? "success" : "failed";
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return "error";
                }
            }

            @Override
            protected void done() {
                if (success) {
                    updateStatus("✓ Connection successful - API is responding", new Color(40, 167, 69));
                } else {
                    updateStatus("✗ Cannot connect - Make sure Discount API is running at this URL", Color.RED);
                }
                testConnectionButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    private boolean testApiConnection(String urlString) {
        try {
            // Test with a real endpoint that we know exists: calculate-senior
            // This is a GET request that should return a valid response
            String testUrl = urlString + "/calculate-senior?cartSubtotal=0";
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // Accept any 2xx response (success) or 4xx (means server is running, just needs proper request)
            boolean success = responseCode >= 200 && responseCode < 500;

            if (success) {
                System.out.println("Connection test successful: HTTP " + responseCode);
            } else {
                System.err.println("Connection test failed: HTTP " + responseCode);
            }

            return success;

        } catch (java.net.ConnectException e) {
            System.err.println("Connection test failed: Cannot connect to server - " + e.getMessage());
            return false;
        } catch (java.net.UnknownHostException e) {
            System.err.println("Connection test failed: Unknown host - " + e.getMessage());
            return false;
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Connection test failed: Connection timeout - " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Connection test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private void saveSettings() {
        String urlString = apiUrlField.getText().trim();

        if (urlString.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a URL",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate URL format
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            JOptionPane.showMessageDialog(this,
                "Invalid URL format. Must start with http:// or https://",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save to config file
        configManager.setDiscountApiUrl(urlString);

        // Update the DiscountApiClient instance
        discountApiClient.setBaseUrl(urlString);

        // Show success toast
        ToastNotification.showToast(parentFrame, "Configuration Saved", "API URL updated successfully");

        // Close the dialog
        dispose();
    }

    private void applyRoundedStyle(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);

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

                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

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

        // Position at lower center of screen
        int keyboardX = (screenSize.width - dialogWidth) / 2;  // Center horizontally
        int keyboardY = screenSize.height - dialogHeight - 50; // 50px padding from bottom

        keyboardDialog.setLocation(keyboardX, keyboardY);
        keyboardDialog.setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
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
        final boolean[] isUpperCase = {false}; // Start lowercase for URLs
        final boolean[] isFirstChar = {false}; // Not capitalizing for URLs
        final boolean[] shiftPressed = {false};

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

        // Row 1: QWERTYUIOP (lowercase for URLs)
        String[] row1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        gbc.gridy = 1;
        for (int i = 0; i < row1.length; i++) {
            gbc.gridx = i;
            JButton key = createKeyboardKey(row1[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            letterButtons.add(key);
            keyboardPanel.add(key, gbc);
        }

        // Row 2: ASDFGHJKL (lowercase)
        String[] row2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
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

        // Row 3: Shift + ZXCVBNM + Special keys
        gbc.gridy = 3;

        // Shift button (left side)
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JButton shiftBtn = new JButton("abc"); // Start lowercase
        shiftBtn.setFont(new Font("Arial", Font.BOLD, 14));
        shiftBtn.setFocusPainted(false);
        // Start with lowercase appearance
        shiftBtn.setBackground(new Color(248, 249, 250)); // Light gray when inactive
        shiftBtn.setForeground(Color.BLACK);
        applyRoundedStyle(shiftBtn);
        shiftBtn.addActionListener(e -> {
            // Toggle shift
            isUpperCase[0] = !isUpperCase[0];
            shiftPressed[0] = isUpperCase[0];
            isFirstChar[0] = false;

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
        shiftButton[0] = shiftBtn;
        keyboardPanel.add(shiftBtn, gbc);

        // ZXCVBNM letters (lowercase)
        String[] row3 = {"z", "x", "c", "v", "b", "n", "m"};
        for (int i = 0; i < row3.length; i++) {
            gbc.gridx = i + 1;
            JButton key = createKeyboardKey(row3[i], targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
            letterButtons.add(key);
            keyboardPanel.add(key, gbc);
        }

        // Special characters for URLs
        gbc.gridx = 8;
        JButton colonBtn = createKeyboardKey(":", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        keyboardPanel.add(colonBtn, gbc);

        gbc.gridx = 9;
        JButton slashBtn = createKeyboardKey("/", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        keyboardPanel.add(slashBtn, gbc);

        // Row 4: Space + dot + Clear + Enter
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        JButton spaceBtn = createKeyboardKey("Space", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        keyboardPanel.add(spaceBtn, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        JButton dotBtn = createKeyboardKey(".", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        keyboardPanel.add(dotBtn, gbc);

        gbc.gridx = 5;
        gbc.gridwidth = 2;
        JButton clearBtn = createKeyboardKey("Clear", targetField, dialog, isUpperCase, isFirstChar, letterButtons, shiftButton, shiftPressed);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        applyRoundedStyle(clearBtn);
        keyboardPanel.add(clearBtn, gbc);

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
        keyButton.setFont(new Font("Arial", Font.BOLD, 16));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250));
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = targetField.getText();
            String newText = currentText;

            // Check if text is selected
            boolean hasSelection = targetField.getSelectionStart() != targetField.getSelectionEnd();

            if (key.equals("Clear")) {
                newText = "";
            } else if (key.equals("←")) {
                if (hasSelection) {
                    int start = targetField.getSelectionStart();
                    int end = targetField.getSelectionEnd();
                    newText = currentText.substring(0, start) + currentText.substring(end);
                } else if (currentText.length() > 0) {
                    newText = currentText.substring(0, currentText.length() - 1);
                }
            } else if (key.equals("Space")) {
                if (hasSelection) {
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + " " + currentText.substring(targetField.getSelectionEnd());
                } else {
                    newText = currentText + " ";
                }
            } else if (key.equals("Enter")) {
                dialog.dispose();
                return;
            } else {
                String keyText = keyButton.getText();

                if (hasSelection) {
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + keyText + currentText.substring(targetField.getSelectionEnd());
                } else {
                    newText = currentText + keyText;
                }

                // Auto-switch to lowercase after letter typed with shift
                if (keyText.length() == 1 && Character.isLetter(keyText.charAt(0)) && shiftPressed[0]) {
                    isUpperCase[0] = false;
                    shiftPressed[0] = false;
                    // Update all letter buttons to lowercase
                    for (JButton btn : letterButtons) {
                        String btnText = btn.getText();
                        if (btnText.length() == 1 && Character.isLetter(btnText.charAt(0))) {
                            btn.setText(btnText.toLowerCase());
                        }
                    }
                    // Update shift button appearance
                    if (shiftButton[0] != null) {
                        shiftButton[0].setText("abc");
                        shiftButton[0].setBackground(new Color(248, 249, 250));
                        shiftButton[0].setForeground(Color.BLACK);
                        shiftButton[0].repaint();
                    }
                }
            }

            targetField.setText(newText);
        });

        return keyButton;
    }

    /**
     * Create close button for keyboard dialog
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
}
