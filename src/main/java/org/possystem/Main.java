package org.possystem;

import org.possystem.database.DatabaseManager;
import org.possystem.ui.LockScreenCarousel;
import org.possystem.ui.PosInterface;

import javax.swing.*;

/**
 * Main entry point for POS System
 */
public class Main {
    public static void main(String[] args) {
        // Parse command-line arguments
        String dbName = "possystemdb";      // Default
        String posName = "";                 // Default (empty means use hostname)
        int serverPort = 8080;               // Default

        for (String arg : args) {
            if (arg.startsWith("--db=")) {
                dbName = arg.substring(5);
            } else if (arg.startsWith("--name=")) {
                posName = arg.substring(7);
            } else if (arg.startsWith("--port=")) {
                try {
                    serverPort = Integer.parseInt(arg.substring(7));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + arg);
                    System.exit(1);
                }
            }
        }

        // Print configuration
        System.out.println("=== POS Configuration ===");
        System.out.println("Database: " + dbName);
        System.out.println("POS Name: " + (posName.isEmpty() ? "(using hostname)" : posName));
        System.out.println("Server Port: " + serverPort);
        System.out.println("=========================");

        // Store configuration for later use
        final String finalPosName = posName;
        final int finalServerPort = serverPort;

        // Disable Mac menu bar for full screen kiosk mode
        System.setProperty("apple.awt.fullscreenhidescursor", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "POS System");

        // Initialize database first with custom name
        DatabaseManager.initialize(dbName);

        // Launch POS Interface on Swing thread
        SwingUtilities.invokeLater(() -> {
            System.out.println("=== POS Application Starting ===");

            // Create POS interface but keep it hidden (pass config)
            System.out.println("Creating POS interface...");
            PosInterface posInterface = new PosInterface(finalPosName, finalServerPort);
            System.out.println("POS interface created (hidden)");

            // Use array wrapper to allow lambda to reference the lock screen
            final LockScreenCarousel[] lockScreenRef = new LockScreenCarousel[1];

            // Show lock screen carousel first
            System.out.println("Creating lock screen carousel...");
            lockScreenRef[0] = new LockScreenCarousel(null, () -> {
                // When user clicks to unlock
                System.out.println("Lock screen clicked - unlocking...");

                // Dispose carousel first
                if (lockScreenRef[0] != null) {
                    lockScreenRef[0].dispose();
                }

                // Wait briefly for carousel to close, then show POS
                Timer delayTimer = new Timer(100, e -> {
                    ((Timer)e.getSource()).stop();

                    System.out.println("Showing POS interface...");
                    posInterface.setVisible(true);
                    posInterface.toFront();
                    posInterface.requestFocus();
                    System.out.println("POS interface now visible");
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            });

            System.out.println("Showing lock screen carousel...");
            lockScreenRef[0].setVisible(true);
            System.out.println("Lock screen should now be visible");
            System.out.println("================================");
        });
    }
}
