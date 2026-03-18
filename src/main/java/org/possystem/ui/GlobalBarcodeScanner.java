package org.possystem.ui;

import org.possystem.entity.PriceBook;
import org.possystem.service.PriceBookService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Global Barcode Scanner - Intercepts all keyboard input at application level
 * Works regardless of focus, open dialogs, or active UI elements
 */
public class GlobalBarcodeScanner {

    // Scanner state
    public enum ScannerState {
        ENABLED,              // Default during active transaction
        DISABLED_FINALIZED,   // After Total button (transaction finalized)
        DISABLED_DIALOG       // Temporarily disabled for specific dialogs
    }

    private ScannerState state = ScannerState.ENABLED;

    // Services and callbacks
    private final PriceBookService priceBookService;
    private final Consumer<PriceBook> onItemScanned;
    private final Runnable onScanSuccess;
    private final Consumer<String> onScanError;

    // Scan detection
    private final StringBuilder scanBuffer = new StringBuilder();
    private Timer scanCompleteTimer;
    private static final int SCAN_COMPLETE_DELAY_MS = 100;  // Time to wait after typing stops

    // Duplicate prevention
    private String lastScannedUPC = "";
    private long lastScanTime = 0;
    private static final int DUPLICATE_SCAN_WINDOW_MS = 800;

    // KeyEventDispatcher for global keyboard interception
    private final KeyEventDispatcher keyEventDispatcher;

    public GlobalBarcodeScanner(PriceBookService priceBookService,
                                Consumer<PriceBook> onItemScanned,
                                Runnable onScanSuccess,
                                Consumer<String> onScanError) {
        this.priceBookService = priceBookService;
        this.onItemScanned = onItemScanned;
        this.onScanSuccess = onScanSuccess;
        this.onScanError = onScanError;

        // Timer to detect when scan is complete (no more keys for 100ms)
        scanCompleteTimer = new Timer(SCAN_COMPLETE_DELAY_MS, e -> processScan());
        scanCompleteTimer.setRepeats(false);

        // Global keyboard event dispatcher
        keyEventDispatcher = this::handleKeyEvent;
    }

    /**
     * Start intercepting keyboard events globally
     */
    public void start() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(keyEventDispatcher);
    }

    /**
     * Stop intercepting keyboard events
     */
    public void stop() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .removeKeyEventDispatcher(keyEventDispatcher);
        scanCompleteTimer.stop();
    }

    /**
     * Global keyboard event handler
     * Returns true to consume the event, false to pass it through
     */
    private boolean handleKeyEvent(KeyEvent e) {
        // Only process KEY_TYPED events (actual characters)
        if (e.getID() != KeyEvent.KEY_TYPED) {
            return false; // Pass through KEY_PRESSED and KEY_RELEASED
        }

        // Check if scanner is enabled
        if (state != ScannerState.ENABLED) {
            return false; // Pass through - scanner disabled
        }

        char keyChar = e.getKeyChar();

        // Check for Enter key (scan complete)
        if (keyChar == '\n' || keyChar == '\r') {
            // Process accumulated scan immediately
            scanCompleteTimer.stop();
            processScan();
            return true; // Consume Enter key
        }

        // Check if it's a printable character (letters, numbers, some symbols)
        if (Character.isLetterOrDigit(keyChar) || keyChar == '-' || keyChar == '_') {
            scanBuffer.append(keyChar);
            scanCompleteTimer.restart(); // Reset timer with each new character
            return true; // Consume the character
        }

        // For other characters, pass through (allow normal UI interaction)
        return false;
    }

    /**
     * Process the scanned barcode
     */
    private void processScan() {
        String scannedUPC = scanBuffer.toString().trim();
        scanBuffer.setLength(0); // Clear buffer

        // Ignore empty scans
        if (scannedUPC.isEmpty()) {
            return;
        }

        // Duplicate prevention
        long currentTime = System.currentTimeMillis();
        if (scannedUPC.equals(lastScannedUPC) &&
            (currentTime - lastScanTime) < DUPLICATE_SCAN_WINDOW_MS) {
            // Duplicate scan detected - ignore it
            return;
        }

        // Update last scan tracking
        lastScannedUPC = scannedUPC;
        lastScanTime = currentTime;

        // Lookup product in price book
        try {
            Optional<PriceBook> result = priceBookService.getItemByUpc(scannedUPC);
            if (result.isPresent()) {
                // Product found - notify callback
                PriceBook item = result.get();
                SwingUtilities.invokeLater(() -> {
                    onItemScanned.accept(item);
                    if (onScanSuccess != null) {
                        onScanSuccess.run();
                    }
                });
            } else {
                // Product not found - notify error callback
                SwingUtilities.invokeLater(() -> {
                    if (onScanError != null) {
                        onScanError.accept(scannedUPC);
                    }
                });
            }
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> {
                if (onScanError != null) {
                    onScanError.accept("Database error: " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Set the scanner state
     */
    public void setState(ScannerState newState) {
        this.state = newState;
        // Clear buffer when disabling
        if (newState != ScannerState.ENABLED) {
            scanBuffer.setLength(0);
            scanCompleteTimer.stop();
        }
    }

    /**
     * Get current scanner state
     */
    public ScannerState getState() {
        return state;
    }

    /**
     * Enable the scanner (set to ENABLED state)
     */
    public void enable() {
        setState(ScannerState.ENABLED);
    }

    /**
     * Disable the scanner temporarily (for dialogs)
     */
    public void disableForDialog() {
        setState(ScannerState.DISABLED_DIALOG);
    }

    /**
     * Disable the scanner for finalized transaction
     */
    public void disableFinalized() {
        setState(ScannerState.DISABLED_FINALIZED);
    }

    /**
     * Check if scanner is currently enabled
     */
    public boolean isEnabled() {
        return state == ScannerState.ENABLED;
    }
}
