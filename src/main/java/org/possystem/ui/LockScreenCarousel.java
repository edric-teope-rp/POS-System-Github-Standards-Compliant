package org.possystem.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

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

    private final BufferedImage[] images;
    private int currentImageIndex = 0;
    private int nextImageIndex = 1;

    private Timer cycleTimer;
    private Timer animationTimer;
    private CarouselPanel carouselPanel;
    private Runnable onUnlockCallback;

    private boolean isAnimating = false;
    private float slideProgress = 0.0f; // 0.0 to 1.0

    /**
     * Create the lock screen carousel.
     *
     * @param onUnlockCallback Callback to execute when user taps to unlock
     */
    public LockScreenCarousel(Runnable onUnlockCallback) {
        this.onUnlockCallback = onUnlockCallback;

        System.out.println("=== LockScreenCarousel Initializing ===");

        // Load images
        images = new BufferedImage[imagePaths.length];
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        System.out.println("Screen size: " + screenSize.width + "x" + screenSize.height);

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
                        Image originalImage = icon.getImage();

                        // Create BufferedImage and scale it
                        images[i] = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = images[i].createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Fill with black background first
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, screenSize.width, screenSize.height);

                        // Draw image scaled to fit inside screen while maintaining aspect ratio (with letterboxing)
                        int imgWidth = icon.getIconWidth();
                        int imgHeight = icon.getIconHeight();

                        double scaleX = (double) screenSize.width / imgWidth;
                        double scaleY = (double) screenSize.height / imgHeight;
                        double scale = Math.min(scaleX, scaleY); // Fit inside screen (letterbox)

                        int scaledWidth = (int) (imgWidth * scale);
                        int scaledHeight = (int) (imgHeight * scale);

                        int x = (screenSize.width - scaledWidth) / 2;
                        int y = (screenSize.height - scaledHeight) / 2;

                        g2d.drawImage(originalImage, x, y, scaledWidth, scaledHeight, null);
                        g2d.dispose();

                        loadedCount++;
                        System.out.println("  Successfully loaded and rendered! (" + imgWidth + "x" + imgHeight + ")");
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

        setupWindow(screenSize);
        setupCarouselPanel();
        setupClickListener();
        startCycleTimer();

        System.out.println("LockScreenCarousel initialized successfully");
        System.out.println("=======================================");
    }

    private void setupWindow(Dimension screenSize) {
        // Remove window decorations
        setUndecorated(true);

        // Borderless full-screen window mode (allows dialogs to work)
        setSize(screenSize);
        setLocation(0, 0);
        setResizable(false);
        setAlwaysOnTop(true);

        // Set cursor to hand to indicate clickable
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        System.out.println("Carousel: Configured as borderless full-screen window");
    }

    private void setupCarouselPanel() {
        carouselPanel = new CarouselPanel();
        carouselPanel.setBackground(Color.BLACK); // Fallback background
        add(carouselPanel);
    }

    private void setupClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                unlock();
            }
        });
    }

    private void startCycleTimer() {
        cycleTimer = new Timer(CYCLE_INTERVAL_MS, e -> slideToNextImage());
        cycleTimer.start();
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

        System.out.println("Carousel unlock - closing carousel");

        // Execute unlock callback
        if (onUnlockCallback != null) {
            onUnlockCallback.run();
        }
    }

    /**
     * Custom panel that renders the fade/cross-fade carousel effect
     */
    private class CarouselPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (!isAnimating) {
                // Static display - show current image
                if (images[currentImageIndex] != null) {
                    g2d.drawImage(images[currentImageIndex], 0, 0, null);
                }
            } else {
                // Fade/Cross-fade animation - show both images with opacity

                // Draw next image first (fading in)
                if (images[nextImageIndex] != null) {
                    float nextAlpha = slideProgress; // 0.0 to 1.0
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nextAlpha));
                    g2d.drawImage(images[nextImageIndex], 0, 0, null);
                }

                // Draw current image on top (fading out)
                if (images[currentImageIndex] != null) {
                    float currentAlpha = 1.0f - slideProgress; // 1.0 to 0.0
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));
                    g2d.drawImage(images[currentImageIndex], 0, 0, null);
                }
            }

            g2d.dispose();
        }
    }
}
