package org.possystem.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * LockScreenCarousel - Full-screen image carousel that cycles through promotional images.
 * Displays on application startup. Click anywhere to unlock and show POS interface.
 */
public class LockScreenCarousel extends JFrame {

    private static final int CYCLE_INTERVAL_MS = 10000; // 10 seconds per image
    private static final int FADE_DURATION_MS = 1000; // 1 second fade animation
    private static final int ANIMATION_FPS = 60; // Smooth 60 FPS animation

    private final String[] imagePaths = {
        "/images/carousel/gatorade.png",
        "/images/carousel/ice-bag.png",
        "/images/carousel/collage.png"
    };

    private final Image[] images;
    private int currentImageIndex = 0;
    private int nextImageIndex = 1;

    private Timer cycleTimer;
    private Timer animationTimer;
    private Timer pulseTimer;
    private CarouselPanel carouselPanel;
    private JLabel instructionLabel;
    private Runnable onUnlockCallback;
    private MouseAdapter unlockListener;

    private boolean isAnimating = false;
    private float slideProgress = 0.0f; // 0.0 to 1.0
    private float pulseProgress = 0.0f; // 0.0 to 1.0 for text pulse animation

    /**
     * Create the lock screen carousel.
     *
     * @param parentWindow Optional parent window to match position/size (null for primary screen)
     * @param onUnlockCallback Callback to execute when user taps to unlock
     */
    public LockScreenCarousel(Window parentWindow, Runnable onUnlockCallback) {
        this.onUnlockCallback = onUnlockCallback;

        System.out.println("=== LockScreenCarousel Initializing ===");

        // Determine carousel size and position based on parent
        Dimension carouselSize;
        Point carouselLocation;
        boolean shouldMaximize = false;

        if (parentWindow != null) {
            // Follow parent window (idle timeout case)
            Rectangle bounds = parentWindow.getBounds();
            carouselSize = new Dimension(bounds.width, bounds.height);
            carouselLocation = new Point(bounds.x, bounds.y);

            // Check if parent is maximized
            if (parentWindow instanceof Frame) {
                Frame frame = (Frame) parentWindow;
                shouldMaximize = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            }

            System.out.println("Following parent window: " + bounds.width + "x" + bounds.height +
                             " at (" + bounds.x + ", " + bounds.y + ")");
            if (shouldMaximize) {
                System.out.println("Parent is maximized - carousel will maximize too");
            }
        } else {
            // Startup case - use primary screen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            carouselSize = screenSize;
            carouselLocation = new Point(0, 0);
            System.out.println("Using primary screen: " + screenSize.width + "x" + screenSize.height);
        }

        // Load images (store originals for dynamic scaling)
        images = new Image[imagePaths.length];
        System.out.println("Carousel size: " + carouselSize.width + "x" + carouselSize.height);

        int loadedCount = 0;
        for (int i = 0; i < imagePaths.length; i++) {
            try {
                System.out.println("Loading image " + (i + 1) + ": " + imagePaths[i]);
                java.net.URL imgUrl = getClass().getResource(imagePaths[i]);
                if (imgUrl != null) {
                    System.out.println("  Found at: " + imgUrl);
                    ImageIcon icon = new ImageIcon(imgUrl);

                    // Wait for image to fully load
                    if (icon.getImageLoadStatus() == MediaTracker.COMPLETE || icon.getIconWidth() > 0) {
                        // Store original image (will be scaled dynamically in paintComponent)
                        images[i] = icon.getImage();
                        loadedCount++;
                        System.out.println("  Successfully loaded! (" + icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
                    } else {
                        System.err.println("  ERROR: Image did not load properly");
                    }
                } else {
                    System.err.println("  ERROR: Could not find image: " + imagePaths[i]);
                }
            } catch (Exception e) {
                System.err.println("  ERROR: Failed to load carousel image: " + imagePaths[i]);
                e.printStackTrace();
            }
        }

        System.out.println("Loaded " + loadedCount + " out of " + imagePaths.length + " images");

        if (loadedCount == 0) {
            System.err.println("WARNING: No carousel images loaded! Lock screen will be blank.");
        }

        setupWindow(carouselSize, carouselLocation, shouldMaximize);
        setupClickListener(); // Initialize unlock listener first
        setupCarouselPanel(); // This uses the unlock listener and creates footer
        startCycleTimer();
        startPulseAnimation(); // Start text pulse animation

        System.out.println("LockScreenCarousel initialized successfully");
        System.out.println("=======================================");
    }

    private void setupWindow(Dimension size, Point location, boolean shouldMaximize) {
        // Remove window decorations
        setUndecorated(true);

        // Set size and position
        setSize(size);
        setLocation(location);
        setResizable(false);
        setAlwaysOnTop(true);

        // Maximize if needed (after setting size/location)
        if (shouldMaximize) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
            System.out.println("Carousel: Configured as maximized window");
        } else {
            System.out.println("Carousel: Configured at " + size.width + "x" + size.height +
                             " at (" + location.x + ", " + location.y + ")");
        }

        // Set cursor to hand to indicate clickable
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setupCarouselPanel() {
        // Use BorderLayout to separate carousel and footer
        setLayout(new BorderLayout());

        // Carousel panel (takes up CENTER - will be ~90% of screen)
        carouselPanel = new CarouselPanel();
        carouselPanel.setBackground(Color.BLACK); // Fallback background
        carouselPanel.addMouseListener(unlockListener); // Enable click to unlock
        add(carouselPanel, BorderLayout.CENTER);

        // Footer instruction bar (takes up SOUTH - ~10% of screen)
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Create footer panel with animated instruction text
     */
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout());
        footer.setBackground(Color.BLACK); // Black background (blends with carousel)
        footer.addMouseListener(unlockListener); // Enable click to unlock on footer

        // Calculate footer height (10% of screen, minimum 80px, maximum 150px)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int footerHeight = Math.max(80, Math.min(150, screenSize.height / 10));
        footer.setPreferredSize(new Dimension(screenSize.width, footerHeight));

        // Instruction text label (will be animated)
        instructionLabel = new JLabel("Touch anywhere to continue");
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionLabel.addMouseListener(unlockListener); // Enable click to unlock on label too

        // Calculate font size based on screen resolution (1080p baseline)
        int baseFontSize = 32;
        float fontScale = screenSize.height / 1080.0f;
        int fontSize = Math.round(baseFontSize * fontScale);
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, fontSize));

        footer.add(instructionLabel, BorderLayout.CENTER);

        System.out.println("Footer created: height=" + footerHeight + "px, font=" + fontSize + "pt");

        return footer;
    }

    private void setupClickListener() {
        // Create shared mouse listener for unlock
        unlockListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                unlock();
            }
        };

        // Add listener to frame
        // Carousel panel and footer will get the listener in setupCarouselPanel
        addMouseListener(unlockListener);
    }

    private void startCycleTimer() {
        cycleTimer = new Timer(CYCLE_INTERVAL_MS, e -> slideToNextImage());
        cycleTimer.start();
    }

    /**
     * Start pulse animation for instruction text (fades 70% to 100% opacity)
     */
    private void startPulseAnimation() {
        int pulseFrameDelay = 1000 / 30; // 30 FPS for smooth pulse
        float pulseDuration = 2000.0f; // 2 seconds per pulse cycle
        float pulseIncrement = 1.0f / (pulseDuration / pulseFrameDelay);

        pulseTimer = new Timer(pulseFrameDelay, e -> {
            pulseProgress += pulseIncrement;

            if (pulseProgress >= 1.0f) {
                pulseProgress = 0.0f; // Loop back to start
            }

            // Calculate opacity using sine wave for smooth pulse (0.7 to 1.0)
            float opacity = 0.7f + (float)(Math.sin(pulseProgress * Math.PI * 2) * 0.15 + 0.15);

            // Update label color with new opacity
            if (instructionLabel != null) {
                int alpha = (int)(opacity * 255);
                instructionLabel.setForeground(new Color(255, 255, 255, alpha));
            }
        });
        pulseTimer.start();
        System.out.println("Pulse animation started for instruction text");
    }

    private void slideToNextImage() {
        if (isAnimating) return; // Prevent overlapping animations

        isAnimating = true;
        slideProgress = 0.0f;

        // Calculate next image index
        nextImageIndex = (currentImageIndex + 1) % images.length;

        // Start fade animation
        int frameDelay = 1000 / ANIMATION_FPS;
        float progressIncrement = 1.0f / (FADE_DURATION_MS / (float) frameDelay);

        animationTimer = new Timer(frameDelay, null);
        animationTimer.addActionListener(e -> {
            slideProgress += progressIncrement;

            if (slideProgress >= 1.0f) {
                slideProgress = 1.0f;
                animationTimer.stop();
                currentImageIndex = nextImageIndex;
                isAnimating = false;
            }

            carouselPanel.repaint();
        });
        animationTimer.start();
    }

    private void unlock() {
        // Stop timers
        if (cycleTimer != null) {
            cycleTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (pulseTimer != null) {
            pulseTimer.stop();
        }

        System.out.println("Carousel unlock - closing carousel");

        // Execute unlock callback
        if (onUnlockCallback != null) {
            onUnlockCallback.run();
        }
    }

    /**
     * Custom panel that renders the fade/cross-fade carousel effect
     * Images are dynamically scaled to fit the current panel size
     */
    private class CarouselPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fill background with black
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (!isAnimating) {
                // Static display - show current image
                if (images[currentImageIndex] != null) {
                    drawScaledImage(g2d, images[currentImageIndex], 1.0f);
                }
            } else {
                // Fade/Cross-fade animation - show both images with opacity

                // Draw next image first (fading in)
                if (images[nextImageIndex] != null) {
                    drawScaledImage(g2d, images[nextImageIndex], slideProgress);
                }

                // Draw current image on top (fading out)
                if (images[currentImageIndex] != null) {
                    drawScaledImage(g2d, images[currentImageIndex], 1.0f - slideProgress);
                }
            }

            g2d.dispose();
        }

        /**
         * Draw image scaled to fit panel while maintaining aspect ratio (letterboxed)
         */
        private void drawScaledImage(Graphics2D g2d, Image image, float alpha) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            // Get original image dimensions
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);

            if (imgWidth <= 0 || imgHeight <= 0) {
                return; // Image not loaded yet
            }

            // Calculate scale to fit inside panel while maintaining aspect ratio
            double scaleX = (double) panelWidth / imgWidth;
            double scaleY = (double) panelHeight / imgHeight;
            double scale = Math.min(scaleX, scaleY); // Letterbox fit

            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);

            // Center the image in the panel
            int x = (panelWidth - scaledWidth) / 2;
            int y = (panelHeight - scaledHeight) / 2;

            // Apply alpha composite for fade effect
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Draw scaled image
            g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
        }
    }
}
