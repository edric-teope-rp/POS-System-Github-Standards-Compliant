package org.possystem.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * API Configuration Dialog
 * Allows configuring the Discount Engine API base URL
 */
public class ApiConfigDialog extends JDialog {

    private static final String CONFIG_FILE = "config/api-config.json";
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/discounts";

    private final org.possystem.service.AuthService authService;
    private JTextField apiUrlField;
    private JLabel statusLabel;

    // Dynamic font scaling
    private final float fontScale;
    private final int headerFontSize;
    private final int labelFontSize;
    private final int buttonFontSize;

    public ApiConfigDialog(Frame parent, org.possystem.service.AuthService authService) {
        super(parent, "API Configuration", Dialog.ModalityType.APPLICATION_MODAL);
        this.authService = authService;

        // Calculate font scaling
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;

        if (screenHeight <= 768) {
            fontScale = 0.85f;
        } else if (screenHeight <= 1080) {
            fontScale = 1.0f;
        } else if (screenHeight <= 1440) {
            fontScale = 1.25f;
        } else {
            fontScale = 1.75f;
        }

        headerFontSize = Math.round(22 * fontScale);
        labelFontSize = Math.round(16 * fontScale);
        buttonFontSize = Math.round(14 * fontScale);

        initializeDialog();
        createUI();
        loadConfiguration();
    }

    private void initializeDialog() {
        setUndecorated(true);
        setResizable(false);
        setSize(Math.round(600 * fontScale), Math.round(350 * fontScale));
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void createUI() {
        // Blue Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // White Content Panel
        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180)); // Steel blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left side: Title with username
        String username = authService.getCurrentUsername();
        String titleText = "API Configuration - Logged in as: " + (username != null ? username : "Unknown");
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        titleLabel.setForeground(Color.WHITE);

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Right side: Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, Math.round(12 * fontScale)));
        logoutButton.setBackground(new Color(220, 53, 69)); // Red
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setOpaque(true);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(Math.round(80 * fontScale), Math.round(35 * fontScale)));
        logoutButton.addActionListener(e -> {
            authService.logout();
            dispose();
        });

        headerPanel.add(logoutButton, BorderLayout.EAST);

        // Make header draggable
        final Point[] dragOffset = {null};
        headerPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getPoint();
            }
        });
        headerPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragOffset[0] != null) {
                    Point location = getLocation();
                    setLocation(location.x + e.getX() - dragOffset[0].x,
                            location.y + e.getY() - dragOffset[0].y);
                }
            }
        });

        return headerPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // API URL Label
        JLabel urlLabel = new JLabel("Discount Engine API URL:");
        urlLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPanel.add(urlLabel, gbc);

        // API URL Field
        apiUrlField = new JTextField(40);
        apiUrlField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        apiUrlField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        contentPanel.add(apiUrlField, gbc);

        // Test Connection Button
        JButton testButton = new JButton("Test Connection");
        testButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        testButton.setPreferredSize(new Dimension(160, 40));
        applyRoundedStyle(testButton, new Color(40, 167, 69)); // Green
        testButton.addActionListener(e -> testConnection());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(testButton, gbc);

        // Status Label
        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, Math.round(14 * fontScale)));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(statusLabel, gbc);

        // Info Label
        JLabel infoLabel = new JLabel("<html><i>Example: http://localhost:8080/api/discounts</i></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, Math.round(12 * fontScale)));
        infoLabel.setForeground(new Color(108, 117, 125));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(infoLabel, gbc);

        return contentPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        // Cancel Button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        cancelButton.setPreferredSize(new Dimension(120, 45));
        applyRoundedStyle(cancelButton, new Color(108, 117, 125)); // Gray
        cancelButton.addActionListener(e -> dispose());

        // Save Button
        JButton saveButton = new JButton("Save");
        saveButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        saveButton.setPreferredSize(new Dimension(120, 45));
        applyRoundedStyle(saveButton, new Color(70, 130, 180)); // Blue
        saveButton.addActionListener(e -> saveConfiguration());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void testConnection() {
        String apiUrl = apiUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            statusLabel.setText("❌ Please enter API URL");
            statusLabel.setForeground(new Color(220, 53, 69));
            return;
        }

        try {
            // Test connection to /health endpoint
            String healthUrl = apiUrl.replace("/api/discounts", "") + "/actuator/health";
            URL url = new URL(healthUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                statusLabel.setText("✅ Connection successful");
                statusLabel.setForeground(new Color(40, 167, 69));
            } else {
                statusLabel.setText("⚠️ API responded with code: " + responseCode);
                statusLabel.setForeground(new Color(255, 193, 7));
            }
            conn.disconnect();
        } catch (Exception e) {
            statusLabel.setText("❌ Connection failed: " + e.getMessage());
            statusLabel.setForeground(new Color(220, 53, 69));
        }
    }

    private void loadConfiguration() {
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                String content = Files.readString(Paths.get(CONFIG_FILE));
                // Simple JSON parsing (extract URL)
                String url = content.replace("{", "")
                        .replace("}", "")
                        .replace("\"", "")
                        .replace("apiUrl:", "")
                        .replace("apiUrl", "")
                        .replace(":", "")
                        .trim();
                apiUrlField.setText(url);
            } else {
                apiUrlField.setText(DEFAULT_API_URL);
            }
        } catch (IOException e) {
            apiUrlField.setText(DEFAULT_API_URL);
        }
    }

    private void saveConfiguration() {
        String apiUrl = apiUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an API URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(Paths.get("config"));

            // Write simple JSON
            String json = String.format("{\"apiUrl\":\"%s\"}", apiUrl);
            Files.writeString(Paths.get(CONFIG_FILE), json);

            JOptionPane.showMessageDialog(this, "Configuration saved successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyRoundedStyle(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
