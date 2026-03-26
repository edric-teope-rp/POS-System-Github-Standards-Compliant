package org.possystem.service;

import org.possystem.dao.UserDao;
import org.possystem.entity.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service class for authentication and session management.
 * Handles user login, logout, and session timeout.
 */
public class AuthService {

    private static final int SESSION_TIMEOUT_MINUTES = 2;

    private final UserDao userDao;
    private String currentUsername = null;
    private LocalDateTime lastActivityTime = null;

    public AuthService() {
        this.userDao = new UserDao();
    }

    /**
     * Attempt to log in a user with username and password.
     * @param username Username
     * @param password Plain text password
     * @return true if login successful
     */
    public boolean login(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);

        if (userOpt.isEmpty()) {
            return false; // User not found
        }

        User user = userOpt.get();

        // Verify password
        if (!UserDao.verifyPassword(password, user.passwordHash())) {
            return false; // Wrong password
        }

        // Login successful
        currentUsername = username;
        lastActivityTime = LocalDateTime.now();

        // Update last login timestamp in database
        userDao.updateLastLogin(username);

        return true;
    }

    /**
     * Log out the current user.
     */
    public void logout() {
        currentUsername = null;
        lastActivityTime = null;
    }

    /**
     * Check if a user is currently authenticated.
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return currentUsername != null && !isSessionExpired();
    }

    /**
     * Check if the current session has expired.
     * @return true if session expired
     */
    public boolean isSessionExpired() {
        if (lastActivityTime == null) {
            return true;
        }

        long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActivityTime, LocalDateTime.now());
        return minutesSinceActivity >= SESSION_TIMEOUT_MINUTES;
    }

    /**
     * Refresh the session by updating last activity time.
     * Call this when user interacts with authenticated features.
     */
    public void refreshSession() {
        if (currentUsername != null) {
            lastActivityTime = LocalDateTime.now();
        }
    }

    /**
     * Get the currently logged-in username.
     * @return Username or null if not logged in
     */
    public String getCurrentUsername() {
        if (isAuthenticated()) {
            return currentUsername;
        }
        return null;
    }

    /**
     * Check authentication and auto-logout if session expired.
     * @return true if authenticated and session valid
     */
    public boolean checkAuthAndRefresh() {
        if (isAuthenticated()) {
            refreshSession();
            return true;
        } else {
            logout(); // Clean up expired session
            return false;
        }
    }
}
