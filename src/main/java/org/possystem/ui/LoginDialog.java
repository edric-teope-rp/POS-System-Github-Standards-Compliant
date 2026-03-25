package org.possystem.ui;

import org.possystem.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

/**
 * Login Dialog for Manager Authentication.
 * Displays username and password fields with on-screen keyboard support.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private boolean loginSuccessful = false;

    // Dynamic font scaling
    private final float fontScale;
    private final int headerFontSize;
    private final int labelFontSize;
    private final int buttonFontSize;

    public LoginDialog(Frame parent, AuthService authService) {
        super(parent, "Manager Login", Dialog.ModalityType.APPLICATION_MODAL);
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
    }

    private void initializeDialog() {
        setUndecorated(true);
        setResizable(false);
        setSize(Math.round(450 * fontScale), Math.round(400 * fontScale));
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void createUI() {
        // Blue Header Panel (Info style)
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
        headerPanel.setBackground(new Color(23, 162, 184)); // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Manager Login Required");
        titleLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(titleLabel, BorderLayout.CENTER);

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
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username Label
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        contentPanel.add(usernameLabel, gbc);

        // Username Field
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        contentPanel.add(usernameField, gbc);

        // Password Label
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        contentPanel.add(passwordLabel, gbc);

        // Password Field
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        contentPanel.add(passwordField, gbc);

        // Add Enter key listener to password field
        passwordField.addActionListener(e -> attemptLogin());

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
        cancelButton.addActionListener(e -> {
            loginSuccessful = false;
            dispose();
        });

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        loginButton.setPreferredSize(new Dimension(120, 45));
        applyRoundedStyle(loginButton, new Color(23, 162, 184)); // Info blue
        loginButton.addActionListener(e -> attemptLogin());

        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton);

        return buttonPanel;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        try {
            boolean success = authService.login(username, password);
            if (success) {
                loginSuccessful = true;
                dispose();
            } else {
                showError("Invalid username or password");
                passwordField.setText("");
                passwordField.requestFocus();
            }
        } catch (SQLException e) {
            showError("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Login Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void applyRoundedStyle(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Show the login dialog and return whether login was successful.
     * @return true if login successful, false otherwise
     */
    public boolean showLoginDialog() {
        setVisible(true);
        return loginSuccessful;
    }
}
