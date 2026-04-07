package org.possystem.database;

import org.h2.tools.Server;
import java.sql.*;

public class DatabaseManager {
    private static String DB_URL = "jdbc:h2:~/possystemdb";  // Configurable
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";
    private static Connection connection;

    /**
     * Initialize database with default name
     */
    public static void initialize() {
        initialize("possystemdb");
    }

    /**
     * Initialize database with custom name
     */
    public static void initialize(String dbName) {
        DB_URL = "jdbc:h2:~/" + dbName;

        // Use different H2 console port for different database instances
        int h2ConsolePort = 8082;
        if (!dbName.equals("possystemdb")) {
            // For non-default databases, use port 8083, 8084, etc.
            h2ConsolePort = 8082 + Math.abs(dbName.hashCode() % 10);
        }

        try {
            Server webServer = Server.createWebServer(
                    "-web", "-webAllowOthers", "-webPort", String.valueOf(h2ConsolePort)
            );
            webServer.start();
            System.out.println("H2 Console available at: http://localhost:" + h2ConsolePort);

            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Database connected successfully!");

            createTables();
            DataSeeder.seed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS price_book (
                upc                 VARCHAR(50) PRIMARY KEY,
                name                VARCHAR(100) NOT NULL,
                price               DOUBLE NOT NULL,
                is_featured         BOOLEAN DEFAULT FALSE,
                quick_key_position  INT DEFAULT NULL,
                has_promotion       BOOLEAN DEFAULT FALSE
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_header (
                id                      INT AUTO_INCREMENT PRIMARY KEY,
                transaction_datetime    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                subtotal                DOUBLE NOT NULL,
                discount                DOUBLE NOT NULL,
                tax                     DOUBLE NOT NULL,
                total                   DOUBLE NOT NULL,
                tender_type             VARCHAR(20) NOT NULL,
                amount_tendered         DOUBLE NOT NULL,
                change_amount           DOUBLE NOT NULL,
                status                  VARCHAR(20) DEFAULT 'PENDING'
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_items (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                transaction_id  INT NOT NULL,
                upc             VARCHAR(50) NOT NULL,
                name            VARCHAR(100) NOT NULL,
                quantity        INT NOT NULL,
                unit_price      DOUBLE NOT NULL,
                subtotal        DOUBLE NOT NULL,
                status          VARCHAR(20) DEFAULT 'ACTIVE',
                FOREIGN KEY (transaction_id) REFERENCES transaction_header(id),
                FOREIGN KEY (upc) REFERENCES price_book(upc)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_discounts (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                transaction_id  INT NOT NULL,
                discount_type   VARCHAR(50) NOT NULL,
                discount_amount DOUBLE NOT NULL,
                item_id         INT DEFAULT NULL,
                status          VARCHAR(20) DEFAULT 'ACTIVE',
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (transaction_id) REFERENCES transaction_header(id),
                FOREIGN KEY (item_id) REFERENCES transaction_items(id)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_payments (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                transaction_id  INT NOT NULL,
                payment_type    VARCHAR(10) NOT NULL,
                amount          DOUBLE NOT NULL,
                payment_order   INT NOT NULL,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (transaction_id) REFERENCES transaction_header(id)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                username        VARCHAR(50) UNIQUE NOT NULL,
                password_hash   VARCHAR(64) NOT NULL,
                role            VARCHAR(20) DEFAULT 'MANAGER',
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login      TIMESTAMP
            )
        """);

        System.out.println("Tables created successfully!");
    }

    public static Connection getConnection() {
        return connection;
    }
}