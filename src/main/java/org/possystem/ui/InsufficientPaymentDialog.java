package org.possystem.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Insufficient Payment Dialog - Displayed when cash payment doesn't cover the total.
 * Offers choices: Add More Cash, Pay with Card, or Cancel.
 * Styled as a personalized dialog with amber/yellow warning header.
 */
public class InsufficientPaymentDialog extends JDialog {

    // Payment choice enum
    public enum PaymentChoice {
        ADD_MORE_CASH,
        PAY_WITH_CARD,
        CANCEL
    }

    private PaymentChoice userChoice = PaymentChoice.CANCEL; // Default to cancel

    // Screen scaling
    private final float scaleFactor;
    private final int headerFontSize;
    private final int bodyFontSize;
    private final int buttonFontSize;

    /**
     * Constructor for InsufficientPaymentDialog.
     *
     * @param parent         The parent window
     * @param totalDue       The total amount due
     * @param amountReceived The cash amount already received
     * @param remaining      The remaining balance to be paid
     */
    public InsufficientPaymentDialog(Window parent, double totalDue, double amountReceived, double remaining) {
        super(parent, "Insufficient Payment", Dialog.ModalityType.APPLICATION_MODAL);

        // Calculate responsive scaling
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        this.scaleFactor = screenHeight / 1080.0f;

        this.headerFontSize = Math.round(20 * scaleFactor);
        this.bodyFontSize = Math.round(16 * scaleFactor);
        this.buttonFontSize = Math.round(16 * scaleFactor);

        // Dialog setup
        setUndecorated(true);
        setResizable(false);
        setLayout(new BorderLayout());

        // Dynamic sizing based on screen - width optimized for three buttons in single row
        int dialogWidth = (int) (480 * scaleFactor); // Increased to fit all 3 buttons in one row
        int dialogHeight = (int) (420 * scaleFactor); // Reduced height since buttons are single row now
        setSize(dialogWidth, dialogHeight);

        // Add border around entire dialog
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));

        // Center on parent window
        setLocationRelativeTo(parent);

        // Initialize components
        initializeComponents(totalDue, amountReceived, remaining);
    }

    private void initializeComponents(double totalDue, double amountReceived, double remaining) {
        // Header panel (Amber warning color)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 193, 7)); // Amber color
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        // Header label (centered)
        JLabel headerLabel = new JLabel("⚠ Insufficient Payment", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.BLACK);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Make header draggable
        final Point[] mouseDownLocation = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownLocation[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownLocation[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownLocation[0] != null) {
                    Point currentLocation = getLocationOnScreen();
                    setLocation(currentLocation.x + e.getX() - mouseDownLocation[0].x,
                               currentLocation.y + e.getY() - mouseDownLocation[0].y);
                }
            }
        });

        add(headerPanel, BorderLayout.NORTH);

        // Details panel (center)
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setLayout(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Total Due
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel totalLabel = new JLabel("Total Due:");
        totalLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsPanel.add(totalLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel totalValue = new JLabel(String.format("$%.2f", totalDue));
        totalValue.setFont(new Font("Arial", Font.BOLD, bodyFontSize));
        detailsPanel.add(totalValue, gbc);

        // Amount Received
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel receivedLabel = new JLabel("Cash Received:");
        receivedLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsPanel.add(receivedLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel receivedValue = new JLabel(String.format("$%.2f", amountReceived));
        receivedValue.setFont(new Font("Arial", Font.BOLD, bodyFontSize));
        detailsPanel.add(receivedValue, gbc);

        // Divider line
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 0, 12, 0);
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(200, 200, 200));
        detailsPanel.add(separator, gbc);

        // Remaining Balance (highlighted)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel remainingLabel = new JLabel("Remaining:");
        remainingLabel.setFont(new Font("Arial", Font.BOLD, (int) (bodyFontSize * 1.2)));
        remainingLabel.setForeground(new Color(220, 53, 69)); // Red color for emphasis
        detailsPanel.add(remainingLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel remainingValue = new JLabel(String.format("$%.2f", remaining));
        remainingValue.setFont(new Font("Arial", Font.BOLD, (int) (bodyFontSize * 1.2)));
        remainingValue.setForeground(new Color(220, 53, 69)); // Red color for emphasis
        detailsPanel.add(remainingValue, gbc);

        add(detailsPanel, BorderLayout.CENTER);

        // Button panel (south) - Single row with all buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 25, 15));

        // Cancel button (Gray) - FIRST (left-most)
        JButton cancelButton = createRoundedButton("Cancel", new Color(108, 117, 125));
        cancelButton.addActionListener(e -> {
            userChoice = PaymentChoice.CANCEL;
            dispose();
        });
        buttonPanel.add(cancelButton);

        // Add More Cash button (Green) - SECOND (middle)
        JButton addCashButton = createRoundedButton("<html><center>Add More<br>Cash</center></html>", new Color(40, 167, 69));
        addCashButton.addActionListener(e -> {
            userChoice = PaymentChoice.ADD_MORE_CASH;
            dispose();
        });
        buttonPanel.add(addCashButton);

        // Pay with Card button (Blue) - THIRD (right-most)
        JButton payCardButton = createRoundedButton("<html><center>Pay with<br>Card</center></html>", new Color(0, 123, 255));
        payCardButton.addActionListener(e -> {
            userChoice = PaymentChoice.PAY_WITH_CARD;
            dispose();
        });
        buttonPanel.add(payCardButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Create a rounded button with the specified text and background color.
     * Uses the same styling as ActionsPanel buttons with rounded corners and black outline.
     */
    private JButton createRoundedButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Narrower button width for single row layout, increased height for better visibility
        int buttonWidth = (int) (115 * scaleFactor); // Narrower to fit 3 buttons
        int buttonHeight = (int) (75 * scaleFactor); // Increased height for better touch target
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

        // Apply rounded style with black outline (matches ActionsPanel)
        applyTextOutline(button);

        return button;
    }

    /**
     * Apply text outline and rounded corners to button.
     * Matches the styling of ActionsPanel buttons.
     */
    private void applyTextOutline(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.setContentAreaFilled(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12; // Matches ActionsPanel roundness

                Color buttonColor = btn.getBackground();
                Color displayColor = btn.isEnabled() ? buttonColor :
                    new Color((int)(buttonColor.getRed() * 0.5),
                             (int)(buttonColor.getGreen() * 0.5),
                             (int)(buttonColor.getBlue() * 0.5));
                Color textColor = btn.isEnabled() ? btn.getForeground() : new Color(180, 180, 180);

                // Draw filled rounded rectangle
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw darker border outline (matches ActionsPanel)
                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw text
                String text = btn.getText();
                g2d.setFont(btn.getFont());

                // Check if text contains HTML for multi-line
                if (text != null && text.toLowerCase().contains("<html>")) {
                    // Handle multi-line HTML text
                    String[] rawLines = text.split("(?i)<br>");
                    java.util.List<String> cleanLines = new java.util.ArrayList<>();
                    for (String line : rawLines) {
                        String plainLine = line.replaceAll("<[^>]*>", "").trim();
                        if (!plainLine.isEmpty()) {
                            cleanLines.add(plainLine);
                        }
                    }

                    FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                    int lineHeight = fm.getHeight();
                    int totalHeight = lineHeight * cleanLines.size();
                    int startY = (btn.getHeight() - totalHeight) / 2 + fm.getAscent();

                    for (int i = 0; i < cleanLines.size(); i++) {
                        String line = cleanLines.get(i);
                        int textWidth = fm.stringWidth(line);
                        int x = (btn.getWidth() - textWidth) / 2;
                        int y = startY + (i * lineHeight);

                        // Draw black outline
                        if (btn.isEnabled()) {
                            g2d.setColor(new Color(0, 0, 0, 150));
                            g2d.setStroke(new BasicStroke(3f));
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    if (dx != 0 || dy != 0) {
                                        g2d.drawString(line, x + dx, y + dy);
                                    }
                                }
                            }
                        }

                        // Draw main text
                        g2d.setColor(textColor);
                        g2d.drawString(line, x, y);
                    }
                } else {
                    // Single line text
                    FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                    int textWidth = fm.stringWidth(text);
                    int x = (btn.getWidth() - textWidth) / 2;
                    int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                    // Draw black outline
                    if (btn.isEnabled()) {
                        g2d.setColor(new Color(0, 0, 0, 150));
                        g2d.setStroke(new BasicStroke(3f));
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (dx != 0 || dy != 0) {
                                    g2d.drawString(text, x + dx, y + dy);
                                }
                            }
                        }
                    }

                    // Draw main text
                    g2d.setColor(textColor);
                    g2d.drawString(text, x, y);
                }

                g2d.dispose();
            }
        });
    }

    /**
     * Get the user's choice after dialog closes.
     *
     * @return The user's payment choice
     */
    public PaymentChoice getUserChoice() {
        return userChoice;
    }

    /**
     * Show the insufficient payment dialog and return user's choice.
     *
     * @param parent         The parent window
     * @param totalDue       The total amount due
     * @param amountReceived The cash amount already received
     * @param remaining      The remaining balance to be paid
     * @return The user's payment choice
     */
    public static PaymentChoice showDialog(Window parent, double totalDue, double amountReceived, double remaining) {
        InsufficientPaymentDialog dialog = new InsufficientPaymentDialog(parent, totalDue, amountReceived, remaining);
        dialog.setVisible(true); // Blocks until dialog is closed
        return dialog.getUserChoice();
    }
}
