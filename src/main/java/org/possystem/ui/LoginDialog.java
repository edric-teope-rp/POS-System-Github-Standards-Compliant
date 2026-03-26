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
        setSize(Math.round(450 * fontScale), Math.round(300 * fontScale));

        // Position at upper center, following parent window location
        Window parentWindow = (Window) getParent();
        if (parentWindow != null) {
            Point parentLocation = parentWindow.getLocation();
            Dimension parentSize = parentWindow.getSize();
            int dialogWidth = Math.round(450 * fontScale);
            int dialogHeight = Math.round(300 * fontScale);

            // Center horizontally on parent
            int x = parentLocation.x + (parentSize.width - dialogWidth) / 2;
            // Position at upper portion (15% down from parent's top)
            int y = parentLocation.y + (int) (parentSize.height * 0.15);

            setLocation(x, y);
        } else {
            // Fallback to center if no parent
            setLocationRelativeTo(getParent());
        }

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

        // Username Label (left side)
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        contentPanel.add(usernameLabel, gbc);

        // Username Field (right side, takes remaining width)
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        usernameField.setEditable(true);
        usernameField.setFocusable(true);
        usernameField.setToolTipText("Double-click for on-screen keyboard");
        usernameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openAlphanumericKeyboard(usernameField, "Username");
                }
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        contentPanel.add(usernameField, gbc);

        // Password Label (left side)
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        contentPanel.add(passwordLabel, gbc);

        // Password Field (right side, takes remaining width)
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        passwordField.setEditable(true);
        passwordField.setFocusable(true);
        passwordField.setToolTipText("Double-click for on-screen keyboard");
        passwordField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Create a temporary JTextField wrapper for the password field
                    JTextField tempField = new JTextField() {
                        @Override
                        public String getText() {
                            return new String(passwordField.getPassword());
                        }
                        @Override
                        public void setText(String text) {
                            passwordField.setText(text);
                        }
                        @Override
                        public int getSelectionStart() {
                            return passwordField.getSelectionStart();
                        }
                        @Override
                        public int getSelectionEnd() {
                            return passwordField.getSelectionEnd();
                        }
                        @Override
                        public void selectAll() {
                            passwordField.selectAll();
                        }
                        @Override
                        public void requestFocus() {
                            passwordField.requestFocus();
                        }
                    };
                    openAlphanumericKeyboard(tempField, "Password");
                }
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
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
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

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
     * Show the login dialog and return whether login was successful.
     * @return true if login successful, false otherwise
     */
    public boolean showLoginDialog() {
        setVisible(true);
        // Request focus on username field after dialog is visible
        SwingUtilities.invokeLater(() -> {
            usernameField.requestFocusInWindow();
        });
        return loginSuccessful;
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

        // Position at lower center, following parent window location
        Window parentWindow = (Window) getParent();
        if (parentWindow != null) {
            Point parentLocation = parentWindow.getLocation();
            Dimension parentSize = parentWindow.getSize();

            // Center horizontally on parent
            int keyboardX = parentLocation.x + (parentSize.width - dialogWidth) / 2;
            // Position at lower center (65% down from parent's top, or 80px from bottom of screen)
            int keyboardY = Math.min(
                parentLocation.y + (int) (parentSize.height * 0.65),
                screenSize.height - dialogHeight - 80
            );

            keyboardDialog.setLocation(keyboardX, keyboardY);
        } else {
            // Fallback to screen center bottom
            int keyboardX = (screenSize.width - dialogWidth) / 2;
            int keyboardY = screenSize.height - dialogHeight - 80;
            keyboardDialog.setLocation(keyboardX, keyboardY);
        }
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

        // QWERTY Keyboard
        JPanel keyboardPanel = createQWERTYKeyboard(targetField, keyboardDialog);
        keyboardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keyboardPanel);

        keyboardDialog.add(centerPanel, BorderLayout.CENTER);

        // Select all text when keyboard opens
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

        // Row 0: Numbers
        String[] row0 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        gbc.gridy = 0;
        for (int i = 0; i < row0.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createSimpleKeyboardKey(row0[i], targetField, dialog);
            keyboardPanel.add(key, gbc);
        }

        // Row 1: qwertyuiop (lowercase)
        String[] row1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        gbc.gridy = 1;
        for (int i = 0; i < row1.length; i++) {
            gbc.gridx = i;
            JButton key = createSimpleKeyboardKey(row1[i], targetField, dialog);
            keyboardPanel.add(key, gbc);
        }

        // Row 2: asdfghjkl (lowercase)
        String[] row2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
        gbc.gridy = 2;
        for (int i = 0; i < row2.length; i++) {
            gbc.gridx = i;
            JButton key = createSimpleKeyboardKey(row2[i], targetField, dialog);
            keyboardPanel.add(key, gbc);
        }

        // Backspace
        gbc.gridx = 9;
        JButton backspaceBtn = createSimpleKeyboardKey("←", targetField, dialog);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        backspaceBtn.setForeground(Color.WHITE);
        applyRoundedKeyStyle(backspaceBtn);
        keyboardPanel.add(backspaceBtn, gbc);

        // Row 3: zxcvbnm (lowercase)
        gbc.gridy = 3;
        String[] row3 = {"z", "x", "c", "v", "b", "n", "m"};
        for (int i = 0; i < row3.length; i++) {
            gbc.gridx = i;
            JButton key = createSimpleKeyboardKey(row3[i], targetField, dialog);
            keyboardPanel.add(key, gbc);
        }

        // Clear button
        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton clearBtn = createSimpleKeyboardKey("Clear", targetField, dialog);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        applyRoundedKeyStyle(clearBtn);
        keyboardPanel.add(clearBtn, gbc);

        // Row 4: Space + Enter
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 7;
        JButton spaceBtn = createSimpleKeyboardKey("Space", targetField, dialog);
        keyboardPanel.add(spaceBtn, gbc);

        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton enterBtn = createSimpleKeyboardKey("Enter", targetField, dialog);
        enterBtn.setBackground(new Color(40, 167, 69)); // Green
        enterBtn.setForeground(Color.WHITE);
        applyRoundedKeyStyle(enterBtn);
        keyboardPanel.add(enterBtn, gbc);

        return keyboardPanel;
    }

    /**
     * Create simple keyboard key button (lowercase only, no shift logic)
     */
    private JButton createSimpleKeyboardKey(String key, JTextField targetField, JDialog dialog) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, Math.round(16 * fontScale)));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250));
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = targetField.getText();
            String newText = currentText;

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
                // Regular key press
                if (hasSelection) {
                    int start = targetField.getSelectionStart();
                    newText = currentText.substring(0, start) + key + currentText.substring(targetField.getSelectionEnd());
                } else {
                    newText = currentText + key;
                }
            }

            targetField.setText(newText);
        });

        return keyButton;
    }

    /**
     * Apply rounded style with darker outline to special keyboard keys (matches search engine keyboard)
     */
    private void applyRoundedKeyStyle(JButton button) {
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
