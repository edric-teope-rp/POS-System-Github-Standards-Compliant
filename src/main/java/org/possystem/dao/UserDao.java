package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User operations.
 * Handles user authentication and management with SHA-256 password hashing.
 */
public class UserDao {

    /**
     * Insert a new user with hashed password.
     * @param username Username
     * @param password Plain text password (will be hashed)
     * @param role User role (e.g., "MANAGER")
     */
    public void insert(String username, String password, String role) throws SQLException {
        String hashedPassword = hashPassword(password);
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.setString(2, hashedPassword);
        stmt.setString(3, role);
        stmt.executeUpdate();
    }

    /**
     * Find a user by username.
     * @param username Username to search for
     * @return Optional containing User if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, role, created_at, last_login FROM users WHERE username = ?";

        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null
            );
            return Optional.of(user);
        }

        return Optional.empty();
    }

    /**
     * Update last login timestamp for a user.
     * @param username Username
     */
    public void updateLastLogin(String username) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";

        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.executeUpdate();
    }

    /**
     * Get all users.
     * @return List of all users
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, password_hash, role, created_at, last_login FROM users";
        List<User> users = new ArrayList<>();

        Connection conn = DatabaseManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null
            );
            users.add(user);
        }

        return users;
    }

    /**
     * Hash a password using SHA-256.
     * @param password Plain text password
     * @return Hashed password as hexadecimal string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Verify a password against a hash.
     * @param password Plain text password to verify
     * @param hash Stored password hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hash) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(hash);
    }
}
