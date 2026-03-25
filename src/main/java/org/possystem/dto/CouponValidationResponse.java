package org.possystem.dto;

/**
 * DTO for Coupon validation API response.
 * Maps to the JSON response from Discount Engine API endpoint:
 * - POST /api/discounts/validate-coupon
 *
 * NEW API STRUCTURE (2026-03-23):
 * - valid: Coupon exists and not expired
 * - triggered: Minimum purchase requirements met
 * - discountAmount: Always returned when valid (even if not triggered)
 * - remainingAmount: How much more needed to unlock discount
 *
 * Example JSON response (Valid, Not Triggered):
 * {
 *   "valid": true,
 *   "triggered": false,
 *   "couponCode": "SAVE20",
 *   "couponType": "CART_PERCENTAGE",
 *   "discountValue": 20.0,
 *   "discountAmount": 15.0,
 *   "currentSubtotal": 75.0,
 *   "requiredSubtotal": 100.0,
 *   "remainingAmount": 25.0,
 *   "description": "20% off entire cart (minimum purchase $100)",
 *   "message": "Add $25.00 more to unlock this discount",
 *   "expirationDate": "2027-03-18"
 * }
 *
 * Example JSON response (Valid, Triggered):
 * {
 *   "valid": true,
 *   "triggered": true,
 *   "couponCode": "SAVE20",
 *   "couponType": "CART_PERCENTAGE",
 *   "discountAmount": 24.0,
 *   "message": "Discount applied!",
 *   "expirationDate": "2027-03-18"
 * }
 *
 * Example JSON response (Invalid - Expired):
 * {
 *   "valid": false,
 *   "triggered": false,
 *   "couponCode": "EXPIRED10",
 *   "errorType": "EXPIRED",
 *   "message": "This coupon expired on 2025-03-18"
 * }
 */
public record CouponValidationResponse(
        boolean valid,              // True if coupon exists and not expired
        boolean triggered,          // True if minimum purchase requirements met
        String couponCode,          // Coupon code (e.g., "SAVE20")
        String couponType,          // "CART_PERCENTAGE", "ITEM_DISCOUNT", etc.
        Double discountValue,       // Base discount value (20% = 20.0, $1.50 = 1.5)
        Double discountAmount,      // Calculated discount amount
        Double newSubtotal,         // Subtotal after discount applied
        Double currentSubtotal,     // Current cart subtotal
        Double requiredSubtotal,    // Required minimum (null if no minimum)
        Double remainingAmount,     // How much more needed (0 if triggered)
        String description,         // User-friendly description
        String message,             // Status message ("Discount applied!" or "Add $X more...")
        String errorType,           // Error type: "EXPIRED", "NOT_FOUND" (only if invalid)
        String expirationDate       // Expiration date string
) {}
