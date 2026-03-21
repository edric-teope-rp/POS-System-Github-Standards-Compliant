package org.possystem.dto;

/**
 * DTO for Senior/Veteran discount API response.
 * Maps to the JSON response from Discount Engine API endpoints:
 * - GET /api/discounts/calculate-senior?cartSubtotal=X
 * - GET /api/discounts/calculate-veteran?cartSubtotal=X
 *
 * Example JSON response:
 * {
 *   "discountType": "SENIOR",
 *   "discountPercentage": 5.0,
 *   "discountAmount": 5.0,
 *   "originalSubtotal": 100.0,
 *   "newSubtotal": 95.0
 * }
 */
public record SeniorVeteranDiscountResponse(
        String discountType,        // "SENIOR" or "VETERAN"
        double discountPercentage,  // 5.0 or 10.0
        double discountAmount,      // Calculated discount (positive value in API response)
        double originalSubtotal,    // Cart subtotal before discount
        double newSubtotal          // Cart subtotal after discount
) {}
