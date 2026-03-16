package org.possystem.service;

import org.possystem.dao.TransactionHeaderDao;
import org.possystem.dao.TransactionItemDao;
import org.possystem.entity.TransactionHeader;
import org.possystem.entity.TransactionItem;
import org.possystem.event.PosEvent;
import org.possystem.event.PosEventDispatcher;
import org.possystem.socket.SocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for Transaction operations.
 * Implements PosEventDispatcher to fire events
 * throughout the transaction lifecycle.
 */
public class TransactionService implements PosEventDispatcher {

    private static final double TAX_RATE = 0.07;
    private static final Logger journal = LoggerFactory.getLogger("TRANSACTION_JOURNAL");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final TransactionHeaderDao transactionHeaderDao;
    private final TransactionItemDao transactionItemDao;
    private SocketService socketService;

    private int currentTransactionId = -1;

    public TransactionService() {
        this.transactionHeaderDao = new TransactionHeaderDao();
        this.transactionItemDao = new TransactionItemDao();
    }

    /**
     * Set socket service for broadcasting journal entries
     */
    public void setSocketService(SocketService socketService) {
        this.socketService = socketService;
    }

    /**
     * Log to journal and broadcast to socket if connected
     */
    private void logJournal(String action, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String journalLine = timestamp + "|" + action + "|" + details;

        // Log to file via SLF4J
        journal.info(action + "|" + details);

        // Broadcast to remote POS systems
        if (socketService != null) {
            socketService.addLocalJournalEntry(journalLine);
        }
    }

    public void createTransaction() throws SQLException {
        TransactionHeader header = new TransactionHeader(
                0,
                LocalDateTime.now(),
                0.0,
                0.0,
                0.0,
                0.0,
                "",
                0.0,
                0.0,
                "PENDING"
        );
        currentTransactionId = transactionHeaderDao.insert(header);

        // Log transaction creation
        logJournal("TX_CREATE", "TX_ID:" + currentTransactionId);

        dispatchEvent(PosEvent.TRANSACTION_CREATED, currentTransactionId);
    }

    public void addItem(String upc, String name, double unitPrice) throws SQLException {
        // Check if item already exists in current transaction
        TransactionItem existingItem = transactionItemDao.findActiveItemByUpc(currentTransactionId, upc);

        if (existingItem != null) {
            // Item already exists, increment quantity
            int oldQuantity = existingItem.quantity();
            int newQuantity = oldQuantity + 1;
            double newSubtotal = newQuantity * unitPrice;
            transactionItemDao.updateQuantity(existingItem.id(), newQuantity, newSubtotal);

            // Log quantity update
            String details = String.format("TX_ID:%d|ITEM_ID:%d|UPC:%s|NAME:%s|OLD_QTY:%d|NEW_QTY:%d|UNIT_PRICE:%.2f|NEW_LINE_TOTAL:%.2f",
                    currentTransactionId, existingItem.id(), upc, name, oldQuantity, newQuantity, unitPrice, newSubtotal);
            logJournal("ITEM_QTY_UPDATE", details);

            TransactionItem updatedItem = new TransactionItem(
                    existingItem.id(),
                    currentTransactionId,
                    upc,
                    name,
                    newQuantity,
                    unitPrice,
                    newSubtotal,
                    "ACTIVE"
            );
            dispatchEvent(PosEvent.QUANTITY_UPDATED, updatedItem);
        } else {
            // New item, insert
            TransactionItem item = new TransactionItem(
                    0,
                    currentTransactionId,
                    upc,
                    name,
                    1,
                    unitPrice,
                    unitPrice,
                    "ACTIVE"
            );
            int itemId = transactionItemDao.insert(item);

            // Log item addition
            String details = String.format("TX_ID:%d|ITEM_ID:%d|UPC:%s|NAME:%s|QTY:1|UNIT_PRICE:%.2f|LINE_TOTAL:%.2f",
                    currentTransactionId, itemId, upc, name, unitPrice, unitPrice);
            logJournal("ITEM_ADD", details);

            TransactionItem savedItem = new TransactionItem(
                    itemId,
                    currentTransactionId,
                    upc,
                    name,
                    1,
                    unitPrice,
                    unitPrice,
                    "ACTIVE"
            );
            dispatchEvent(PosEvent.ITEM_ADDED, savedItem);
        }
    }

    public void voidItem(int itemId) throws SQLException {
        // Get item details before voiding
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        TransactionItem itemToVoid = items.stream()
                .filter(item -> item.id() == itemId)
                .findFirst()
                .orElse(null);

        transactionItemDao.updateStatus(itemId, "VOIDED");

        // Log item void
        if (itemToVoid != null) {
            String details = String.format("TX_ID:%d|ITEM_ID:%d|NAME:%s|QTY:%d|VOIDED_AMOUNT:%.2f",
                    currentTransactionId, itemId, itemToVoid.name(), itemToVoid.quantity(), itemToVoid.subtotal());
            logJournal("ITEM_VOID", details);
        }

        dispatchEvent(PosEvent.ITEM_VOIDED, itemId);
    }

    public void voidTransaction() throws SQLException {
        transactionHeaderDao.updateStatus(currentTransactionId, "VOIDED");

        // Log transaction void
        logJournal("TX_VOID", "TX_ID:" + currentTransactionId + "|REASON:user_cancelled");

        dispatchEvent(PosEvent.TRANSACTION_VOIDED, currentTransactionId);
        currentTransactionId = -1;
    }

    public void updateQuantity(int itemId, int quantity, double unitPrice) throws SQLException {
        // Get old quantity before updating
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        TransactionItem oldItem = items.stream()
                .filter(item -> item.id() == itemId)
                .findFirst()
                .orElse(null);

        double subtotal = quantity * unitPrice;
        transactionItemDao.updateQuantity(itemId, quantity, subtotal);

        // Log quantity update
        if (oldItem != null) {
            String details = String.format("TX_ID:%d|ITEM_ID:%d|UPC:%s|NAME:%s|OLD_QTY:%d|NEW_QTY:%d|UNIT_PRICE:%.2f|NEW_LINE_TOTAL:%.2f",
                    currentTransactionId, itemId, oldItem.upc(), oldItem.name(), oldItem.quantity(), quantity, unitPrice, subtotal);
            logJournal("ITEM_QTY_UPDATE", details);
        }

        dispatchEvent(PosEvent.QUANTITY_UPDATED, itemId);
    }

    public void totalTransaction() throws SQLException {
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);

        // Calculate subtotal from active items only
        double subtotal = items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        transactionHeaderDao.updateTotals(currentTransactionId, subtotal, 0.0, tax, total);
        dispatchEvent(PosEvent.TRANSACTION_TOTALLED, total);
    }

    public void processCash(double amountTendered) throws SQLException {
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        double subtotal = items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;
        double changeAmount = amountTendered - total;

        transactionHeaderDao.updateTender(currentTransactionId, "CASH", amountTendered, changeAmount);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");

        // Log cash payment
        String details = String.format("TX_ID:%d|TENDER:CASH|SUBTOTAL:%.2f|TAX:%.2f|TOTAL:%.2f|TENDERED:%.2f|CHANGE:%.2f",
                currentTransactionId, subtotal, tax, total, amountTendered, changeAmount);
        logJournal("TX_COMPLETE", details);

        dispatchEvent(PosEvent.PAYMENT_PROCESSED, changeAmount);
        currentTransactionId = -1;
    }

    public void processCard(String cardId, String cvv, String expiration) throws SQLException {
        // Calculate totals for logging
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        double subtotal = items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        transactionHeaderDao.updateTender(currentTransactionId, "CARD", 0.0, 0.0);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");

        // Log card payment
        String details = String.format("TX_ID:%d|TENDER:CARD|SUBTOTAL:%.2f|TAX:%.2f|TOTAL:%.2f",
                currentTransactionId, subtotal, tax, total);
        logJournal("TX_COMPLETE", details);

        dispatchEvent(PosEvent.PAYMENT_PROCESSED, null);
        currentTransactionId = -1;
    }

    public int getCurrentTransactionId() {
        return currentTransactionId;
    }

    public List<TransactionItem> getCurrentSaleItems() throws SQLException {
        if (currentTransactionId == -1) {
            return new ArrayList<>();
        }
        return transactionItemDao.findByTransactionId(currentTransactionId);
    }

    public void deleteSelectedItems(List<Integer> itemIds) throws SQLException {
        // Get item details before voiding
        List<TransactionItem> allItems = transactionItemDao.findByTransactionId(currentTransactionId);

        for (Integer itemId : itemIds) {
            // Find the item to get its details for logging
            TransactionItem itemToVoid = allItems.stream()
                    .filter(item -> item.id() == itemId)
                    .findFirst()
                    .orElse(null);

            transactionItemDao.updateStatus(itemId, "VOIDED");

            // Log each item void
            if (itemToVoid != null) {
                String details = String.format("TX_ID:%d|ITEM_ID:%d|NAME:%s|QTY:%d|VOIDED_AMOUNT:%.2f",
                        currentTransactionId, itemId, itemToVoid.name(), itemToVoid.quantity(), itemToVoid.subtotal());
                logJournal("ITEM_VOID", details);
            }
        }

        dispatchEvent(PosEvent.ITEM_VOIDED, itemIds);
    }

    public double getTransactionSubtotal() throws SQLException {
        if (currentTransactionId == -1) {
            return 0.0;
        }
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        return items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();
    }

    public double getTransactionTotal() throws SQLException {
        double subtotal = getTransactionSubtotal();
        double tax = subtotal * TAX_RATE;
        return subtotal + tax;
    }
}