package org.possystem.ui;

import org.possystem.socket.RemoteJournalEntry;
import org.possystem.socket.SocketService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Pinned Live Journal Viewer Window
 * Independent window that shows real-time journal entries from all POS systems
 * Stays visible even when Socket Configuration Dialog is closed
 */
public class PinnedJournalViewerWindow extends JDialog {
    private final SocketService socketService;
    private JTextArea journalViewer;
    private JScrollPane journalScrollPane;

    // Dynamic font scaling based on screen resolution
    private final float fontScale;
    private final int titleFontSize;

    // Header drag functionality
    private Point mouseDownPoint = null;

    public PinnedJournalViewerWindow(Frame parent, SocketService socketService) {
        super(parent, "Live Journal Viewer (Pinned)", Dialog.ModalityType.MODELESS);
        this.socketService = socketService;
        System.out.println("DEBUG: PinnedJournalViewerWindow constructor started");

        // Calculate font scaling based on screen resolution
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

        titleFontSize = Math.round(13 * fontScale);

        initializeWindow();
        createUI();
        wireListeners();

        System.out.println("DEBUG: PinnedJournalViewerWindow constructor completed");
    }

    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);  // Changed to HIDE instead of DISPOSE
        setUndecorated(true);

        // Match Current Sale zone width (42% of screen width, minimum 400px)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int currentSaleWidth = Math.max((int) (screenSize.width * 0.42), 400);

        // Match Actions zone height (40% of content area after 50px header)
        int headerHeight = 50;
        int contentAreaHeight = screenSize.height - headerHeight;
        int actionsZoneHeight = (int) (contentAreaHeight * 0.40);

        setSize(currentSaleWidth, actionsZoneHeight);
        setLocationRelativeTo(null);

        // Not always on top by default - will be behind Socket Config Dialog but in front of POS Interface
        setAlwaysOnTop(false);

        System.out.println("DEBUG: PinnedJournalViewerWindow initialized with width=" + currentSaleWidth + " (Current Sale width), height=" + actionsZoneHeight + " (Actions zone height)");
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(40, 167, 69), 2));

        // Header with title and controls
        JPanel header = createHeader();
        mainPanel.add(header, BorderLayout.NORTH);

        // Toolbar with action buttons
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.SOUTH);

        // Journal text area
        journalViewer = new JTextArea();
        journalViewer.setEditable(false);
        journalViewer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        journalViewer.setBackground(Color.BLACK);
        journalViewer.setForeground(Color.WHITE);
        journalScrollPane = new JScrollPane(journalViewer);
        mainPanel.add(journalScrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 167, 69));  // Green
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Make header draggable
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDownPoint = null;
            }
        });

        header.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDownPoint != null) {
                    Point currentScreenLocation = e.getLocationOnScreen();
                    setLocation(
                        currentScreenLocation.x - mouseDownPoint.x,
                        currentScreenLocation.y - mouseDownPoint.y
                    );
                }
            }
        });

        // Title
        JLabel titleLabel = new JLabel("📊 Live Journal Viewer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, Math.round(18 * fontScale)));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        // Close button
        JButton closeButton = new JButton("✕");
        closeButton.setFont(new Font("Arial", Font.BOLD, Math.round(18 * fontScale)));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(40, 167, 69));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            System.out.println("DEBUG: Close button clicked on PinnedJournalViewerWindow");
            setVisible(false);
        });

        // Hover effect for close button
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(new Color(220, 53, 69));  // Red on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(new Color(40, 167, 69));  // Back to green
            }
        });

        header.add(closeButton, BorderLayout.EAST);

        return header;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setBackground(new Color(245, 245, 245));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Always On Top Toggle
        JCheckBox alwaysOnTopCheckbox = new JCheckBox("Always On Top", false);
        alwaysOnTopCheckbox.setFont(new Font("Arial", Font.PLAIN, titleFontSize));
        alwaysOnTopCheckbox.setFocusPainted(false);
        alwaysOnTopCheckbox.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheckbox.isSelected()));
        toolbar.add(alwaysOnTopCheckbox);

        // Clear Button
        JButton clearButton = new JButton("Clear");
        clearButton.setBackground(new Color(220, 53, 69));  // Red
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(clearButton);
        clearButton.addActionListener(e -> journalViewer.setText(""));
        toolbar.add(clearButton);

        // Export Button
        JButton exportButton = new JButton("Export");
        exportButton.setBackground(new Color(23, 162, 184));  // Teal
        exportButton.setForeground(Color.WHITE);
        exportButton.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        applyRoundedStyle(exportButton);
        exportButton.addActionListener(e -> exportJournal());
        toolbar.add(exportButton);

        return toolbar;
    }

    private void wireListeners() {
        // Journal entry listener - receive real-time updates
        socketService.addJournalListener(entry -> SwingUtilities.invokeLater(() -> {
            appendJournalEntry(entry);
        }));
    }

    /**
     * Append journal entry with color coding
     */
    private void appendJournalEntry(RemoteJournalEntry entry) {
        String formattedEntry = entry.toDisplayString() + "\n";
        journalViewer.append(formattedEntry);

        // Auto-scroll to bottom
        journalViewer.setCaretPosition(journalViewer.getDocument().getLength());
    }

    /**
     * Export journal to file
     */
    private void exportJournal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Journal");
        fileChooser.setSelectedFile(new File("journal-export-" + System.currentTimeMillis() + ".txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(journalViewer.getText());
                showSuccessDialog(
                    "Export Successful",
                    "Journal exported successfully!",
                    "File saved to: " + file.getAbsolutePath()
                );
            } catch (IOException e) {
                showErrorDialog(
                    "Export Failed",
                    "Failed to export journal",
                    "Error: " + e.getMessage()
                );
            }
        }
    }

    /**
     * Apply rounded button style
     */
    private void applyRoundedStyle(JButton button) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    // Personalized dialogs
    private void showSuccessDialog(String title, String message, String details) {
        showPersonalizedDialog(title, message, details, new Color(40, 167, 69));  // Green
    }

    private void showErrorDialog(String title, String message, String details) {
        showPersonalizedDialog(title, message, details, new Color(220, 53, 69));  // Red
    }

    private void showPersonalizedDialog(String title, String message, String details, Color headerColor) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerColor);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.BOLD, Math.round(16 * fontScale)));
        messageLabel.setForeground(Color.WHITE);
        header.add(messageLabel, BorderLayout.CENTER);

        Point dialogMouseDownPoint = new Point();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dialogMouseDownPoint.setLocation(e.getPoint());
            }
        });
        header.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentScreenLocation = e.getLocationOnScreen();
                dialog.setLocation(
                    currentScreenLocation.x - dialogMouseDownPoint.x,
                    currentScreenLocation.y - dialogMouseDownPoint.y
                );
            }
        });

        dialog.add(header, BorderLayout.NORTH);

        // Details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html>" + details.replace("\n", "<br>") + "</html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, Math.round(14 * fontScale)));
        detailsPanel.add(detailsLabel);

        dialog.add(detailsPanel, BorderLayout.CENTER);

        // Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(headerColor);
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, Math.round(14 * fontScale)));
        applyRoundedStyle(okButton);
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
