package org.possystem.service;

import org.possystem.dao.TransactionHeaderDao;
import org.possystem.dao.TransactionItemDao;
import org.possystem.dao.TransactionDiscountDao;
import org.possystem.dao.PriceBookDao;
import org.possystem.dto.SeniorVeteranDiscountResponse;
import org.possystem.dto.PromotionalDiscountResponse;
import org.possystem.dto.CouponValidationResponse;
import org.possystem.entity.TransactionHeader;
import org.possystem.entity.TransactionItem;
import org.possystem.entity.TransactionDiscount;
import org.possystem.entity.PriceBook;
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
import java.util.HashSet;
import java.util.Set;

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
    private final TransactionDiscountDao transactionDiscountDao;
    private final PriceBookDao priceBookDao;
    private SocketService socketService;
    private DiscountApiClient discountApiClient;
    private ToastCallback toastCallback;

    private int currentTransactionId = -1;
    private Set<String> triggeredPromotions = new HashSet<>(); // Track which promotions showed toast
    private java.util.HashMap<Integer, String> pendingCouponCodes = new java.util.HashMap<>(); // Track coupon codes (discount_id -> coupon_code)

    public TransactionService() {
        this.transactionHeaderDao = new TransactionHeaderDao();
        this.transactionItemDao = new TransactionItemDao();
        this.transactionDiscountDao = new TransactionDiscountDao();
        this.priceBookDao = new PriceBookDao();
    }

    /**
     * Callback interface for showing toast notifications
     */
    public interface ToastCallback {
        void showToast(String title, String message);
    }

    /**
     * Set socket service for broadcasting journal entries
     */
    public void setSocketService(SocketService socketService) {
        this.socketService = socketService;
    }

    /**
     * Set discount API client for recalculating discounts
     */
    public void setDiscountApiClient(DiscountApiClient discountApiClient) {
        this.discountApiClient = discountApiClient;
    }

    /**
     * Set toast callback for showing promotional discount notifications
     */
    public void setToastCallback(ToastCallback toastCallback) {
        this.toastCallback = toastCallback;
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

        // Clear triggered promotions and pending coupons for new transaction
        triggeredPromotions.clear();

        // DEBUG: Log HashMap clear
        System.out.println("=== HASHMAP CLEAR (createTransaction) ===");
        System.out.println("HashMap size before clear: " + pendingCouponCodes.size());
        System.out.println("Current Transaction ID: " + currentTransactionId);
        pendingCouponCodes.clear();
        System.out.println("HashMap cleared");
        System.out.println("=========================================");

        // Log transaction creation
        logJournal("TX_CREATE", "TX_ID:" + currentTransactionId);

        dispatchEvent(PosEvent.TRANSACTION_CREATED, currentTransactionId);
    }

    public void addItem(String upc, String name, double unitPrice) throws SQLException {
        // Check if this item is promotional
        PriceBook priceBookItem = priceBookDao.findByUpc(upc).orElse(null);
        boolean isPromotionalItem = priceBookItem != null && priceBookItem.hasPromotion();

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

        // Recalculate active percentage-based discounts
        recalculateActiveDiscounts();

        // OPTIMIZATION: Only call promotional discount API if this item is promotional
        if (isPromotionalItem) {
            System.out.println("OPTIMIZATION: Item is promotional, checking discount for UPC: " + upc);
            checkAndApplyPromotionalDiscount(upc); // Check only this specific item
        } else {
            System.out.println("OPTIMIZATION: Item is not promotional, skipping promotional discount check");
        }

        // OPTIMIZATION: Only recalculate coupons if coupons exist in cart
        if (hasActiveCoupons()) {
            System.out.println("OPTIMIZATION: Active coupons found, recalculating");
            recalculateCouponDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No active coupons, skipping coupon recalculation");
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

        // Remove any promotional discounts linked to this item
        removeDiscountsByItemId(itemId);

        // Log item void
        if (itemToVoid != null) {
            String details = String.format("TX_ID:%d|ITEM_ID:%d|NAME:%s|QTY:%d|VOIDED_AMOUNT:%.2f",
                    currentTransactionId, itemId, itemToVoid.name(), itemToVoid.quantity(), itemToVoid.subtotal());
            logJournal("ITEM_VOID", details);
        }

        dispatchEvent(PosEvent.ITEM_VOIDED, itemId);

        // Recalculate active percentage-based discounts
        recalculateActiveDiscounts();

        // OPTIMIZATION: Check if there are any promotional items remaining in cart
        if (hasPromotionalItems()) {
            System.out.println("OPTIMIZATION: Promotional items found after void, recalculating all promotional discounts");
            checkAndApplyPromotionalDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No promotional items in cart, skipping promotional discount check");
        }

        // OPTIMIZATION: Only recalculate coupons if coupons exist
        if (hasActiveCoupons()) {
            System.out.println("OPTIMIZATION: Active coupons found after void, recalculating");
            recalculateCouponDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No active coupons after void, skipping coupon recalculation");
        }
    }

    public void voidTransaction() throws SQLException {
        transactionHeaderDao.updateStatus(currentTransactionId, "VOIDED");

        // Log transaction void
        logJournal("TX_VOID", "TX_ID:" + currentTransactionId + "|REASON:user_cancelled");

        dispatchEvent(PosEvent.TRANSACTION_VOIDED, currentTransactionId);

        // Clear transaction state
        currentTransactionId = -1;
        triggeredPromotions.clear();
        pendingCouponCodes.clear();
    }

    public void updateQuantity(int itemId, int quantity, double unitPrice) throws SQLException {
        // Get old quantity before updating
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        TransactionItem oldItem = items.stream()
                .filter(item -> item.id() == itemId)
                .findFirst()
                .orElse(null);

        // Check if this item is promotional
        boolean isPromotionalItem = false;
        if (oldItem != null) {
            PriceBook priceBookItem = priceBookDao.findByUpc(oldItem.upc()).orElse(null);
            isPromotionalItem = priceBookItem != null && priceBookItem.hasPromotion();
        }

        double subtotal = quantity * unitPrice;
        transactionItemDao.updateQuantity(itemId, quantity, subtotal);

        // Log quantity update
        if (oldItem != null) {
            String details = String.format("TX_ID:%d|ITEM_ID:%d|UPC:%s|NAME:%s|OLD_QTY:%d|NEW_QTY:%d|UNIT_PRICE:%.2f|NEW_LINE_TOTAL:%.2f",
                    currentTransactionId, itemId, oldItem.upc(), oldItem.name(), oldItem.quantity(), quantity, unitPrice, subtotal);
            logJournal("ITEM_QTY_UPDATE", details);
        }

        dispatchEvent(PosEvent.QUANTITY_UPDATED, itemId);

        // Recalculate active percentage-based discounts
        recalculateActiveDiscounts();

        // OPTIMIZATION: Only recalculate promotional discount for this specific item if it's promotional
        if (isPromotionalItem && oldItem != null) {
            System.out.println("OPTIMIZATION: Item is promotional, checking discount for UPC: " + oldItem.upc());
            checkAndApplyPromotionalDiscount(oldItem.upc());
        } else {
            System.out.println("OPTIMIZATION: Item is not promotional, skipping promotional discount check");
        }

        // OPTIMIZATION: Only recalculate coupons if coupons exist
        if (hasActiveCoupons()) {
            System.out.println("OPTIMIZATION: Active coupons found, recalculating");
            recalculateCouponDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No active coupons, skipping coupon recalculation");
        }
    }

    public void totalTransaction() throws SQLException {
        // Calculate items subtotal (before discounts)
        double itemsSubtotal = getTransactionSubtotal();

        // Calculate discount amount
        double discountedSubtotal = getSubtotal(); // includes discounts
        double discountAmount = itemsSubtotal - discountedSubtotal;

        double tax = discountedSubtotal * TAX_RATE;
        double total = discountedSubtotal + tax;

        transactionHeaderDao.updateTotals(currentTransactionId, itemsSubtotal, discountAmount, tax, total);
        dispatchEvent(PosEvent.TRANSACTION_TOTALLED, total);
    }

    public void processCash(double amountTendered) throws SQLException {
        // Calculate items subtotal (before discounts)
        double itemsSubtotal = getTransactionSubtotal();

        // Calculate discount amount
        double discountedSubtotal = getSubtotal(); // includes discounts
        double discountAmount = itemsSubtotal - discountedSubtotal;

        double tax = discountedSubtotal * TAX_RATE;
        double total = discountedSubtotal + tax;
        double changeAmount = amountTendered - total;

        // Update all totals in header
        transactionHeaderDao.updateTotals(currentTransactionId, itemsSubtotal, discountAmount, tax, total);
        transactionHeaderDao.updateTender(currentTransactionId, "CASH", amountTendered, changeAmount);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");

        // Log cash payment
        String details = String.format("TX_ID:%d|TENDER:CASH|SUBTOTAL:%.2f|DISCOUNT:%.2f|TAX:%.2f|TOTAL:%.2f|TENDERED:%.2f|CHANGE:%.2f",
                currentTransactionId, itemsSubtotal, discountAmount, tax, total, amountTendered, changeAmount);
        logJournal("TX_COMPLETE", details);

        dispatchEvent(PosEvent.PAYMENT_PROCESSED, changeAmount);

        // Clear transaction state (but keep pendingCouponCodes for receipt generation)
        currentTransactionId = -1;
        triggeredPromotions.clear();
        // Note: pendingCouponCodes is NOT cleared here - it persists for receipt display
        // and will be cleared when createTransaction() is called for the next transaction
    }

    public void processCard(String cardId, String cvv, String expiration) throws SQLException {
        // Calculate items subtotal (before discounts)
        double itemsSubtotal = getTransactionSubtotal();

        // Calculate discount amount
        double discountedSubtotal = getSubtotal(); // includes discounts
        double discountAmount = itemsSubtotal - discountedSubtotal;

        double tax = discountedSubtotal * TAX_RATE;
        double total = discountedSubtotal + tax;

        // Update all totals in header
        transactionHeaderDao.updateTotals(currentTransactionId, itemsSubtotal, discountAmount, tax, total);
        transactionHeaderDao.updateTender(currentTransactionId, "CARD", total, 0.0);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");

        // Log card payment
        String details = String.format("TX_ID:%d|TENDER:CARD|SUBTOTAL:%.2f|DISCOUNT:%.2f|TAX:%.2f|TOTAL:%.2f",
                currentTransactionId, itemsSubtotal, discountAmount, tax, total);
        logJournal("TX_COMPLETE", details);

        dispatchEvent(PosEvent.PAYMENT_PROCESSED, null);

        // Clear transaction state (but keep pendingCouponCodes for receipt generation)
        currentTransactionId = -1;
        triggeredPromotions.clear();
        // Note: pendingCouponCodes is NOT cleared here - it persists for receipt display
        // and will be cleared when createTransaction() is called for the next transaction
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

        // Recalculate active percentage-based discounts
        recalculateActiveDiscounts();

        // OPTIMIZATION: Check if there are any promotional items remaining in cart
        if (hasPromotionalItems()) {
            System.out.println("OPTIMIZATION: Promotional items found after bulk delete, recalculating all promotional discounts");
            checkAndApplyPromotionalDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No promotional items in cart, skipping promotional discount check");
        }

        // OPTIMIZATION: Only recalculate coupons if coupons exist
        if (hasActiveCoupons()) {
            System.out.println("OPTIMIZATION: Active coupons found after bulk delete, recalculating");
            recalculateCouponDiscounts();
        } else {
            System.out.println("OPTIMIZATION: No active coupons after bulk delete, skipping coupon recalculation");
        }
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
        double subtotal = getSubtotal(); // Use getSubtotal() to include discounts
        double tax = subtotal * TAX_RATE;
        return subtotal + tax;
    }

    // =====================================================
    // DISCOUNT METHODS (Phase 3)
    // =====================================================

    /**
     * Get subtotal INCLUDING discounts (for display and calculations).
     * Formula: items subtotal - discount amounts
     */
    public double getSubtotal() throws SQLException {
        if (currentTransactionId == -1) {
            return 0.0;
        }

        // Get items subtotal
        double itemsSubtotal = getTransactionSubtotal();

        // Subtract active discounts (discounts are stored as negative)
        double discountTotal = transactionDiscountDao.getTotalDiscountAmount(currentTransactionId);

        // discountTotal is negative (e.g., -5.00), so adding it subtracts from subtotal
        return itemsSubtotal + discountTotal;
    }

    /**
     * Get all active discounts for the current transaction.
     */
    public List<TransactionDiscount> getActiveDiscounts() throws SQLException {
        if (currentTransactionId == -1) {
            return new ArrayList<>();
        }
        return transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
    }

    /**
     * Get the coupon code for a specific discount ID.
     * Returns null if discount is not a coupon or code not found.
     *
     * @param discountId The discount ID
     * @return The coupon code (e.g., "SAVE20") or null
     */
    public String getCouponCodeForDiscount(int discountId) {
        String code = pendingCouponCodes.get(discountId);

        // DEBUG: Log HashMap retrieval
        System.out.println("=== HASHMAP GET ===");
        System.out.println("Looking up discount ID: " + discountId);
        System.out.println("HashMap size: " + pendingCouponCodes.size());
        System.out.println("HashMap contents: " + pendingCouponCodes);
        System.out.println("Retrieved code: " + (code != null ? code : "null"));
        System.out.println("==================");

        return code;
    }

    /**
     * Check if a specific discount type is already applied.
     * @param discountType The discount type to check ('SENIOR', 'VETERAN', 'COUPON', 'PROMOTIONAL')
     * @return true if discount type exists and is active
     */
    public boolean hasDiscountType(String discountType) throws SQLException {
        if (currentTransactionId == -1) {
            return false;
        }
        TransactionDiscount discount = transactionDiscountDao.findActiveByTransactionIdAndType(
                currentTransactionId, discountType);
        return discount != null;
    }

    /**
     * Apply senior or veteran discount.
     * @param discountType 'SENIOR' or 'VETERAN'
     * @param discountAmount The discount amount (should be negative, e.g., -5.00)
     */
    public void applySeniorVeteranDiscount(String discountType, double discountAmount) throws SQLException {
        if (currentTransactionId == -1) {
            throw new IllegalStateException("No active transaction");
        }

        // Create discount record (cart-level, so item_id is null)
        TransactionDiscount discount = new TransactionDiscount(
                0,                      // id (auto-generated)
                currentTransactionId,
                discountType,
                discountAmount,         // negative value
                null,                   // itemId = null (cart-level discount)
                "ACTIVE",
                null                    // createdAt (auto-generated)
        );

        int discountId = transactionDiscountDao.insert(discount);

        // Log discount application
        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:%s|AMOUNT:%.2f",
                currentTransactionId, discountId, discountType, discountAmount);
        logJournal("DISCOUNT_APPLY", details);

        dispatchEvent(PosEvent.ITEM_ADDED, null); // Trigger UI refresh
    }

    /**
     * Apply coupon discount.
     * @param couponCode The coupon code (e.g., "SAVE20")
     * @param discountAmount The discount amount (should be negative, e.g., -20.00)
     * @param description User-friendly description (e.g., "$20 off your purchase")
     */
    public void applyCouponDiscount(String couponCode, double discountAmount, String description) throws SQLException {
        if (currentTransactionId == -1) {
            throw new IllegalStateException("No active transaction");
        }

        // Create discount record (cart-level, so item_id is null)
        TransactionDiscount discount = new TransactionDiscount(
                0,                      // id (auto-generated)
                currentTransactionId,
                "COUPON",               // discountType
                discountAmount,         // negative value
                null,                   // itemId = null (cart-level discount)
                "ACTIVE",
                null                    // createdAt (auto-generated)
        );

        int discountId = transactionDiscountDao.insert(discount);

        // Store coupon code for later re-validation at Total time
        pendingCouponCodes.put(discountId, couponCode);

        // DEBUG: Log HashMap operation
        System.out.println("=== HASHMAP ADD ===");
        System.out.println("Added to HashMap: ID=" + discountId + " → CODE=" + couponCode);
        System.out.println("HashMap size after add: " + pendingCouponCodes.size());
        System.out.println("==================");

        // Log discount application
        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:COUPON|CODE:%s|AMOUNT:%.2f|DESC:%s",
                currentTransactionId, discountId, couponCode, discountAmount, description);
        logJournal("DISCOUNT_APPLY", details);

        dispatchEvent(PosEvent.ITEM_ADDED, null); // Trigger UI refresh
    }

    /**
     * Check if any coupons are not triggered (informational only).
     * Returns a user-friendly message if minimum purchase requirements not met.
     * Does NOT block - just provides information for dialog display.
     *
     * @return Warning message if coupons not triggered, null otherwise
     */
    public String checkUntriggeredCoupons() throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            return null;
        }

        // Get all active COUPON discounts
        List<TransactionDiscount> allDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
        List<TransactionDiscount> couponDiscounts = allDiscounts.stream()
                .filter(d -> "COUPON".equals(d.discountType()))
                .toList();

        if (couponDiscounts.isEmpty()) {
            return null;
        }

        // Get current cart state
        List<TransactionItem> cartItems = getCurrentSaleItems();
        double cartSubtotal = getTransactionSubtotal();

        // Check each coupon
        for (TransactionDiscount couponDiscount : couponDiscounts) {
            String couponCode = pendingCouponCodes.get(couponDiscount.id());
            if (couponCode == null) {
                continue;
            }

            try {
                CouponValidationResponse response = discountApiClient.validateCoupon(
                    couponCode,
                    cartItems,
                    cartSubtotal
                );

                // If not triggered, return informational message
                if (response.valid() && !response.triggered()) {
                    if (response.remainingAmount() != null && response.remainingAmount() > 0) {
                        return String.format("Coupon %s requires $%.2f more to qualify (minimum $%.2f). " +
                            "You can proceed without the discount or add more items to save $%.2f.",
                            couponCode,
                            response.remainingAmount(),
                            response.requiredSubtotal(),
                            response.discountAmount());
                    } else {
                        return String.format("Coupon %s requirements not met. Proceed without discount or add eligible items.",
                            couponCode);
                    }
                }

            } catch (Exception e) {
                System.err.println("Failed to check coupon " + couponCode + ": " + e.getMessage());
            }
        }

        return null; // All coupons triggered or check failed
    }

    /**
     * Remove all untriggered coupons before payment processing.
     * Called silently when payment buttons are clicked.
     * Only removes coupons that don't meet minimum purchase requirements.
     */
    public void removeUntriggeredCoupons() throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            return;
        }

        // Get all active COUPON discounts
        List<TransactionDiscount> allDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
        List<TransactionDiscount> couponDiscounts = allDiscounts.stream()
                .filter(d -> "COUPON".equals(d.discountType()))
                .toList();

        if (couponDiscounts.isEmpty()) {
            return;
        }

        // Get current cart state
        List<TransactionItem> cartItems = getCurrentSaleItems();
        double cartSubtotal = getTransactionSubtotal();

        // Remove untriggered coupons
        for (TransactionDiscount couponDiscount : couponDiscounts) {
            String couponCode = pendingCouponCodes.get(couponDiscount.id());
            if (couponCode == null) {
                continue;
            }

            try {
                CouponValidationResponse response = discountApiClient.validateCoupon(
                    couponCode,
                    cartItems,
                    cartSubtotal
                );

                // Remove if not triggered
                if (response.valid() && !response.triggered()) {
                    System.out.println("Removing untriggered coupon: " + couponCode);
                    transactionDiscountDao.deleteById(couponDiscount.id());

                    // DEBUG: Log HashMap removal
                    System.out.println("=== HASHMAP REMOVE (untriggered) ===");
                    System.out.println("Removing discount ID: " + couponDiscount.id() + " with code: " + couponCode);
                    System.out.println("HashMap size before remove: " + pendingCouponCodes.size());
                    pendingCouponCodes.remove(couponDiscount.id());
                    System.out.println("HashMap size after remove: " + pendingCouponCodes.size());
                    System.out.println("====================================");

                    // Log removal
                    String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:COUPON|CODE:%s|REASON:minimum_not_met",
                        currentTransactionId, couponDiscount.id(), couponCode);
                    logJournal("DISCOUNT_REMOVE", details);
                }

            } catch (Exception e) {
                System.err.println("Failed to check coupon " + couponCode + ": " + e.getMessage());
            }
        }
    }

    /**
     * Validate all pending coupons before completing payment.
     * This is called when the user clicks Total/Pay buttons.
     * Re-validates coupons to ensure minimum purchase requirements are still met.
     *
     * @return null if all coupons valid, error message if validation fails
     */
    public String validatePendingCoupons() throws SQLException {
        System.out.println("=== VALIDATE PENDING COUPONS CALLED ===");

        if (currentTransactionId == -1 || discountApiClient == null) {
            System.out.println("No transaction or API client - validation skipped");
            return null;
        }

        // Get all active COUPON discounts
        List<TransactionDiscount> allDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
        List<TransactionDiscount> couponDiscounts = allDiscounts.stream()
                .filter(d -> "COUPON".equals(d.discountType()))
                .toList();

        System.out.println("Found " + couponDiscounts.size() + " coupon discount(s) to validate");

        if (couponDiscounts.isEmpty()) {
            return null; // No coupons to validate
        }

        // Get current cart state
        List<TransactionItem> cartItems = getCurrentSaleItems();
        double cartSubtotal = getTransactionSubtotal();
        System.out.println("Cart subtotal for validation: $" + String.format("%.2f", cartSubtotal));

        boolean discountUpdated = false;

        // Validate each coupon
        for (TransactionDiscount couponDiscount : couponDiscounts) {
            String couponCode = pendingCouponCodes.get(couponDiscount.id());
            if (couponCode == null) {
                System.out.println("WARNING: No coupon code found for discount ID " + couponDiscount.id());
                continue; // Skip if no code stored (shouldn't happen)
            }

            System.out.println("Validating coupon: " + couponCode);

            try {
                CouponValidationResponse response = discountApiClient.validateCoupon(
                    couponCode,
                    cartItems,
                    cartSubtotal
                );

                System.out.println("API Response - Valid: " + response.valid() + ", Triggered: " + response.triggered());
                if (response.remainingAmount() != null) {
                    System.out.println("Remaining amount: $" + String.format("%.2f", response.remainingAmount()));
                }

                // Check if coupon is valid (exists and not expired)
                if (!response.valid()) {
                    String errorMsg = response.message();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Coupon " + couponCode + " is no longer valid";
                    }
                    return errorMsg;
                }

                // Check if coupon is triggered (minimum purchase met)
                if (!response.triggered()) {
                    System.out.println("⚠️ COUPON NOT TRIGGERED - Blocking payment");
                    String errorMsg = response.message();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        // Construct message from remaining amount
                        if (response.remainingAmount() != null && response.remainingAmount() > 0) {
                            errorMsg = String.format("Add $%.2f more to use coupon %s",
                                response.remainingAmount(), couponCode);
                        } else {
                            errorMsg = "Coupon " + couponCode + " requirements not met";
                        }
                    }
                    System.out.println("Error message to show: " + errorMsg);
                    return errorMsg;
                }

                System.out.println("✅ COUPON TRIGGERED - Payment allowed");

                // Coupon is valid AND triggered - update discount amount if it changed
                double newAmount = response.discountAmount() != null ? -response.discountAmount() : 0.0;
                if (Math.abs(newAmount - couponDiscount.discountAmount()) > 0.01) {
                    transactionDiscountDao.updateDiscountAmount(couponDiscount.id(), newAmount);
                    discountUpdated = true;
                    System.out.println("Updated coupon " + couponCode + " discount from $" +
                        String.format("%.2f", Math.abs(couponDiscount.discountAmount())) +
                        " to $" + String.format("%.2f", Math.abs(newAmount)));
                }

            } catch (Exception e) {
                return "Failed to validate coupon " + couponCode + ": " + e.getMessage();
            }
        }

        // Trigger UI refresh if any discount was updated
        if (discountUpdated) {
            dispatchEvent(PosEvent.ITEM_ADDED, null);
        }

        System.out.println("✅ All coupons valid and triggered - returning null");
        System.out.println("========================================");
        return null; // All coupons valid
    }

    /**
     * Remove discount by type.
     * @param discountType The discount type to remove ('SENIOR', 'VETERAN', 'COUPON')
     */
    public void removeDiscountByType(String discountType) throws SQLException {
        if (currentTransactionId == -1) {
            return;
        }

        // If removing coupon, clean up pending coupon codes
        if ("COUPON".equals(discountType)) {
            TransactionDiscount couponDiscount = transactionDiscountDao.findActiveByTransactionIdAndType(
                    currentTransactionId, discountType);
            if (couponDiscount != null) {
                pendingCouponCodes.remove(couponDiscount.id());
            }
        }

        transactionDiscountDao.deleteByTransactionIdAndType(currentTransactionId, discountType);

        // Log discount removal
        String details = String.format("TX_ID:%d|TYPE:%s", currentTransactionId, discountType);
        logJournal("DISCOUNT_REMOVE", details);

        dispatchEvent(PosEvent.ITEM_VOIDED, null); // Trigger UI refresh
    }

    /**
     * Remove discounts linked to a specific item.
     * Called when a product is voided.
     * @param itemId The transaction item ID
     */
    public void removeDiscountsByItemId(int itemId) throws SQLException {
        transactionDiscountDao.deleteByItemId(itemId);

        // Log discount removal
        String details = String.format("TX_ID:%d|ITEM_ID:%d", currentTransactionId, itemId);
        logJournal("DISCOUNT_REMOVE_ITEM", details);
    }

    /**
     * Recalculate active percentage-based discounts (Senior/Veteran) after cart changes.
     * Called automatically after addItem, voidItem, updateQuantity, deleteSelectedItems.
     */
    public void recalculateActiveDiscounts() throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            return; // No active transaction or API client not set
        }

        List<TransactionDiscount> activeDiscounts = getActiveDiscounts();

        for (TransactionDiscount discount : activeDiscounts) {
            String type = discount.discountType();

            // Only recalculate percentage-based discounts (Senior/Veteran)
            if (type.equals("SENIOR") || type.equals("VETERAN")) {
                try {
                    double cartSubtotal = getTransactionSubtotal();

                    SeniorVeteranDiscountResponse response;
                    if (type.equals("SENIOR")) {
                        response = discountApiClient.calculateSeniorDiscount(cartSubtotal);
                    } else {
                        response = discountApiClient.calculateVeteranDiscount(cartSubtotal);
                    }

                    double oldAmount = discount.discountAmount();
                    double newAmount = -response.discountAmount(); // Store as negative

                    // Only update if amount changed
                    if (Math.abs(oldAmount - newAmount) > 0.001) {
                        transactionDiscountDao.updateDiscountAmount(discount.id(), newAmount);

                        // Log recalculation
                        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:%s|OLD_AMOUNT:%.2f|NEW_AMOUNT:%.2f|SUBTOTAL:%.2f",
                                currentTransactionId, discount.id(), type, oldAmount, newAmount, cartSubtotal);
                        logJournal("DISCOUNT_RECALC", details);
                    }

                } catch (Exception e) {
                    // Log error but don't fail the transaction
                    System.err.println("Failed to recalculate " + type + " discount: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Check if there are any active coupons in the current transaction.
     * Used to optimize API calls - skip coupon recalculation if no coupons exist.
     */
    private boolean hasActiveCoupons() throws SQLException {
        if (currentTransactionId == -1) {
            return false;
        }
        List<TransactionDiscount> discounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
        return discounts.stream().anyMatch(d -> "COUPON".equals(d.discountType()));
    }

    /**
     * Check if there are any promotional items in the current transaction.
     * Used to optimize API calls - skip promotional discount check if no promotional items exist.
     */
    private boolean hasPromotionalItems() throws SQLException {
        if (currentTransactionId == -1) {
            return false;
        }
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        for (TransactionItem item : items) {
            if ("ACTIVE".equals(item.status())) {
                PriceBook priceBookItem = priceBookDao.findByUpc(item.upc()).orElse(null);
                if (priceBookItem != null && priceBookItem.hasPromotion()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check and apply promotional discount for a SPECIFIC item (optimization).
     * Only calls the API for the given UPC, avoiding unnecessary checks of other items.
     * Called when adding a promotional item to the cart.
     */
    private void checkAndApplyPromotionalDiscount(String upc) throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            System.out.println("DEBUG: checkAndApplyPromotionalDiscount - No transaction or API client");
            return;
        }

        // Get the item from the cart
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        TransactionItem item = items.stream()
                .filter(i -> i.upc().equals(upc) && i.status().equals("ACTIVE"))
                .findFirst()
                .orElse(null);

        if (item == null) {
            System.out.println("DEBUG: Item " + upc + " not found in cart");
            return;
        }

        // Check if item is promotional
        try {
            PriceBook priceBookItem = priceBookDao.findByUpc(item.upc()).orElse(null);
            if (priceBookItem == null || !priceBookItem.hasPromotion()) {
                return; // Not promotional
            }

            System.out.println("DEBUG: Checking promotional discount for UPC=" + item.upc() + ", qty=" + item.quantity());

            // Call API to check for promotional discount
            PromotionalDiscountResponse response = discountApiClient.calculatePromotionalDiscount(
                item.upc(),
                item.quantity(),
                item.unitPrice()
            );

            System.out.println("DEBUG: API Response - triggered=" + response.triggered() +
                               ", discountAmount=" + response.discountAmount());

            String promoKey = item.upc();

            // Check if discount triggered
            if (response.triggered()) {
                // Check if we already have this discount in the database
                List<TransactionDiscount> existingDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
                TransactionDiscount existingPromo = existingDiscounts.stream()
                    .filter(d -> d.discountType().equals("PROMOTIONAL") && d.itemId() != null && d.itemId() == item.id())
                    .findFirst()
                    .orElse(null);

                boolean isFirstTrigger = !triggeredPromotions.contains(promoKey);

                if (existingPromo == null) {
                    // Create new promotional discount
                    TransactionDiscount discount = new TransactionDiscount(
                        0,
                        currentTransactionId,
                        "PROMOTIONAL",
                        -response.discountAmount(),
                        item.id(),
                        "ACTIVE",
                        null
                    );

                    int discountId = transactionDiscountDao.insert(discount);

                    // Log discount application
                    String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|PROMO:%s|AMOUNT:%.2f",
                        currentTransactionId, discountId, item.id(), item.upc(), response.promotionType(), response.discountAmount());
                    logJournal("DISCOUNT_APPLY", details);

                    // Show toast only on first trigger
                    if (isFirstTrigger && toastCallback != null) {
                        String title = response.description();
                        String message = String.format("%s - You saved $%.2f", item.name(), response.discountAmount());
                        toastCallback.showToast(title, message);
                        triggeredPromotions.add(promoKey);
                    }
                } else {
                    // Update existing discount if amount changed
                    double oldAmount = existingPromo.discountAmount();
                    double newAmount = -response.discountAmount();

                    if (Math.abs(oldAmount - newAmount) > 0.001) {
                        transactionDiscountDao.updateDiscountAmount(existingPromo.id(), newAmount);

                        // Log recalculation
                        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|OLD_AMOUNT:%.2f|NEW_AMOUNT:%.2f",
                            currentTransactionId, existingPromo.id(), item.id(), item.upc(), oldAmount, newAmount);
                        logJournal("DISCOUNT_RECALC", details);
                    }

                    triggeredPromotions.add(promoKey);
                }
            } else {
                // Discount no longer triggered - remove if exists
                List<TransactionDiscount> existingDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
                TransactionDiscount existingPromo = existingDiscounts.stream()
                    .filter(d -> d.discountType().equals("PROMOTIONAL") && d.itemId() != null && d.itemId() == item.id())
                    .findFirst()
                    .orElse(null);

                if (existingPromo != null) {
                    transactionDiscountDao.deleteById(existingPromo.id());

                    // Log removal
                    String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|REASON:threshold_not_met",
                        currentTransactionId, existingPromo.id(), item.id(), item.upc());
                    logJournal("DISCOUNT_REMOVE", details);

                    triggeredPromotions.remove(promoKey);
                }
            }

        } catch (Exception e) {
            System.err.println("DEBUG ERROR: Failed to check promotional discount for item " + item.upc() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check and apply promotional discounts for all promotional items in the cart.
     * Called automatically after voidItem, updateQuantity, deleteSelectedItems.
     * For addItem, use checkAndApplyPromotionalDiscount(upc) instead for better performance.
     */
    public void checkAndApplyPromotionalDiscounts() throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            System.out.println("DEBUG: checkAndApplyPromotionalDiscounts - No transaction or API client");
            return; // No active transaction or API client not set
        }

        // Get all active items in transaction
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        System.out.println("DEBUG: Checking " + items.size() + " items for promotional discounts");

        for (TransactionItem item : items) {
            if (!item.status().equals("ACTIVE")) {
                continue; // Skip voided items
            }

            // Check if item is promotional
            try {
                PriceBook priceBookItem = priceBookDao.findByUpc(item.upc()).orElse(null);
                if (priceBookItem == null) {
                    System.out.println("DEBUG: Item " + item.upc() + " not found in price book");
                    continue;
                }

                if (!priceBookItem.hasPromotion()) {
                    System.out.println("DEBUG: Item " + item.upc() + " (" + item.name() + ") - hasPromotion=false");
                    continue; // Not a promotional item
                }

                System.out.println("DEBUG: Item " + item.upc() + " (" + item.name() + ") - IS PROMOTIONAL, qty=" + item.quantity());

                // Call API to check for promotional discount
                System.out.println("DEBUG: Calling API for UPC=" + item.upc() + ", qty=" + item.quantity() + ", price=" + item.unitPrice());
                PromotionalDiscountResponse response = discountApiClient.calculatePromotionalDiscount(
                    item.upc(),
                    item.quantity(),
                    item.unitPrice()
                );

                System.out.println("DEBUG: API Response - triggered=" + response.triggered() +
                                   ", hasPromotion=" + response.hasPromotion() +
                                   ", discountAmount=" + response.discountAmount() +
                                   ", description=" + response.description());

                String promoKey = item.upc(); // Use UPC as unique key for this promotion

                // Check if discount triggered
                if (response.triggered()) {
                    System.out.println("DEBUG: Discount TRIGGERED for " + item.upc());
                    // Check if we already have this discount in the database
                    List<TransactionDiscount> existingDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
                    TransactionDiscount existingPromo = existingDiscounts.stream()
                        .filter(d -> d.discountType().equals("PROMOTIONAL") && d.itemId() != null && d.itemId() == item.id())
                        .findFirst()
                        .orElse(null);

                    boolean isFirstTrigger = !triggeredPromotions.contains(promoKey);

                    if (existingPromo == null) {
                        // Create new promotional discount
                        TransactionDiscount discount = new TransactionDiscount(
                            0,
                            currentTransactionId,
                            "PROMOTIONAL",
                            -response.discountAmount(), // Store as negative
                            item.id(), // Link to specific item
                            "ACTIVE",
                            null
                        );

                        int discountId = transactionDiscountDao.insert(discount);

                        // Log discount application
                        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|PROMO:%s|AMOUNT:%.2f",
                            currentTransactionId, discountId, item.id(), item.upc(), response.promotionType(), response.discountAmount());
                        logJournal("DISCOUNT_APPLY", details);

                        // Show toast only on first trigger
                        if (isFirstTrigger && toastCallback != null) {
                            String title = response.description();
                            String message = String.format("%s - You saved $%.2f", item.name(), response.discountAmount());
                            toastCallback.showToast(title, message);
                            triggeredPromotions.add(promoKey);
                        }
                    } else {
                        // Update existing discount if amount changed
                        double oldAmount = existingPromo.discountAmount();
                        double newAmount = -response.discountAmount();

                        if (Math.abs(oldAmount - newAmount) > 0.001) {
                            transactionDiscountDao.updateDiscountAmount(existingPromo.id(), newAmount);

                            // Log recalculation
                            String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|OLD_AMOUNT:%.2f|NEW_AMOUNT:%.2f",
                                currentTransactionId, existingPromo.id(), item.id(), item.upc(), oldAmount, newAmount);
                            logJournal("DISCOUNT_RECALC", details);
                        }

                        // Mark as triggered (no toast on recalculation)
                        triggeredPromotions.add(promoKey);
                    }
                } else {
                    System.out.println("DEBUG: Discount NOT triggered for " + item.upc() + " (threshold not met or no promotion configured)");
                    // Discount no longer triggered - remove if exists
                    List<TransactionDiscount> existingDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
                    TransactionDiscount existingPromo = existingDiscounts.stream()
                        .filter(d -> d.discountType().equals("PROMOTIONAL") && d.itemId() != null && d.itemId() == item.id())
                        .findFirst()
                        .orElse(null);

                    if (existingPromo != null) {
                        transactionDiscountDao.deleteById(existingPromo.id());

                        // Log removal
                        String details = String.format("TX_ID:%d|DISCOUNT_ID:%d|TYPE:PROMOTIONAL|ITEM_ID:%d|UPC:%s|REASON:threshold_not_met",
                            currentTransactionId, existingPromo.id(), item.id(), item.upc());
                        logJournal("DISCOUNT_REMOVE", details);

                        // Remove from triggered set
                        triggeredPromotions.remove(promoKey);
                    }
                }

            } catch (Exception e) {
                // Log error but don't fail the transaction
                System.err.println("DEBUG ERROR: Failed to check promotional discount for item " + item.upc() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("DEBUG: checkAndApplyPromotionalDiscounts - COMPLETE");
    }

    /**
     * Recalculate coupon discount amounts based on current cart state.
     * Called automatically when cart changes (items added, removed, or quantity updated).
     * This ensures coupon discounts update in real-time as the cart total changes.
     */
    public void recalculateCouponDiscounts() throws SQLException {
        if (currentTransactionId == -1 || discountApiClient == null) {
            return; // No active transaction or API client not set
        }

        // Get all active coupon discounts
        List<TransactionDiscount> allDiscounts = transactionDiscountDao.findActiveByTransactionId(currentTransactionId);
        List<TransactionDiscount> couponDiscounts = allDiscounts.stream()
                .filter(d -> "COUPON".equals(d.discountType()))
                .toList();

        if (couponDiscounts.isEmpty()) {
            return; // No coupons to recalculate
        }

        // Get current cart state
        List<TransactionItem> cartItems = getCurrentSaleItems();
        double cartSubtotal = getTransactionSubtotal();

        System.out.println("=== RECALCULATE COUPON DISCOUNTS ===");
        System.out.println("Current cart subtotal: $" + String.format("%.2f", cartSubtotal));
        System.out.println("Active cart items: " + cartItems.size());

        // Check if cart is effectively empty (all items voided or no items)
        boolean isEmptyCart = cartItems.stream().noneMatch(item -> "ACTIVE".equals(item.status()));
        if (isEmptyCart) {
            System.out.println("Cart is empty (all items voided) - Using dummy data for validation");
            // Create dummy cart item for validation (API requires non-empty cart)
            cartItems = List.of(new TransactionItem(0, 0, "000000000000", "Validation", 1, 0.01, 0.01, "ACTIVE"));
            cartSubtotal = 0.01;
        }

        // Recalculate each coupon
        for (TransactionDiscount couponDiscount : couponDiscounts) {
            String couponCode = pendingCouponCodes.get(couponDiscount.id());
            if (couponCode == null) {
                continue; // Skip if no code stored
            }

            try {
                System.out.println("Validating coupon: " + couponCode);
                CouponValidationResponse response = discountApiClient.validateCoupon(
                    couponCode,
                    cartItems,
                    cartSubtotal
                );

                System.out.println("  API Response - Valid: " + response.valid() + ", Triggered: " + response.triggered());
                System.out.println("  Current discount in DB: $" + String.format("%.2f", couponDiscount.discountAmount()));

                // Update discount amount based on current validation
                // Only apply discount if coupon is both valid AND triggered
                double newAmount = 0.0;
                if (response.valid() && response.triggered() && response.discountAmount() != null) {
                    newAmount = -response.discountAmount();
                    System.out.println("  Coupon IS triggered - Setting amount to: $" + String.format("%.2f", newAmount));
                } else {
                    System.out.println("  Coupon NOT triggered - Setting amount to: $0.00");
                }

                if (Math.abs(newAmount - couponDiscount.discountAmount()) > 0.01) {
                    System.out.println("  UPDATING database from $" + String.format("%.2f", couponDiscount.discountAmount()) +
                                     " to $" + String.format("%.2f", newAmount));
                    transactionDiscountDao.updateDiscountAmount(couponDiscount.id(), newAmount);
                    String status = response.triggered() ? "triggered" : "pending";
                    System.out.println("  SUCCESS: Recalculated coupon " + couponCode + " (" + status + ")");
                } else {
                    System.out.println("  No update needed (amount unchanged)");
                }

            } catch (Exception e) {
                // Silent error - just log it, don't interrupt cart operations
                System.err.println("Failed to recalculate coupon " + couponCode + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}