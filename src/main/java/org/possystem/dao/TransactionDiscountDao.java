package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.TransactionDiscount;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for TransactionDiscount entity.
 * Handles all database operations for the transaction_discounts table.
 */
public class TransactionDiscountDao {

    /**
     * Insert a new discount record into the database.
     * @param discount The discount to insert
     * @return The generated ID of the inserted discount
     * @throws SQLException if database operation fails
     */
    public int insert(TransactionDiscount discount) throws SQLException {
        String sql = """
                INSERT INTO transaction_discounts
                (transaction_id, discount_type, discount_amount, item_id, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, discount.transactionId());
        stmt.setString(2, discount.discountType());
        stmt.setDouble(3, discount.discountAmount());

        // Handle nullable item_id
        if (discount.itemId() != null) {
            stmt.setInt(4, discount.itemId());
        } else {
            stmt.setNull(4, Types.INTEGER);
        }

        stmt.setString(5, discount.status());
        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }
        return -1;
    }

    /**
     * Find all active discounts for a transaction.
     * @param transactionId The transaction ID
     * @return List of active discounts
     * @throws SQLException if database operation fails
     */
    public List<TransactionDiscount> findActiveByTransactionId(int transactionId) throws SQLException {
        String sql = "SELECT * FROM transaction_discounts WHERE transaction_id = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        ResultSet rs = stmt.executeQuery();

        List<TransactionDiscount> discounts = new ArrayList<>();
        while (rs.next()) {
            discounts.add(mapResultSetToDiscount(rs));
        }
        return discounts;
    }

    /**
     * Find active discount by transaction ID and type.
     * @param transactionId The transaction ID
     * @param discountType The discount type ('SENIOR', 'VETERAN', etc.)
     * @return The discount if found, null otherwise
     * @throws SQLException if database operation fails
     */
    public TransactionDiscount findActiveByTransactionIdAndType(int transactionId, String discountType) throws SQLException {
        String sql = "SELECT * FROM transaction_discounts WHERE transaction_id = ? AND discount_type = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        stmt.setString(2, discountType);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return mapResultSetToDiscount(rs);
        }
        return null;
    }

    /**
     * Find active discounts linked to a specific item.
     * @param itemId The transaction item ID
     * @return List of active discounts linked to the item
     * @throws SQLException if database operation fails
     */
    public List<TransactionDiscount> findActiveByItemId(int itemId) throws SQLException {
        String sql = "SELECT * FROM transaction_discounts WHERE item_id = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, itemId);
        ResultSet rs = stmt.executeQuery();

        List<TransactionDiscount> discounts = new ArrayList<>();
        while (rs.next()) {
            discounts.add(mapResultSetToDiscount(rs));
        }
        return discounts;
    }

    /**
     * Update the status of a discount (for soft delete).
     * @param id The discount ID
     * @param status The new status ('ACTIVE' or 'REMOVED')
     * @throws SQLException if database operation fails
     */
    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE transaction_discounts SET status = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status);
        stmt.setInt(2, id);
        stmt.executeUpdate();
    }

    /**
     * Update the discount amount for a specific discount.
     * Used when recalculating percentage-based discounts after cart changes.
     * @param id The discount ID
     * @param newAmount The new discount amount
     * @throws SQLException if database operation fails
     */
    public void updateDiscountAmount(int id, double newAmount) throws SQLException {
        String sql = "UPDATE transaction_discounts SET discount_amount = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, newAmount);
        stmt.setInt(2, id);
        stmt.executeUpdate();
    }

    /**
     * Delete all active discounts of a specific type for a transaction.
     * Used when switching between Senior/Veteran (mutually exclusive).
     * @param transactionId The transaction ID
     * @param discountType The discount type to remove
     * @throws SQLException if database operation fails
     */
    public void deleteByTransactionIdAndType(int transactionId, String discountType) throws SQLException {
        String sql = "UPDATE transaction_discounts SET status = 'REMOVED' WHERE transaction_id = ? AND discount_type = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        stmt.setString(2, discountType);
        stmt.executeUpdate();
    }

    /**
     * Delete all active discounts linked to a specific item.
     * Used when a product is voided.
     * @param itemId The transaction item ID
     * @throws SQLException if database operation fails
     */
    public void deleteByItemId(int itemId) throws SQLException {
        String sql = "UPDATE transaction_discounts SET status = 'REMOVED' WHERE item_id = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, itemId);
        stmt.executeUpdate();
    }

    /**
     * Get the total discount amount for a transaction.
     * @param transactionId The transaction ID
     * @return The sum of all active discount amounts (negative value)
     * @throws SQLException if database operation fails
     */
    public double getTotalDiscountAmount(int transactionId) throws SQLException {
        String sql = "SELECT SUM(discount_amount) as total FROM transaction_discounts WHERE transaction_id = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("total");
        }
        return 0.0;
    }

    /**
     * Map a ResultSet row to a TransactionDiscount object.
     * @param rs The ResultSet positioned at a row
     * @return The mapped TransactionDiscount
     * @throws SQLException if database operation fails
     */
    private TransactionDiscount mapResultSetToDiscount(ResultSet rs) throws SQLException {
        Integer itemId = rs.getInt("item_id");
        if (rs.wasNull()) {
            itemId = null;
        }

        Timestamp timestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : null;

        return new TransactionDiscount(
                rs.getInt("id"),
                rs.getInt("transaction_id"),
                rs.getString("discount_type"),
                rs.getDouble("discount_amount"),
                itemId,
                rs.getString("status"),
                createdAt
        );
    }
}
