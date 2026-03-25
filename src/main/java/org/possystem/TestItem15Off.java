package org.possystem;

import org.possystem.service.DiscountApiClient;
import org.possystem.dto.CouponValidationResponse;
import org.possystem.entity.TransactionItem;
import java.util.List;

/**
 * Test program to check ITEM15OFF behavior with different scenarios
 */
public class TestItem15Off {

    public static void main(String[] args) {
        DiscountApiClient apiClient = new DiscountApiClient();

        System.out.println("=".repeat(70));
        System.out.println("TESTING ITEM15OFF COUPON BEHAVIOR");
        System.out.println("=".repeat(70));
        System.out.println();

        // Test 1: Single item $0.99 (below any reasonable minimum)
        testScenario(apiClient, "Test 1: Single $0.99 item",
            List.of(createItem("123456789012", "Cheap Item", 1, 0.99)),
            0.99
        );

        // Test 2: Single item $1.50 (at the discount amount)
        testScenario(apiClient, "Test 2: Single $1.50 item",
            List.of(createItem("123456789012", "Low Item", 1, 1.50)),
            1.50
        );

        // Test 3: Single item $2.00 (at the new minimum you set)
        testScenario(apiClient, "Test 3: Single $2.00 item",
            List.of(createItem("123456789012", "Min Item", 1, 2.00)),
            2.00
        );

        // Test 4: Single item $5.00 (comfortable above minimum)
        testScenario(apiClient, "Test 4: Single $5.00 item",
            List.of(createItem("123456789012", "Good Item", 1, 5.00)),
            5.00
        );

        // Test 5: Multiple items - check if it targets highest
        testScenario(apiClient, "Test 5: Multiple items ($0.99, $3.00, $1.50)",
            List.of(
                createItem("111111111111", "Cheap", 1, 0.99),
                createItem("222222222222", "Expensive", 1, 3.00),
                createItem("333333333333", "Medium", 1, 1.50)
            ),
            5.49  // Total of all items
        );

        // Test 6: Multiple quantities of same item
        testScenario(apiClient, "Test 6: 3x $2.00 items (qty=3, price=$2.00)",
            List.of(createItem("123456789012", "Same Item", 3, 2.00)),
            6.00
        );

        System.out.println("=".repeat(70));
        System.out.println("TESTING COMPLETE");
        System.out.println("=".repeat(70));
    }

    private static void testScenario(DiscountApiClient apiClient, String testName,
                                     List<TransactionItem> items, double subtotal) {
        System.out.println(testName);
        System.out.println("-".repeat(70));

        // Print cart details
        System.out.println("Cart Items:");
        for (TransactionItem item : items) {
            System.out.printf("  - %s (qty: %d, price: $%.2f, subtotal: $%.2f)%n",
                item.name(), item.quantity(), item.unitPrice(), item.subtotal());
        }
        System.out.printf("Cart Subtotal: $%.2f%n", subtotal);
        System.out.println();

        try {
            CouponValidationResponse response = apiClient.validateCoupon("ITEM15OFF", items, subtotal);

            // Print API response
            System.out.println("API Response:");
            System.out.println("  valid: " + response.valid());
            System.out.println("  triggered: " + response.triggered());
            System.out.println("  couponCode: " + response.couponCode());
            System.out.println("  couponType: " + response.couponType());
            System.out.println("  discountAmount: $" + (response.discountAmount() != null ? String.format("%.2f", response.discountAmount()) : "null"));
            System.out.println("  remainingAmount: $" + (response.remainingAmount() != null ? String.format("%.2f", response.remainingAmount()) : "null"));
            System.out.println("  message: " + response.message());
            System.out.println("  description: " + response.description());
            System.out.println("  errorType: " + response.errorType());

            // Analysis
            System.out.println();
            System.out.println("Analysis:");
            if (!response.valid()) {
                System.out.println("  ❌ Coupon is INVALID (expired or doesn't exist)");
            } else if (response.triggered()) {
                System.out.println("  ✅ Coupon is VALID and TRIGGERED");
                System.out.println("  💰 Discount will be applied: -$" + String.format("%.2f", response.discountAmount()));
            } else {
                System.out.println("  ⚠️  Coupon is VALID but NOT TRIGGERED (minimum not met)");
                System.out.println("  📊 Need $" + String.format("%.2f", response.remainingAmount()) + " more");
            }

        } catch (Exception e) {
            System.out.println("❌ API Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println();
    }

    private static TransactionItem createItem(String upc, String name, int qty, double unitPrice) {
        return new TransactionItem(
            0,              // id
            0,              // transactionId
            upc,            // upc
            name,           // name
            qty,            // quantity
            unitPrice,      // unitPrice
            unitPrice * qty, // subtotal
            "ACTIVE"        // status
        );
    }
}
