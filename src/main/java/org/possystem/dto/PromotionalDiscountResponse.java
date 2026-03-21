package org.possystem.dto;

/**
 * DTO for Promotional discount API response.
 * Maps to the JSON response from Discount Engine API endpoint:
 * - GET /api/discounts/calculate-promotion?upc=X&quantity=Y&unitPrice=Z
 *
 * Example JSON response (triggered):
 * {
 *   "hasPromotion": true,
 *   "promotionType": "BUY_X_OR_MORE_GET_Y_PERCENT_OFF",
 *   "description": "Buy 2 or More Get 25% Off",
 *   "minimumQuantity": 2,
 *   "discountPercentage": 25.0,
 *   "lineTotal": 3.98,
 *   "discountAmount": 0.995,
 *   "finalAmount": 2.985,
 *   "triggered": true,
 *   "message": "Buy 2 or More Get 25% Off applied! You saved $1.00"
 * }
 *
 * Example JSON response (not triggered):
 * {
 *   "hasPromotion": true,
 *   "promotionType": "BUY_X_OR_MORE_GET_Y_PERCENT_OFF",
 *   "description": "Buy 2 or More Get 25% Off",
 *   "minimumQuantity": 2,
 *   "discountPercentage": 25.0,
 *   "lineTotal": 1.99,
 *   "discountAmount": 0.0,
 *   "finalAmount": 1.99,
 *   "triggered": false,
 *   "message": "Add 1 more to unlock Buy 2 or More Get 25% Off!"
 * }
 */
public record PromotionalDiscountResponse(
        boolean hasPromotion,       // True if item is eligible for promotion
        String promotionType,       // "BUY_X_OR_MORE_GET_Y_PERCENT_OFF"
        String description,         // "Buy 2 or More Get 25% Off"
        int minimumQuantity,        // 2
        double discountPercentage,  // 25.0
        double lineTotal,           // Original line total (qty * unit price)
        double discountAmount,      // Calculated discount (0.0 if not triggered)
        double finalAmount,         // Line total after discount
        boolean triggered,          // True if discount was applied
        String message              // User-friendly message for display/toast
) {}
