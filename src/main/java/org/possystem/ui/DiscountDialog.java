package org.possystem.ui;

import org.possystem.dto.SeniorVeteranDiscountResponse;
import org.possystem.entity.TransactionDiscount;
import org.possystem.service.DiscountApiClient;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Discount Dialog - Allows user to apply Senior, Veteran, or Coupon discounts.
 * Styled as a personalized dialog with blue header and rounded buttons.
 */
public class DiscountDialog extends JDialog {

    private final TransactionService transactionService;
    private final DiscountApiClient discountApiClient;
    private final Runnable onDiscountApplied;

    // UI Components
    private JLabel statusLabel;
    private JButton seniorButton;
    private JButton veteranButton;
    private JButton couponButton;
    private JButton removeSeniorVeteranButton;
    private JButton removeCouponButton;

    // Screen scaling
    private final float scaleFactor;
    private final int headerFontSize;
    private final int bodyFontSize;
    private final int buttonFontSize;
    private final int buttonWidth;
    private final int buttonHeight;

    /**
     * Constructor for DiscountDialog.
     *
     * @param parent              The parent window
     * @param transactionService  The transaction service
     * @param discountApiClient   The discount API client
     * @param onDiscountApplied   Callback to refresh UI after discount applied
     */
    public DiscountDialog(Window parent, TransactionService transactionService,
                          DiscountApiClient discountApiClient, Runnable onDiscountApplied) {
        super(parent, "Discount Options", Dialog.ModalityType.APPLICATION_MODAL);
        this.transactionService = transactionService;
        this.discountApiClient = discountApiClient;
        this.onDiscountApplied = onDiscountApplied;

        // Calculate responsive scaling
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        this.scaleFactor = screenHeight / 1080.0f;

        this.headerFontSize = Math.round(20 * scaleFactor);
        this.bodyFontSize = Math.round(14 * scaleFactor); // Base font size for calculations
        this.buttonFontSize = Math.round(15 * scaleFactor); // Moderate font size
        this.buttonWidth = Math.round(140 * scaleFactor); // Compact width
        this.buttonHeight = Math.round(65 * scaleFactor); // Moderate height

        // Dialog setup
        setUndecorated(true);
        setResizable(false);
        setLayout(new BorderLayout());
        setBackground(Color.WHITE); // Set dialog background to white

        int dialogWidth = (int) (screenSize.width * 0.27); // 1/4 smaller (from 30% to 27%)
        int dialogHeight = (int) (screenSize.height * 0.32); // Same height
        setSize(dialogWidth, dialogHeight);
        setMinimumSize(new Dimension(490, 300));

        // Add border around entire dialog for definition
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));

        // Center on screen
        setLocationRelativeTo(null);

        initializeComponents();
        layoutComponents();
        updateStatusDisplay();
    }

    private void initializeComponents() {
        // Action buttons (shorter text for 2x2 grid) - Purple color scheme for discount-related actions
        seniorButton = createButton("<html><center>Senior<br>Discount (5%)</center></html>", new Color(168, 85, 247)); // Vibrant purple
        seniorButton.addActionListener(e -> applySeniorDiscount());

        veteranButton = createButton("<html><center>Veteran<br>Discount (10%)</center></html>", new Color(168, 85, 247)); // Vibrant purple
        veteranButton.addActionListener(e -> applyVeteranDiscount());

        couponButton = createButton("<html><center>Apply<br>Coupon</center></html>", new Color(168, 85, 247)); // Vibrant purple
        couponButton.addActionListener(e -> applyCoupon());

        // Remove buttons (initially hidden, text will be updated dynamically)
        removeSeniorVeteranButton = createButton("<html><center>Remove<br>Discount</center></html>", new Color(220, 53, 69)); // Red
        removeSeniorVeteranButton.addActionListener(e -> removeSeniorVeteranDiscount());
        removeSeniorVeteranButton.setVisible(false);

        removeCouponButton = createButton("<html><center>Remove<br>Coupon</center></html>", new Color(220, 53, 69)); // Red
        removeCouponButton.addActionListener(e -> removeCouponDiscount());
        removeCouponButton.setVisible(false);

        // Status label (left-aligned for side panel)
        statusLabel = new JLabel("<html><div style='text-align: left;'>No discounts<br>applied</div></html>");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, Math.round(bodyFontSize * 1.15f))); // Even bigger font
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setVerticalAlignment(SwingConstants.TOP);
        statusLabel.setForeground(new Color(100, 100, 100));
    }

    private JButton createButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        button.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        button.setFocusPainted(false);

        // Apply text outline and rounded styling (matches ActionsPanel)
        applyTextOutline(button);

        return button;
    }

    /**
     * Apply black text outline to button for better readability.
     * Handles HTML-formatted multi-line button text.
     * Matches ActionsPanel button styling (12px arc, darker border).
     */
    private void applyTextOutline(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();

                // Enable anti-aliasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12; // Match ActionsPanel arc size

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

                // Draw filled rounded rectangle background
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw darker border outline (matches ActionsPanel)
                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                String text = btn.getText();

                // Check if text contains HTML
                if (text != null && text.toLowerCase().contains("<html>")) {
                    // Split by <br> first, then remove HTML tags from each line
                    String[] rawLines = text.split("(?i)<br>");

                    // Clean up lines and remove HTML tags
                    java.util.List<String> cleanLines = new java.util.ArrayList<>();
                    for (String line : rawLines) {
                        String plainLine = line.replaceAll("<[^>]*>", "").trim(); // Remove HTML tags
                        if (!plainLine.isEmpty()) {
                            cleanLines.add(plainLine);
                        }
                    }

                    // Draw multi-line text with outline
                    FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                    g2d.setFont(btn.getFont());

                    int lineHeight = fm.getHeight();
                    int totalHeight = lineHeight * cleanLines.size();
                    int startY = (btn.getHeight() - totalHeight) / 2 + fm.getAscent();

                    for (int i = 0; i < cleanLines.size(); i++) {
                        String line = cleanLines.get(i);
                        int textWidth = fm.stringWidth(line);
                        int x = (btn.getWidth() - textWidth) / 2;
                        int y = startY + (i * lineHeight);

                        // Draw black outline if button is enabled (matches ActionsPanel)
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

                        // Draw main text on top
                        g2d.setColor(textColor);
                        g2d.drawString(line, x, y);
                    }
                } else {
                    // Plain text - use simple string rendering
                    FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                    g2d.setFont(btn.getFont());

                    // Calculate text position (centered)
                    int textWidth = fm.stringWidth(text);
                    int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    int x = (btn.getWidth() - textWidth) / 2;

                    // Draw black outline if button is enabled (matches ActionsPanel)
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

                    // Draw main text on top
                    g2d.setColor(textColor);
                    g2d.drawString(text, x, y);
                }

                g2d.dispose();
            }
        });
    }

    private void layoutComponents() {
        // Header Panel (Blue)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184)); // Blue header
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18)); // Reduced padding

        JLabel headerLabel = new JLabel("Discount Options");
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add close button
        JButton closeButton = new JButton("✕");
        closeButton.setFont(new Font("Arial", Font.BOLD, 20));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(23, 162, 184));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false); // Transparent background
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

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

        // Main Content Panel - Horizontal split
        JPanel contentPanel = new JPanel(new BorderLayout(0, 0));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 8)); // Reduced padding for smaller dialog

        // LEFT: 2x2 Button Grid Panel
        JPanel buttonGridWrapper = new JPanel(new BorderLayout());
        buttonGridWrapper.setBackground(Color.WHITE);
        buttonGridWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12)); // Add 12px right padding

        JPanel buttonGridPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonGridPanel.setBackground(Color.WHITE);

        // Calculate grid size based on button dimensions to fill most of the height
        int gridWidth = (buttonWidth * 2) + 10; // 2 buttons + gap
        int gridHeight = (buttonHeight * 2) + 10; // 2 buttons + gap
        buttonGridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
        buttonGridPanel.setMaximumSize(new Dimension(gridWidth, gridHeight));
        buttonGridPanel.setMinimumSize(new Dimension(gridWidth, gridHeight));

        // Create Cancel button (gray)
        JButton cancelButton = createButton("<html><center>Cancel</center></html>", new Color(108, 117, 125));
        cancelButton.addActionListener(e -> dispose());

        // Add buttons to grid: Row 1: Senior | Veteran, Row 2: Apply Coupon | Cancel
        buttonGridPanel.add(seniorButton);
        buttonGridPanel.add(veteranButton);
        buttonGridPanel.add(couponButton);
        buttonGridPanel.add(cancelButton);

        buttonGridWrapper.add(buttonGridPanel, BorderLayout.CENTER);
        contentPanel.add(buttonGridWrapper, BorderLayout.WEST);

        // RIGHT: Currently Applied Panel with prominent divider
        JPanel rightSidePanel = new JPanel(new BorderLayout(0, 0));
        rightSidePanel.setBackground(Color.WHITE);

        // Add prominent vertical divider
        JPanel dividerPanel = new JPanel();
        dividerPanel.setBackground(new Color(200, 200, 200)); // Gray divider
        dividerPanel.setPreferredSize(new Dimension(2, 0)); // 2px thick divider
        dividerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12)); // 12px right padding
        rightSidePanel.add(dividerPanel, BorderLayout.WEST);

        JPanel appliedPanelWrapper = new JPanel(new BorderLayout());
        appliedPanelWrapper.setBackground(Color.WHITE);

        JPanel appliedPanel = new JPanel();
        appliedPanel.setLayout(new BoxLayout(appliedPanel, BoxLayout.Y_AXIS));
        appliedPanel.setBackground(Color.WHITE);
        appliedPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); // Minimal left padding

        // Title
        JLabel appliedLabel = new JLabel("Currently Applied");
        appliedLabel.setFont(new Font("Arial", Font.BOLD, Math.round(bodyFontSize * 1.3f))); // Even bigger font
        appliedLabel.setForeground(Color.BLACK);
        appliedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appliedPanel.add(appliedLabel);
        appliedPanel.add(Box.createVerticalStrut(12));

        // Status label (left-aligned)
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, Math.round(bodyFontSize * 1.15f))); // Even bigger font
        appliedPanel.add(statusLabel);
        appliedPanel.add(Box.createVerticalStrut(18));

        // Remove buttons section
        JLabel removeLabel = new JLabel("Remove Discounts");
        removeLabel.setFont(new Font("Arial", Font.BOLD, Math.round(bodyFontSize * 1.25f))); // Even bigger font
        removeLabel.setForeground(Color.BLACK);
        removeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appliedPanel.add(removeLabel);
        appliedPanel.add(Box.createVerticalStrut(10));

        // Make remove buttons appropriately sized for compact dialog
        int smallButtonWidth = Math.round(buttonWidth * 1.35f); // Slightly wider to fill space better
        int smallButtonHeight = Math.round(buttonHeight * 0.65f); // Compact height

        removeSeniorVeteranButton.setPreferredSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeSeniorVeteranButton.setMaximumSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeSeniorVeteranButton.setMinimumSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeSeniorVeteranButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        appliedPanel.add(removeSeniorVeteranButton);
        appliedPanel.add(Box.createVerticalStrut(10));

        removeCouponButton.setPreferredSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeCouponButton.setMaximumSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeCouponButton.setMinimumSize(new Dimension(smallButtonWidth, smallButtonHeight));
        removeCouponButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        appliedPanel.add(removeCouponButton);

        // Add glue to push everything to the top
        appliedPanel.add(Box.createVerticalGlue());

        appliedPanelWrapper.add(appliedPanel, BorderLayout.NORTH);
        rightSidePanel.add(appliedPanelWrapper, BorderLayout.CENTER);

        contentPanel.add(rightSidePanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Update the status display to show currently applied discounts.
     */
    private void updateStatusDisplay() {
        try {
            List<TransactionDiscount> activeDiscounts = transactionService.getActiveDiscounts();

            if (activeDiscounts.isEmpty()) {
                statusLabel.setText("<html><div style='text-align: left;'>No discounts<br>applied</div></html>");
                statusLabel.setForeground(new Color(100, 100, 100));
                removeSeniorVeteranButton.setVisible(false);
                removeCouponButton.setVisible(false);

                // Re-enable both buttons when no discounts applied
                seniorButton.setEnabled(true);
                veteranButton.setEnabled(true);
                return;
            }

            StringBuilder statusText = new StringBuilder("<html><div style='text-align: left;'>");
            boolean hasSenior = false;
            boolean hasVeteran = false;
            boolean hasCoupon = false;

            for (TransactionDiscount discount : activeDiscounts) {
                String type = discount.discountType();
                double amount = Math.abs(discount.discountAmount());

                if (type.equals("SENIOR")) {
                    statusText.append("• Senior (5%)<br>  -$").append(String.format("%.2f", amount)).append("<br>");
                    hasSenior = true;
                } else if (type.equals("VETERAN")) {
                    statusText.append("• Veteran (10%)<br>  -$").append(String.format("%.2f", amount)).append("<br>");
                    hasVeteran = true;
                } else if (type.equals("COUPON")) {
                    statusText.append("• Coupon<br>  -$").append(String.format("%.2f", amount)).append("<br>");
                    hasCoupon = true;
                }
            }

            statusText.append("</div></html>");
            statusLabel.setText(statusText.toString());
            statusLabel.setForeground(new Color(40, 167, 69)); // Green for active

            // Update button text and visibility based on which discount is active
            if (hasSenior) {
                removeSeniorVeteranButton.setText("<html><center>Remove<br>Senior Discount</center></html>");
                removeSeniorVeteranButton.setVisible(true);
                seniorButton.setEnabled(false);   // Disable both buttons when senior applied
                veteranButton.setEnabled(false);
            } else if (hasVeteran) {
                removeSeniorVeteranButton.setText("<html><center>Remove<br>Veteran Discount</center></html>");
                removeSeniorVeteranButton.setVisible(true);
                seniorButton.setEnabled(false);   // Disable both buttons when veteran applied
                veteranButton.setEnabled(false);
            } else {
                removeSeniorVeteranButton.setVisible(false);
                seniorButton.setEnabled(true);    // Re-enable both when no discount
                veteranButton.setEnabled(true);
            }

            removeCouponButton.setVisible(hasCoupon);

        } catch (Exception e) {
            statusLabel.setText("<html><div style='text-align: left;'>Error loading<br>discounts</div></html>");
            statusLabel.setForeground(new Color(220, 53, 69));
        }
    }

    /**
     * Apply senior discount (5%).
     */
    private void applySeniorDiscount() {
        try {
            double cartSubtotal = transactionService.getSubtotal();
            SeniorVeteranDiscountResponse response = discountApiClient.calculateSeniorDiscount(cartSubtotal);

            transactionService.applySeniorVeteranDiscount(
                "SENIOR",
                -response.discountAmount() // Store as negative
            );

            updateStatusDisplay();
            if (onDiscountApplied != null) {
                onDiscountApplied.run();
            }

            // Close dialog after successful application
            dispose();

        } catch (Exception e) {
            showErrorDialog("Error", "Failed to apply senior discount: " + e.getMessage());
        }
    }

    /**
     * Apply veteran discount (10%).
     */
    private void applyVeteranDiscount() {
        try {
            double cartSubtotal = transactionService.getSubtotal();
            SeniorVeteranDiscountResponse response = discountApiClient.calculateVeteranDiscount(cartSubtotal);

            transactionService.applySeniorVeteranDiscount(
                "VETERAN",
                -response.discountAmount() // Store as negative
            );

            updateStatusDisplay();
            if (onDiscountApplied != null) {
                onDiscountApplied.run();
            }

            // Close dialog after successful application
            dispose();

        } catch (Exception e) {
            showErrorDialog("Error", "Failed to apply veteran discount: " + e.getMessage());
        }
    }

    /**
     * Apply coupon code (placeholder for Phase 3C).
     */
    private void applyCoupon() {
        showInfoDialog("Coming Soon", "Coupon feature will be implemented in Phase 3C.");
    }

    /**
     * Remove senior or veteran discount.
     */
    private void removeSeniorVeteranDiscount() {
        try {
            boolean removed = false;

            if (transactionService.hasDiscountType("SENIOR")) {
                transactionService.removeDiscountByType("SENIOR");
                removed = true;
            } else if (transactionService.hasDiscountType("VETERAN")) {
                transactionService.removeDiscountByType("VETERAN");
                removed = true;
            }

            if (removed) {
                updateStatusDisplay();
                if (onDiscountApplied != null) {
                    onDiscountApplied.run();
                }
            }

        } catch (Exception e) {
            showErrorDialog("Error", "Failed to remove discount: " + e.getMessage());
        }
    }

    /**
     * Remove coupon discount (placeholder for Phase 3C).
     */
    private void removeCouponDiscount() {
        showInfoDialog("Coming Soon", "Coupon removal will be implemented in Phase 3C.");
    }

    // Dialog helper methods
    private void showErrorDialog(String title, String message) {
        showSimpleDialog(title, message, new Color(220, 53, 69)); // Red
    }

    private void showSuccessDialog(String title, String message) {
        showSimpleDialog(title, message, new Color(40, 167, 69)); // Green
    }

    private void showWarningDialog(String title, String message) {
        showSimpleDialog(title, message, new Color(255, 193, 7)); // Amber
    }

    private void showInfoDialog(String title, String message) {
        showSimpleDialog(title, message, new Color(23, 162, 184)); // Blue
    }

    private void showSimpleDialog(String title, String message, Color headerColor) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerColor);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        header.add(headerLabel, BorderLayout.CENTER);

        dialog.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>" + message + "</div></html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(messageLabel, BorderLayout.CENTER);

        dialog.add(body, BorderLayout.CENTER);

        // Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(headerColor);
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(120, 45));
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
