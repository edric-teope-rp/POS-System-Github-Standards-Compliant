package org.possystem.ui;

import org.possystem.dto.SeniorVeteranDiscountResponse;
import org.possystem.dto.CouponValidationResponse;
import org.possystem.entity.TransactionDiscount;
import org.possystem.entity.TransactionItem;
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
    private final GlobalBarcodeScanner globalScanner;

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
     * @param globalScanner       The global barcode scanner (to disable during text entry)
     * @param onDiscountApplied   Callback to refresh UI after discount applied
     */
    public DiscountDialog(Window parent, TransactionService transactionService,
                          DiscountApiClient discountApiClient, GlobalBarcodeScanner globalScanner,
                          Runnable onDiscountApplied) {
        super(parent, "Discount Options", Dialog.ModalityType.APPLICATION_MODAL);
        this.transactionService = transactionService;
        this.discountApiClient = discountApiClient;
        this.globalScanner = globalScanner;
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
        int dialogHeight = (int) (screenSize.height * 0.38); // Increased from 0.32 to 0.38 for better spacing with multiple discounts
        setSize(dialogWidth, dialogHeight);
        setMinimumSize(new Dimension(490, 360)); // Increased from 300 to 360

        // Add border around entire dialog for definition
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));

        // Center on parent window
        setLocationRelativeTo(getParent());

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

        // Create Close button (gray)
        JButton cancelButton = createButton("<html><center>Close</center></html>", new Color(108, 117, 125));
        cancelButton.addActionListener(e -> dispose());

        // Add buttons to grid: Row 1: Senior | Veteran, Row 2: Apply Coupon | Close
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
     * Apply coupon code.
     * Opens on-screen keyboard for manual entry.
     */
    private void applyCoupon() {
        try {
            // Check if coupon already applied
            if (transactionService.hasDiscountType("COUPON")) {
                showWarningDialog("One Coupon Per Transaction", "Only one coupon can be applied per transaction.");
                return;
            }

            // Disable global barcode scanner during text entry
            if (globalScanner != null) {
                globalScanner.disableForDialog();
            }

            // Create a simple on-screen keyboard dialog for coupon entry
            JDialog keyboardDialog = new JDialog(this, "Enter Coupon Code", true);
            keyboardDialog.setUndecorated(true);
            keyboardDialog.setLayout(new BorderLayout());

            // Re-enable scanner when dialog closes
            keyboardDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    if (globalScanner != null) {
                        globalScanner.enable();
                    }
                }
            });

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(168, 85, 247)); // Purple
            header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

            JLabel headerLabel = new JLabel("Enter Coupon Code");
            headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
            headerLabel.setForeground(Color.WHITE);
            header.add(headerLabel, BorderLayout.CENTER);

            // Close button
            JButton closeButton = new JButton("✕");
            closeButton.setFont(new Font("Arial", Font.BOLD, 20));
            closeButton.setForeground(Color.WHITE);
            closeButton.setBackground(new Color(168, 85, 247));
            closeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            closeButton.setFocusPainted(false);
            closeButton.setOpaque(true);
            closeButton.setBorderPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> keyboardDialog.dispose());
            header.add(closeButton, BorderLayout.EAST);

            keyboardDialog.add(header, BorderLayout.NORTH);

            // Body
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            body.setBackground(Color.WHITE);

            // Text field for coupon code
            JTextField couponField = new JTextField();
            couponField.setFont(new Font("Arial", Font.BOLD, Math.round(bodyFontSize * 1.5f)));
            couponField.setPreferredSize(new Dimension(700, 50));
            couponField.setMaximumSize(new Dimension(700, 50));
            couponField.setHorizontalAlignment(JTextField.CENTER);
            couponField.setEditable(false); // Read-only, use on-screen keyboard
            couponField.setAlignmentX(Component.CENTER_ALIGNMENT);

            body.add(couponField);
            body.add(Box.createVerticalStrut(15));

            // On-screen keyboard
            JPanel keyboardPanel = createCouponKeyboard(couponField, keyboardDialog);
            keyboardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(keyboardPanel);

            keyboardDialog.add(body, BorderLayout.CENTER);

            keyboardDialog.setSize(750, 500);
            keyboardDialog.setLocationRelativeTo(this);
            keyboardDialog.setVisible(true);

        } catch (Exception e) {
            showErrorDialog("Error", "Failed to open coupon entry: " + e.getMessage());
        }
    }

    /**
     * Validate and apply coupon code via API.
     * @param couponCode The coupon code to validate
     */
    private void validateAndApplyCoupon(String couponCode) {
        try {
            // Get cart items and subtotal
            List<TransactionItem> cartItems = transactionService.getCurrentSaleItems();
            double cartSubtotal = transactionService.getTransactionSubtotal();

            double discountAmount = 0.0;
            String description = "Coupon: " + couponCode;
            boolean isEmptyCart = cartItems.isEmpty() || cartSubtotal == 0;

            // DEBUG: Log what we're sending
            System.out.println("=== COUPON VALIDATION DEBUG ===");
            System.out.println("Coupon Code: " + couponCode);
            System.out.println("Cart Subtotal: $" + String.format("%.2f", cartSubtotal));
            System.out.println("Cart Items Count: " + cartItems.size());

            // If cart is empty, send minimal dummy data to validate coupon exists
            if (isEmptyCart) {
                System.out.println("Empty cart - Sending dummy data for validation");
                // Create dummy cart item for validation
                cartItems = List.of(new TransactionItem(0, 0, "000000000000", "Validation", 1, 0.01, 0.01, "ACTIVE"));
                cartSubtotal = 0.01;
            }

            // Call API to validate coupon (with real or dummy data)
            CouponValidationResponse response = discountApiClient.validateCoupon(
                couponCode,
                cartItems,
                cartSubtotal
            );

            // DEBUG: Log API response
            System.out.println("API Response - Valid: " + response.valid());
            System.out.println("API Response - Triggered: " + response.triggered());
            if (!response.valid()) {
                System.out.println("API Response - Error Type: " + response.errorType());
                System.out.println("API Response - Message: " + response.message());
            } else {
                System.out.println("API Response - Discount Amount: $" + String.format("%.2f", response.discountAmount()));
                if (!response.triggered()) {
                    System.out.println("API Response - Remaining Amount: $" + String.format("%.2f", response.remainingAmount()));
                }
            }
            System.out.println("==============================");

            // Check if coupon is valid (exists and not expired)
            if (!response.valid()) {
                // Show error for invalid/expired coupons only
                String errorMessage = response.message();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Invalid coupon code";
                }
                showWarningDialog("Coupon Invalid", errorMessage);
                return;
            }

            // Coupon is valid - get discount amount and description from API
            // If cart was empty, use $0 discount (ignore API amount from dummy data)
            if (isEmptyCart) {
                discountAmount = 0.0;
                description = response.description() != null ? response.description() : "Coupon: " + couponCode;
                System.out.println("Empty cart - Using $0 discount (will recalculate when items added)");
            } else {
                // For non-empty carts, only use discount amount if triggered
                if (response.triggered()) {
                    discountAmount = response.discountAmount() != null ? response.discountAmount() : 0.0;
                    System.out.println("Coupon triggered - Applying discount: $" + discountAmount);
                } else {
                    discountAmount = 0.0;
                    System.out.println("Coupon not triggered (minimum not met) - Using $0 discount");
                }
                description = response.description() != null ? response.description() : "Coupon: " + couponCode;
            }

            System.out.println("Applying coupon " + couponCode + " with discount: $" + discountAmount);

            transactionService.applyCouponDiscount(
                couponCode,
                -discountAmount, // Store as negative
                description
            );

            updateStatusDisplay();
            if (onDiscountApplied != null) {
                onDiscountApplied.run();
            }

            // Show success toast notification
            Window parent = getOwner();
            if (parent != null) {
                String toastMessage = String.format("%s applied", couponCode.toUpperCase());
                ToastNotification.showToast(parent, "Coupon Applied", toastMessage);
            }

            // Close dialog after successful application
            dispose();

        } catch (Exception e) {
            // DEBUG: Print full stack trace
            System.err.println("=== COUPON VALIDATION EXCEPTION ===");
            e.printStackTrace();
            System.err.println("===================================");

            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + " occurred during validation";
            }
            showErrorDialog("Error", "Failed to validate coupon: " + errorMessage);
        }
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
     * Remove coupon discount.
     */
    private void removeCouponDiscount() {
        try {
            if (transactionService.hasDiscountType("COUPON")) {
                transactionService.removeDiscountByType("COUPON");

                updateStatusDisplay();
                if (onDiscountApplied != null) {
                    onDiscountApplied.run();
                }
            }

        } catch (Exception e) {
            showErrorDialog("Error", "Failed to remove coupon: " + e.getMessage());
        }
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

        // Calculate responsive sizes
        int headerPadding = Math.round(20 * scaleFactor);
        int bodyPadding = Math.round(25 * scaleFactor);
        int buttonPanelPadding = Math.round(20 * scaleFactor);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerColor);
        header.setBorder(BorderFactory.createEmptyBorder(headerPadding, headerPadding, headerPadding, headerPadding));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, Math.round(headerFontSize * 1.4f))); // Bigger header
        headerLabel.setForeground(Color.WHITE);
        header.add(headerLabel, BorderLayout.CENTER);

        dialog.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(bodyPadding, bodyPadding, bodyPadding, bodyPadding));

        JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>" + message + "</div></html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, Math.round(bodyFontSize * 1.6f))); // Much bigger body text
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(messageLabel, BorderLayout.CENTER);

        dialog.add(body, BorderLayout.CENTER);

        // Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(buttonPanelPadding, buttonPanelPadding, buttonPanelPadding, buttonPanelPadding));

        JButton okButton = new JButton("OK");
        okButton.setBackground(headerColor);
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, Math.round(buttonFontSize * 1.5f))); // Bigger button text

        // Make button color visible
        okButton.setOpaque(true);
        okButton.setBorderPainted(false);
        okButton.setContentAreaFilled(true);

        int okButtonWidth = Math.round(150 * scaleFactor);
        int okButtonHeight = Math.round(55 * scaleFactor);
        okButton.setPreferredSize(new Dimension(okButtonWidth, okButtonHeight));
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Scale dialog size
        int dialogWidth = Math.round(500 * scaleFactor);
        int dialogHeight = Math.round(300 * scaleFactor);
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Create on-screen QWERTY keyboard for coupon entry.
     * @param textField The text field to update
     * @param parentDialog The parent dialog to close on Apply
     * @return JPanel containing the keyboard
     */
    private JPanel createCouponKeyboard(JTextField textField, JDialog parentDialog) {
        JPanel keyboardPanel = new JPanel(new GridBagLayout());
        keyboardPanel.setBackground(Color.WHITE);
        keyboardPanel.setMaximumSize(new Dimension(700, 300));
        keyboardPanel.setPreferredSize(new Dimension(700, 300));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Row 0: 1 2 3 4 5 6 7 8 9 0
        String[] row0 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        gbc.gridy = 0;
        for (int i = 0; i < row0.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createCouponKey(row0[i], textField, parentDialog);
            keyboardPanel.add(key, gbc);
        }

        // Row 1: Q W E R T Y U I O P
        String[] row1 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
        gbc.gridy = 1;
        for (int i = 0; i < row1.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createCouponKey(row1[i], textField, parentDialog);
            keyboardPanel.add(key, gbc);
        }

        // Row 2: A S D F G H J K L
        String[] row2 = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
        gbc.gridy = 2;
        for (int i = 0; i < row2.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createCouponKey(row2[i], textField, parentDialog);
            keyboardPanel.add(key, gbc);
        }

        // Backspace button (AMBER)
        gbc.gridx = 9;
        gbc.gridwidth = 1;
        JButton backspaceBtn = createCouponKey("←", textField, parentDialog);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        backspaceBtn.setForeground(Color.WHITE);
        keyboardPanel.add(backspaceBtn, gbc);

        // Row 3: Z X C V B N M
        String[] row3 = {"Z", "X", "C", "V", "B", "N", "M"};
        gbc.gridy = 3;
        for (int i = 0; i < row3.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createCouponKey(row3[i], textField, parentDialog);
            keyboardPanel.add(key, gbc);
        }

        // Clear button (RED)
        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton clearBtn = createCouponKey("Clear", textField, parentDialog);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        keyboardPanel.add(clearBtn, gbc);

        // Row 4: Cancel, Space, and Apply buttons
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton cancelBtn = createCouponKey("Cancel", textField, parentDialog);
        cancelBtn.setBackground(new Color(108, 117, 125)); // Gray
        cancelBtn.setForeground(Color.WHITE);
        keyboardPanel.add(cancelBtn, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 5;
        JButton spaceBtn = createCouponKey("Space", textField, parentDialog);
        keyboardPanel.add(spaceBtn, gbc);

        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton applyBtn = createCouponKey("Apply", textField, parentDialog);
        applyBtn.setBackground(new Color(40, 167, 69)); // Green
        applyBtn.setForeground(Color.WHITE);
        keyboardPanel.add(applyBtn, gbc);

        return keyboardPanel;
    }

    /**
     * Apply rounded style with border to keyboard button (similar to QuickKeysPanel keyboard).
     * @param button The button to style
     * @param useBlackOutline If true, use black outline; if false, use darker shade of button color
     */
    private void applyKeyboardRoundedStyle(JButton button, boolean useBlackOutline) {
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setContentAreaFilled(false);

        button.setUI(new BasicButtonUI() {
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

                // Fill rounded rectangle background
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw outline (black for special keys, darker shade for regular keys)
                if (useBlackOutline) {
                    g2d.setColor(Color.BLACK);
                } else {
                    g2d.setColor(displayColor.darker());
                }
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw centered text
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
     * Create a keyboard key button for coupon entry.
     * @param key The key label
     * @param textField The text field to update
     * @param parentDialog The parent dialog to close when done
     * @return JButton for the key
     */
    private JButton createCouponKey(String key, JTextField textField, JDialog parentDialog) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, 16));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250)); // Light gray
        keyButton.setForeground(Color.BLACK);

        // Determine if this is a special action key
        boolean isSpecialKey = key.equals("←") || key.equals("Clear") ||
                               key.equals("Cancel") || key.equals("Apply");

        // Only apply rounded style to special keys (matching search keyboard behavior)
        if (isSpecialKey) {
            applyKeyboardRoundedStyle(keyButton, true); // Black outline for special keys
        }

        keyButton.addActionListener(e -> {
            String currentText = textField.getText();
            String newText = currentText;

            if (key.equals("Clear")) {
                newText = "";
            } else if (key.equals("←")) {
                // Backspace
                if (currentText.length() > 0) {
                    newText = currentText.substring(0, currentText.length() - 1);
                }
            } else if (key.equals("Space")) {
                newText = currentText + " ";
            } else if (key.equals("Cancel")) {
                // Close dialog without applying
                parentDialog.dispose();
                return;
            } else if (key.equals("Apply")) {
                // Apply coupon
                String code = textField.getText().trim().toUpperCase();
                if (!code.isEmpty()) {
                    parentDialog.dispose();
                    validateAndApplyCoupon(code);
                } else {
                    showWarningDialog("Empty Coupon Code", "Please enter a coupon code.");
                }
                return;
            } else {
                // Letter or number key
                newText = currentText + key;
            }

            textField.setText(newText);
        });

        return keyButton;
    }
}
