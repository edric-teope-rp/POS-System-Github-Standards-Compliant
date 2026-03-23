package org.possystem.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * ToastNotification - Non-intrusive popup notification for promotional discounts.
 * Appears in upper-right corner, auto-dismisses after 3 seconds.
 */
public class ToastNotification extends JWindow {

    private static final int TOAST_WIDTH = 400;
    private static final int TOAST_HEIGHT = 80;
    private static final int MARGIN = 20;
    private static final int DISPLAY_DURATION_MS = 3000; // 3 seconds
    private static final int ANIMATION_DURATION_MS = 200; // Slide-in animation

    private Timer dismissTimer;
    private Timer animationTimer;
    private int targetX;
    private int currentX;

    /**
     * Create and show a toast notification.
     *
     * @param parentWindow The parent window to position relative to
     * @param title        The notification title (e.g., "Buy 2 or More Get 25% Off!")
     * @param message      The notification message (e.g., "You saved $0.99")
     */
    public ToastNotification(Window parentWindow, String title, String message) {
        super(parentWindow);

        // Calculate position (upper-right corner)
        Rectangle parentBounds = parentWindow.getBounds();
        targetX = parentBounds.x + parentBounds.width - TOAST_WIDTH - MARGIN;
        int y = parentBounds.y + MARGIN;

        // Start off-screen to the right for slide-in animation
        currentX = parentBounds.x + parentBounds.width;

        setBounds(currentX, y, TOAST_WIDTH, TOAST_HEIGHT);
        setAlwaysOnTop(true);

        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 5));
        contentPanel.setBackground(new Color(212, 237, 218)); // Light green
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 167, 69), 2), // Green border
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Success icon (checkmark)
        JLabel iconLabel = new JLabel("\u2713"); // Checkmark character
        iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        iconLabel.setForeground(new Color(40, 167, 69)); // Green
        contentPanel.add(iconLabel, BorderLayout.WEST);

        // Message panel
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(20, 100, 40)); // Dark green
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(titleLabel);

        messagePanel.add(Box.createVerticalStrut(3));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(20, 100, 40)); // Dark green
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(messageLabel);

        contentPanel.add(messagePanel, BorderLayout.CENTER);

        add(contentPanel);

        // Start slide-in animation
        startSlideInAnimation();

        // Auto-dismiss after duration
        dismissTimer = new Timer(DISPLAY_DURATION_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fadeOut();
            }
        });
        dismissTimer.setRepeats(false);
        dismissTimer.start();

        setVisible(true);
    }

    /**
     * Slide-in animation from right.
     */
    private void startSlideInAnimation() {
        final int steps = 10;
        final int stepDelay = ANIMATION_DURATION_MS / steps;
        final int stepDistance = (currentX - targetX) / steps;

        animationTimer = new Timer(stepDelay, new ActionListener() {
            int step = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                if (step <= steps) {
                    currentX -= stepDistance;
                    setLocation(currentX, getY());
                } else {
                    // Ensure we land exactly on target
                    setLocation(targetX, getY());
                    animationTimer.stop();
                }
            }
        });
        animationTimer.start();
    }

    /**
     * Fade out and dispose.
     */
    private void fadeOut() {
        // Simple fade: just dispose immediately (Swing doesn't support transparency animation easily)
        dispose();
    }

    /**
     * Dispose of timers and window.
     */
    @Override
    public void dispose() {
        if (dismissTimer != null) {
            dismissTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.dispose();
    }

    /**
     * Static helper to show a toast notification.
     *
     * @param parentWindow The parent window
     * @param title        Notification title
     * @param message      Notification message
     */
    public static void showToast(Window parentWindow, String title, String message) {
        SwingUtilities.invokeLater(() -> new ToastNotification(parentWindow, title, message));
    }
}
