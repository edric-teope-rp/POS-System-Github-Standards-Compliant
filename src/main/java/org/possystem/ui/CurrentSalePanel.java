package org.possystem.ui;

import org.possystem.entity.TransactionItem;
import org.possystem.entity.TransactionDiscount;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Current Sale Panel - Shopping cart with table, totals, and item management
 */
public class CurrentSalePanel extends JPanel {

    private final TransactionService transactionService;
    private final Runnable onSelectionChanged;

    private boolean editingEnabled = true; // Track if editing is allowed
    private JTable saleTable;
    private SaleTableModel saleTableModel;
    private JLabel subtotalLabel;
    private JPanel discountsPanel; // Container for discount labels
    private JLabel taxLabel;
    private JLabel totalLabel;

    public static final int MIN_WIDTH = 400;

    // Calculate width as 35% of screen width
    private static int calculateWidth() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int calculatedWidth = (int) (screenSize.width * 0.35);
        // Ensure minimum of 400px
        return Math.max(calculatedWidth, MIN_WIDTH);
    }

    public static final int DEFAULT_WIDTH = calculateWidth();
    public static final int MAX_WIDTH = DEFAULT_WIDTH; // Same as default for fixed panel

    public CurrentSalePanel(TransactionService transactionService, Runnable onSelectionChanged) {
        this.transactionService = transactionService;
        this.onSelectionChanged = onSelectionChanged;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Current Sale"));
        // Fixed width panel - no resizing, but responsive to screen size (35% of screen width)
        setPreferredSize(new Dimension(DEFAULT_WIDTH, 0));
        setMinimumSize(new Dimension(DEFAULT_WIDTH, 0));
        setMaximumSize(new Dimension(DEFAULT_WIDTH, Integer.MAX_VALUE));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // Create custom table model
        saleTableModel = new SaleTableModel();
        saleTable = new JTable(saleTableModel);
        saleTable.setRowHeight(45);
        saleTable.setFont(new Font("Arial", Font.PLAIN, 16));  // Increase table font size
        saleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));  // Increase header font size
        saleTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        saleTable.getTableHeader().setReorderingAllowed(false);

        // Enable row selection
        saleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        saleTable.setRowSelectionAllowed(true);

        // Add selection listener for row selection
        saleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        });

        // Set column widths (proportional to 35% screen width)
        saleTable.getColumnModel().getColumn(0).setPreferredWidth(408); // Name
        saleTable.getColumnModel().getColumn(1).setPreferredWidth(70); // Qty
        saleTable.getColumnModel().getColumn(1).setMinWidth(60);
        saleTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Price
        saleTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Line Total

        // Set custom renderers
        saleTable.getColumnModel().getColumn(0).setCellRenderer(new DiscountAwareTextRenderer());
        saleTable.getColumnModel().getColumn(1).setCellRenderer(new QuantityControlRenderer());
        saleTable.getColumnModel().getColumn(2).setCellRenderer(new DiscountAwareTextRenderer());
        saleTable.getColumnModel().getColumn(3).setCellRenderer(new DiscountAwareTextRenderer());

        // Totals Display
        subtotalLabel = new JLabel("Subtotal: $0.00");
        subtotalLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        // Discounts panel - will hold dynamic discount labels
        discountsPanel = new JPanel();
        discountsPanel.setLayout(new BoxLayout(discountsPanel, BoxLayout.Y_AXIS));
        discountsPanel.setOpaque(false);

        taxLabel = new JLabel("Tax (7%): $0.00");
        taxLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 22));
    }

    private void layoutComponents() {
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(saleTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Totals panel at bottom - using BoxLayout for dynamic rows
        JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout(new BoxLayout(totalsPanel, BoxLayout.Y_AXIS));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add components in order: Subtotal, Discounts (dynamic), Tax, Total
        subtotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalsPanel.add(subtotalLabel);

        discountsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalsPanel.add(discountsPanel);

        taxLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalsPanel.add(taxLabel);

        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalsPanel.add(totalLabel);

        add(totalsPanel, BorderLayout.SOUTH);
    }

    public void refreshDisplay() {
        try {
            List<TransactionItem> items = transactionService.getCurrentSaleItems();
            List<TransactionDiscount> activeDiscounts = transactionService.getActiveDiscounts();

            // Build a map of item_id to promotional discount
            Map<Integer, TransactionDiscount> itemDiscountMap = new HashMap<>();
            for (TransactionDiscount discount : activeDiscounts) {
                if (discount.discountType().equals("PROMOTIONAL") && discount.itemId() != null) {
                    itemDiscountMap.put(discount.itemId(), discount);
                }
            }

            // Build table rows: items + their promotional discounts inline
            List<TableRow> tableRows = new ArrayList<>();
            for (TransactionItem item : items) {
                if (item.status().equals("ACTIVE")) {
                    // Add item row
                    tableRows.add(new TableRow(item));

                    // If item has a promotional discount, add discount row right after it
                    TransactionDiscount discount = itemDiscountMap.get(item.id());
                    if (discount != null) {
                        // Calculate percentage (assume discount is percentage-based from API)
                        double percentage = (Math.abs(discount.discountAmount()) / item.subtotal()) * 100;
                        String description = String.format("  Buy %d+ Save %.0f%%", item.quantity(), percentage);
                        tableRows.add(new TableRow(discount, description));
                    }
                }
            }

            saleTableModel.setRows(tableRows);

            // Get items subtotal (before discounts)
            double itemsSubtotal = transactionService.getTransactionSubtotal();
            subtotalLabel.setText(String.format("Subtotal: $%.2f", itemsSubtotal));

            // Clear and rebuild discounts panel (only non-promotional discounts)
            discountsPanel.removeAll();

            // Display only Senior/Veteran/Coupon discounts in totals panel
            for (TransactionDiscount discount : activeDiscounts) {
                String discountText = "";
                if (discount.discountType().equals("SENIOR")) {
                    discountText = String.format("Senior Discount (5%%): -$%.2f", Math.abs(discount.discountAmount()));
                } else if (discount.discountType().equals("VETERAN")) {
                    discountText = String.format("Veteran Discount (10%%): -$%.2f", Math.abs(discount.discountAmount()));
                } else if (discount.discountType().equals("COUPON")) {
                    // Get the coupon code for this discount
                    String couponCode = transactionService.getCouponCodeForDiscount(discount.id());
                    if (couponCode != null && !couponCode.isEmpty()) {
                        discountText = String.format("Coupon (%s): -$%.2f", couponCode, Math.abs(discount.discountAmount()));
                    } else {
                        discountText = String.format("Coupon Discount: -$%.2f", Math.abs(discount.discountAmount()));
                    }
                }
                // Skip PROMOTIONAL - they're now shown in the table

                if (!discountText.isEmpty()) {
                    JLabel discountLabel = new JLabel(discountText);
                    discountLabel.setFont(new Font("Arial", Font.ITALIC, 18));
                    discountLabel.setForeground(new Color(100, 100, 100)); // Gray color
                    discountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    discountsPanel.add(discountLabel);
                }
            }

            // Get discounted subtotal (includes discount amounts)
            double discountedSubtotal = transactionService.getSubtotal();

            // Calculate tax on discounted subtotal
            double tax = discountedSubtotal * 0.07;
            double total = discountedSubtotal + tax;

            taxLabel.setText(String.format("Tax (7%%): $%.2f", tax));
            totalLabel.setText(String.format("Total: $%.2f", total));

            // Refresh the UI
            discountsPanel.revalidate();
            discountsPanel.repaint();

        } catch (SQLException e) {
            showError("Failed to refresh display: " + e.getMessage());
        }
    }

    public Integer getRowSelectedItemId() {
        int selectedRow = saleTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < saleTableModel.getRowCount()) {
            TransactionItem item = saleTableModel.getItemAt(selectedRow);
            if (item != null) {
                return item.id();
            }
        }
        return null;
    }

    public boolean hasRowSelection() {
        return saleTable.getSelectedRow() >= 0;
    }

    public void setEditingEnabled(boolean enabled) {
        this.editingEnabled = enabled;

        // Disable/enable the table
        saleTable.setEnabled(enabled);

        // Reset cursor to default when disabled
        if (!enabled) {
            saleTable.setCursor(Cursor.getDefaultCursor());
        }

        // Repaint to show visual changes
        saleTable.repaint();
    }

    private void showError(String message) {
        showErrorDialog("Error", "System Error", message);
    }

    private void showErrorDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.25);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog errorDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        errorDialog.setUndecorated(true);
        errorDialog.setResizable(false);
        errorDialog.setSize(dialogWidth, dialogHeight);
        errorDialog.setMinimumSize(new Dimension(320, 250));
        errorDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
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

        JLabel detailsLabel = new JLabel("<html><div style='text-align: center;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
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

        // Apply rounded style (simple version without custom UI for now)
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        okButton.setFocusPainted(false);

        okButton.addActionListener(e -> errorDialog.dispose());

        buttonPanel.add(okButton);

        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        errorDialog.setVisible(true);
    }

    private boolean showConfirmDialog(String title, String message, String details) {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonHeight = Math.round(50 * scaleFactor);

        JDialog confirmDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        confirmDialog.setUndecorated(true);
        confirmDialog.setResizable(false);
        confirmDialog.setSize(400, 280);
        confirmDialog.setMinimumSize(new Dimension(350, 280));
        confirmDialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header with warning color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 193, 7));  // Amber/warning color
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

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
                    confirmDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });


        // Details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel detailsLabel = new JLabel("<html><center>" + details + "</center></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detailsPanel.add(detailsLabel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        final boolean[] result = {false};

        JButton noButton = new JButton("No");
        noButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        noButton.setPreferredSize(new Dimension(0, buttonHeight));
        noButton.setBackground(new Color(220, 53, 69));  // Red
        noButton.setForeground(Color.WHITE);
        noButton.addActionListener(e -> {
            result[0] = false;
            confirmDialog.dispose();
        });
        applyRoundedStyle(noButton);

        JButton yesButton = new JButton("Yes");
        yesButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        yesButton.setPreferredSize(new Dimension(0, buttonHeight));
        yesButton.setBackground(new Color(40, 167, 69));  // Green
        yesButton.setForeground(Color.WHITE);
        yesButton.addActionListener(e -> {
            result[0] = true;
            confirmDialog.dispose();
        });
        applyRoundedStyle(yesButton);

        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        confirmDialog.add(mainPanel);
        confirmDialog.setVisible(true);

        return result[0];
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

    // ==== Row Wrapper for Table ====
    private static class TableRow {
        enum Type { ITEM, DISCOUNT }

        private final Type type;
        private final TransactionItem item;
        private final TransactionDiscount discount;
        private final String discountDescription;

        // Constructor for item rows
        public TableRow(TransactionItem item) {
            this.type = Type.ITEM;
            this.item = item;
            this.discount = null;
            this.discountDescription = null;
        }

        // Constructor for discount rows
        public TableRow(TransactionDiscount discount, String description) {
            this.type = Type.DISCOUNT;
            this.item = null;
            this.discount = discount;
            this.discountDescription = description;
        }

        public Type getType() { return type; }
        public TransactionItem getItem() { return item; }
        public TransactionDiscount getDiscount() { return discount; }
        public String getDiscountDescription() { return discountDescription; }
    }

    // ==== Custom Table Model ====
    private class SaleTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Item Name", "Qty", "Price", "Line Total"};
        private List<TableRow> rows = new ArrayList<>();

        public void setRows(List<TableRow> newRows) {
            this.rows = new ArrayList<>(newRows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row >= rows.size()) return null;
            TableRow tableRow = rows.get(row);

            if (tableRow.getType() == TableRow.Type.ITEM) {
                TransactionItem item = tableRow.getItem();
                return switch (column) {
                    case 0 -> item.name();
                    case 1 -> item;
                    case 2 -> String.format("$%.2f", item.unitPrice());
                    case 3 -> String.format("$%.2f", item.subtotal());
                    default -> null;
                };
            } else {
                // Discount row
                TransactionDiscount discount = tableRow.getDiscount();
                return switch (column) {
                    case 0 -> tableRow.getDiscountDescription();
                    case 1 -> "";
                    case 2 -> "";
                    case 3 -> String.format("-$%.2f", Math.abs(discount.discountAmount()));
                    default -> null;
                };
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 1) return Object.class;
            return String.class;
        }

        public TransactionItem getItemAt(int row) {
            if (row >= rows.size()) return null;
            TableRow tableRow = rows.get(row);
            if (tableRow.getType() == TableRow.Type.ITEM) {
                return tableRow.getItem();
            }
            return null;
        }

        public boolean isDiscountRow(int row) {
            if (row >= rows.size()) return false;
            return rows.get(row).getType() == TableRow.Type.DISCOUNT;
        }
    }

    // ==== Custom Cell Renderers ====
    private class DiscountAwareTextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Check if this is a discount row
            if (saleTableModel.isDiscountRow(row)) {
                setFont(new Font("Arial", Font.ITALIC, 14));
                setForeground(new Color(100, 100, 100)); // Gray

                // Override selection colors for discount rows
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
            } else {
                // Regular item row
                setFont(new Font("Arial", Font.PLAIN, 16));
                if (isSelected) {
                    setForeground(table.getSelectionForeground());
                    setBackground(table.getSelectionBackground());
                } else {
                    setForeground(table.getForeground());
                    setBackground(table.getBackground());
                }
            }

            return c;
        }
    }

    private class QuantityControlRenderer extends JPanel implements TableCellRenderer {
        private final JLabel qtyLabel;

        public QuantityControlRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
            qtyLabel = new JLabel("1");
            qtyLabel.setPreferredSize(new Dimension(40, 30));
            qtyLabel.setHorizontalAlignment(JLabel.CENTER);
            qtyLabel.setFont(new Font("Arial", Font.PLAIN, 16));

            add(qtyLabel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // Check if this is a discount row
            if (saleTableModel.isDiscountRow(row)) {
                qtyLabel.setText("");
            } else if (value instanceof TransactionItem item) {
                qtyLabel.setText(String.valueOf(item.quantity()));
            }

            // Match table cell background colors
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                qtyLabel.setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                qtyLabel.setForeground(table.getForeground());
            }

            return this;
        }
    }

}
