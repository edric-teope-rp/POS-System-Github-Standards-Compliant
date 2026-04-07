package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.TransactionPayment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for TransactionPayment entity.
 * Handles all database operations for the transaction_payments table.
 */
public class TransactionPaymentDao {

    /**
     * Insert a new payment record into the database.
     * @param payment The payment to insert
     * @return The generated ID of the inserted payment
     * @throws SQLException if database operation fails
     */
    public int insert(TransactionPayment payment) throws SQLException {
        String sql = """
                INSERT INTO transaction_payments
                (transaction_id, payment_type, amount, payment_order)
                VALUES (?, ?, ?, ?)
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, payment.transactionId());
        stmt.setString(2, payment.paymentType());
        stmt.setDouble(3, payment.amount());
        stmt.setInt(4, payment.paymentOrder());
        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }
        return -1;
    }

    /**
     * Find all payments for a transaction.
     * @param transactionId The transaction ID
     * @return List of payments ordered by payment_order
     * @throws SQLException if database operation fails
     */
    public List<TransactionPayment> findByTransactionId(int transactionId) throws SQLException {
        String sql = "SELECT * FROM transaction_payments WHERE transaction_id = ? ORDER BY payment_order";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        ResultSet rs = stmt.executeQuery();

        List<TransactionPayment> payments = new ArrayList<>();
        while (rs.next()) {
            payments.add(mapResultSetToPayment(rs));
        }
        return payments;
    }

    /**
     * Get the total amount paid for a transaction.
     * @param transactionId The transaction ID
     * @return The sum of all payment amounts
     * @throws SQLException if database operation fails
     */
    public double getTotalPaidAmount(int transactionId) throws SQLException {
        String sql = "SELECT SUM(amount) as total FROM transaction_payments WHERE transaction_id = ?";
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
     * Get the count of payments for a transaction.
     * Used to determine the next payment_order value.
     * @param transactionId The transaction ID
     * @return The count of payments
     * @throws SQLException if database operation fails
     */
    public int getPaymentCount(int transactionId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM transaction_payments WHERE transaction_id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }

    /**
     * Delete all payments for a transaction.
     * Used when voiding a transaction.
     * @param transactionId The transaction ID
     * @throws SQLException if database operation fails
     */
    public void deleteByTransactionId(int transactionId) throws SQLException {
        String sql = "DELETE FROM transaction_payments WHERE transaction_id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        stmt.executeUpdate();
    }

    /**
     * Map a ResultSet row to a TransactionPayment object.
     * @param rs The ResultSet positioned at a row
     * @return The mapped TransactionPayment
     * @throws SQLException if database operation fails
     */
    private TransactionPayment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : null;

        return new TransactionPayment(
                rs.getInt("id"),
                rs.getInt("transaction_id"),
                rs.getString("payment_type"),
                rs.getDouble("amount"),
                rs.getInt("payment_order"),
                createdAt
        );
    }
}
