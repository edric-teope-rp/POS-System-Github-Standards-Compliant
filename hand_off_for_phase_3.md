# Phase 3 Handoff Document - Discount Feature Implementation

**Date**: 2026-03-18
**Project**: POS System
**Phase**: Phase 3 - Discount Features Integration
**Status**: Ready for Implementation

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Current State - What's Already Done](#current-state---whats-already-done)
3. [Phase 3 Requirements](#phase-3-requirements)
4. [Technical Architecture](#technical-architecture)
5. [UI Components Ready for Integration](#ui-components-ready-for-integration)
6. [Database Schema](#database-schema)
7. [Spring Boot Integration Points](#spring-boot-integration-points)
8. [Implementation Checklist](#implementation-checklist)
9. [Testing Strategy](#testing-strategy)
10. [Key Files Reference](#key-files-reference)

---

## Project Overview

### POS System Tech Stack
- **Frontend**: Java Swing (Java 21)ex
- **Backend**: Spring Boot (separate project)
- **Database**: H2 (embedded)
- **Build Tool**: Gradle
- **Screen Resolution**: Optimized for 1920x1080 (responsive design)

### Current Phase Status
✅ **Phase 1**: Core POS functionality - COMPLETE
✅ **Phase 2**: UI Polish and Quick Keys - COMPLETE
🔨 **Phase 3**: Discount Features - READY TO IMPLEMENT

### Spring Boot Project Details (Actual)
- **Group ID**: `org.discountengine`
- **Artifact ID**: `discount-engine-api`
- **Package**: `org.discountengine.api`
- **Purpose**: Standalone discount calculation service

---

## Current State - What's Already Done

### 1. UI Components Created

#### ✅ Promotional Products Display
- **Location**: Quick Keys Panel
- **Styling**: Light violet buttons (RGB: 230, 200, 255)
- **Features**:
  - Dark violet text (RGB: 80, 40, 120)
  - Green price text for all promotional items
  - Rounded corners (12px radius)
  - Custom `RoundedBorder` class for HTML rendering support
- **Database Field**: `has_promotion` boolean flag in `price_book` table
- **Data**: 17 products marked as promotional (6 featured + 11 random)

#### ✅ Discount Button Added
- **Location**: Transaction Actions Zone (4th button)
- **Color**: Purple/Violet (RGB: 147, 51, 234)
- **Text**: "Discount" (white, Arial Bold 14pt)
- **Status**: Placeholder - clickable but does nothing
- **Handler**: `handleDiscount()` method exists in `ActionsPanel.java` (line 1917-1922)

#### ✅ Layout Optimizations
- **Current Sale Panel**: Width reduced from 42% to 35% of screen width
- **Transaction Actions**: Now has 5 buttons with better readability
- **Space Distribution**: Current Sale (35%) | Quick Keys + Actions (65%)

### 2. Database Ready

#### ✅ `has_promotion` Column Added
```sql
CREATE TABLE IF NOT EXISTS price_book (
    upc VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    is_featured BOOLEAN DEFAULT FALSE,
    quick_key_position INT DEFAULT NULL,
    has_promotion BOOLEAN DEFAULT FALSE
)
```

#### ✅ Data Seeded
- **File**: `src/main/resources/pricebook.tsv`
- **Format**: 6 columns (upc, name, price, is_featured, quick_key_position, has_promotion)
- **Promotional Items**: 17 products marked with `true` in has_promotion column

### 3. Entity Models Updated

#### ✅ PriceBook Record
```java
public record PriceBook(
    String upc,
    String name,
    double price,
    boolean isFeatured,
    Integer quickKeyPosition,
    boolean hasPromotion  // ✅ Added
) {}
```

#### ✅ DAO Layer Updated
- All queries in `PriceBookDao.java` read `has_promotion` column
- All PriceBook instantiations include `hasPromotion` parameter

---

## Phase 3 Requirements

### Discount Types to Implement

#### 1. Senior Discount
- **Amount**: 5% off
- **Application**: Entire cart/transaction
- **Verification**: None required (cashier discretion)
- **Timing**: Can be applied anytime before Total button
- **Mutually Exclusive**: Cannot combine with Veteran Discount
- **Removable**: Cashier can remove if applied incorrectly

#### 2. Veteran Discount
- **Amount**: 10% off
- **Application**: Entire cart/transaction
- **Verification**: None required (cashier discretion)
- **Timing**: Can be applied anytime before Total button
- **Mutually Exclusive**: Cannot combine with Senior Discount
- **Removable**: Cashier can remove if applied incorrectly

#### 3. Promotional Discount
- **Amount**: 15% off
- **Application**: Only on items marked with `has_promotion = true`
- **Trigger**: Automatic when promotional item is added to cart
- **Timing**: Applied immediately when item scanned/selected
- **Stackable**: Can stack with Senior OR Veteran discount
- **Visual Indicator**: Light violet buttons in Quick Keys panel

#### 4. Coupon Codes
Four coupon codes to implement:

| Coupon Code | Discount | Scope | Expiration | Status |
|-------------|----------|-------|------------|--------|
| **SAVE20** | 20% | Entire Cart | None | Always Valid |
| **ITEM15OFF** | 15% | Single Item | None | Always Valid |
| **EXPIRED10** | 10% | Entire Cart | 2025-01-01 | **EXPIRED** |
| **MEMBER10** | 10% | Entire Cart | None | Always Valid |

**Coupon Rules**:
- User enters coupon code via on-screen keyboard
- System validates expiration and scope
- Expired coupons show error message
- Cannot combine multiple coupons (one per transaction)
- Can stack with Senior/Veteran + Promotional discounts
- Removable before finalizing transaction

---

## Technical Architecture

### Discount Calculation Flow

```
User Action → Discount Dialog → Spring Boot API → Database
    ↓              ↓                    ↓              ↓
Clicks       Choose Type          Validate &      Store discount
Discount     (Senior/Vet/        Calculate        in transaction
Button       Coupon)             Discount          table
    ↓              ↓                    ↓              ↓
              Apply to Cart ← Return Result ← Update totals
                   ↓
           Refresh Display
           (Show discount line items)
```

### Discount Precedence
1. **Promotional Discount** (15%) - Applied first to eligible items
2. **Senior OR Veteran** (5% or 10%) - Applied to cart subtotal
3. **Coupon Code** (varies) - Applied last to final subtotal

**Example Calculation**:
```
Original Cart: $100.00
- Item A: $30 (has_promotion = true)
- Item B: $70 (has_promotion = false)

Step 1: Promotional Discount
- Item A: $30 - 15% = $25.50
- Item B: $70 (no change)
- New Subtotal: $95.50

Step 2: Senior Discount (5%)
- $95.50 - 5% = $90.73

Step 3: Coupon SAVE20 (20%)
- $90.73 - 20% = $72.58

Final: $72.58 + Tax (7%) = $77.66
```

---

## UI Components Ready for Integration

### 1. Discount Button (Already Created)

**File**: `ActionsPanel.java`
**Line**: 1917-1922

```java
/**
 * Handler for Discount button
 * Placeholder - does nothing for now
 */
private void handleDiscount() {
    // TODO: Implement discount dialog
    // 1. Show dialog with options: Senior, Veteran, Coupon
    // 2. Based on selection, show appropriate UI
    // 3. Call Spring Boot DiscountService API
    // 4. Update transaction with discount
    // 5. Refresh Current Sale display
}
```

#### Implementation Steps:
1. **Create Discount Options Dialog**:
   - Modal dialog with 3 buttons: "Senior Discount", "Veteran Discount", "Apply Coupon"
   - Styled similar to existing dialogs (see error/confirm dialogs in CurrentSalePanel.java)
   - Button colors: Blue for Senior, Blue for Veteran, Purple for Coupon

2. **Senior/Veteran Discount Flow**:
   ```java
   private void applySeniorDiscount() {
       // 1. Check if veteran discount already applied (mutually exclusive)
       // 2. Call: transactionService.applySeniorDiscount()
       // 3. Show confirmation: "5% Senior Discount Applied"
       // 4. Refresh Current Sale display
       // 5. Show discount as line item in cart
   }

   private void applyVeteranDiscount() {
       // Same as senior but 10%
   }
   ```

3. **Coupon Flow**:
   ```java
   private void showCouponDialog() {
       // 1. Show on-screen keyboard for coupon entry
       // 2. User enters code (e.g., "SAVE20")
       // 3. Call: transactionService.applyCoupon(code)
       // 4. Handle response:
       //    - Success: Show discount applied
       //    - Expired: Show error "Coupon expired on [date]"
       //    - Invalid: Show error "Invalid coupon code"
       // 5. Refresh display
   }
   ```

### 2. Current Sale Display (Already Supports Discounts)

**File**: `CurrentSalePanel.java`

The table already displays transaction items. You'll need to:
1. Add discount line items to the table (negative amounts)
2. Update totals to reflect discounts
3. Show discount type in item name (e.g., "Senior Discount (5%)")

**Example Display**:
```
┌─────────────────────────────────────────────┐
│ Item Name          Qty    Price   Line Total│
├─────────────────────────────────────────────┤
│ Apple              2      $1.50   $3.00     │
│ Banana             1      $0.75   $0.75     │
│ Promotional Item   1      $10.00  $8.50     │ ← 15% auto applied
│ - Promo Discount   -      -       -$1.50    │ ← Shown as line item
│ Senior Discount    -      -       -$0.61    │ ← 5% cart discount
│                                              │
│ Subtotal: $10.14                            │
│ Tax (7%): $0.71                             │
│ Total: $10.85                               │
└─────────────────────────────────────────────┘
```

### 3. Remove Discount Functionality

Add "Remove Discount" option:
- Right-click on discount line item in Current Sale table
- Show context menu: "Remove Discount"
- Call `transactionService.removeDiscount(discountId)`
- Refresh display

---

## Database Schema

### Current Transaction Tables

#### `transactions` Table
```sql
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal DOUBLE NOT NULL,
    tax DOUBLE NOT NULL,
    total DOUBLE NOT NULL,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS'
)
```

#### `transaction_items` Table
```sql
CREATE TABLE IF NOT EXISTS transaction_items (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    transaction_id INTEGER NOT NULL,
    upc VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DOUBLE NOT NULL,
    subtotal DOUBLE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
)
```

### Recommended: Add `transaction_discounts` Table

```sql
CREATE TABLE IF NOT EXISTS transaction_discounts (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    transaction_id INTEGER NOT NULL,
    discount_type VARCHAR(50) NOT NULL,  -- 'SENIOR', 'VETERAN', 'COUPON', 'PROMOTIONAL'
    discount_code VARCHAR(50),           -- NULL for Senior/Veteran, code for Coupon
    discount_percentage DOUBLE,          -- e.g., 5.0 for 5%
    discount_amount DOUBLE NOT NULL,     -- calculated discount amount
    applied_to VARCHAR(20) DEFAULT 'CART', -- 'CART' or 'ITEM'
    item_id INTEGER,                     -- FK to transaction_items if applied to specific item
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE' or 'REMOVED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (item_id) REFERENCES transaction_items(id)
)
```

### Recommended: Add `coupon_codes` Table

```sql
CREATE TABLE IF NOT EXISTS coupon_codes (
    code VARCHAR(50) PRIMARY KEY,
    discount_percentage DOUBLE NOT NULL,
    applies_to VARCHAR(20) NOT NULL,  -- 'CART' or 'ITEM'
    expiration_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(200)
)
```

**Seed Data**:
```sql
INSERT INTO coupon_codes VALUES
('SAVE20', 20.0, 'CART', NULL, TRUE, '20% off entire cart'),
('ITEM15OFF', 15.0, 'ITEM', NULL, TRUE, '15% off single item'),
('EXPIRED10', 10.0, 'CART', '2025-01-01', FALSE, 'Expired 10% cart discount'),
('MEMBER10', 10.0, 'CART', NULL, TRUE, 'Member exclusive 10% off');
```

---

## Spring Boot Integration Points

**Spring Boot Project**: `org.discountengine:discount-engine-api`
- Package: `org.discountengine.api`
- All controller, service, and repository classes will be under this package structure

### Service Layer Required

#### 1. DiscountService
Create a Spring Boot REST service for discount operations.

**Endpoints Needed**:

```java
@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    // Apply Senior Discount (5%)
    @PostMapping("/senior/{transactionId}")
    public ResponseEntity<DiscountResponse> applySeniorDiscount(
        @PathVariable Integer transactionId
    ) {
        // 1. Validate no veteran discount exists
        // 2. Calculate 5% of cart subtotal (after promotional)
        // 3. Save to transaction_discounts table
        // 4. Return discount details
    }

    // Apply Veteran Discount (10%)
    @PostMapping("/veteran/{transactionId}")
    public ResponseEntity<DiscountResponse> applyVeteranDiscount(
        @PathVariable Integer transactionId
    ) {
        // Same as senior but 10%
    }

    // Apply Promotional Discount (15% per item)
    @PostMapping("/promotional")
    public ResponseEntity<DiscountResponse> applyPromotionalDiscount(
        @RequestBody PromotionalDiscountRequest request
    ) {
        // 1. Verify item has has_promotion = true
        // 2. Calculate 15% of item price
        // 3. Save to transaction_discounts
        // 4. Return discount details
    }

    // Validate and Apply Coupon
    @PostMapping("/coupon")
    public ResponseEntity<CouponResponse> applyCoupon(
        @RequestBody CouponRequest request
    ) {
        // 1. Lookup coupon_code in database
        // 2. Check expiration date
        // 3. Check if already applied
        // 4. Calculate discount based on applies_to
        // 5. Save to transaction_discounts
        // 6. Return result or error
    }

    // Remove Discount
    @DeleteMapping("/{discountId}")
    public ResponseEntity<Void> removeDiscount(
        @PathVariable Integer discountId
    ) {
        // 1. Update status to 'REMOVED' in transaction_discounts
        // 2. Recalculate transaction totals
        // 3. Return success
    }

    // Get All Discounts for Transaction
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<DiscountResponse>> getDiscounts(
        @PathVariable Integer transactionId
    ) {
        // Return all ACTIVE discounts for transaction
    }
}
```

#### 2. TransactionService Integration

Update `TransactionService` to work with discounts:

```java
public class TransactionService {

    // Calculate totals INCLUDING discounts
    public double getTransactionSubtotal() {
        double subtotal = calculateItemsSubtotal();
        double promotionalDiscount = getPromotionalDiscountTotal();
        return subtotal - promotionalDiscount;
    }

    public double getTransactionTotal() {
        double subtotal = getTransactionSubtotal();
        double cartDiscount = getCartDiscountTotal(); // Senior/Veteran/Coupon
        double afterDiscount = subtotal - cartDiscount;
        double tax = afterDiscount * 0.07;
        return afterDiscount + tax;
    }

    // NEW: Get promotional discount total
    private double getPromotionalDiscountTotal() {
        // Query transaction_discounts where type='PROMOTIONAL' and status='ACTIVE'
    }

    // NEW: Get cart-level discount total
    private double getCartDiscountTotal() {
        // Query transaction_discounts where applies_to='CART' and status='ACTIVE'
    }
}
```

### API Client in Java Swing

Create a service to call Spring Boot APIs:

```java
package org.possystem.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class DiscountApiClient {

    private static final String BASE_URL = "http://localhost:8080/api/discounts";
    private final HttpClient httpClient;

    public DiscountApiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public DiscountResponse applySeniorDiscount(int transactionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/senior/" + transactionId))
            .POST(HttpRequest.BodyPublishers.noBody())
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        // Parse JSON response and return DiscountResponse object
    }

    // Similar methods for veteran, promotional, coupon
}
```

---

## Implementation Checklist

### Backend (Spring Boot)

- [ ] Create `transaction_discounts` table
- [ ] Create `coupon_codes` table
- [ ] Seed coupon data (SAVE20, ITEM15OFF, EXPIRED10, MEMBER10)
- [ ] Create `DiscountService` class
- [ ] Create `DiscountController` REST endpoints
- [ ] Update `TransactionService` to include discount calculations
- [ ] Write unit tests for discount logic
- [ ] Test coupon expiration validation
- [ ] Test mutual exclusivity (Senior vs Veteran)
- [ ] Test discount stacking rules

### Frontend (Java Swing)

- [ ] Create `DiscountApiClient` class for API calls
- [ ] Implement discount options dialog (Senior/Veteran/Coupon buttons)
- [ ] Implement `applySeniorDiscount()` method
- [ ] Implement `applyVeteranDiscount()` method
- [ ] Implement coupon entry dialog with on-screen keyboard
- [ ] Implement `applyCoupon()` method
- [ ] Handle coupon validation errors (expired, invalid)
- [ ] Display discount line items in Current Sale table
- [ ] Update totals display to show discounts
- [ ] Implement "Remove Discount" context menu
- [ ] Disable Discount button when no items in cart
- [ ] Enable Discount button after items added
- [ ] Test promotional discount auto-applies when item added
- [ ] Visual feedback for applied discounts

### Integration Testing

- [ ] Test: Senior discount applies 5% correctly
- [ ] Test: Veteran discount applies 10% correctly
- [ ] Test: Cannot apply both Senior and Veteran
- [ ] Test: Promotional discount auto-applies on add to cart
- [ ] Test: Promotional + Senior stacks correctly
- [ ] Test: Promotional + Veteran stacks correctly
- [ ] Test: Valid coupon applies correctly
- [ ] Test: Expired coupon shows error
- [ ] Test: Invalid coupon shows error
- [ ] Test: Cannot apply multiple coupons
- [ ] Test: Remove discount recalculates totals
- [ ] Test: Discount persists across page refreshes
- [ ] Test: Finalizing transaction locks discounts

---

## Testing Strategy

### Unit Tests

1. **Discount Calculation Tests**:
   ```java
   @Test
   public void testSeniorDiscountCalculation() {
       // Cart subtotal: $100
       // Senior discount: 5%
       // Expected: $95
   }

   @Test
   public void testPromotionalStacksWithSenior() {
       // Item: $10 (promotional)
       // After 15% promo: $8.50
       // After 5% senior on cart: $8.08
   }
   ```

2. **Coupon Validation Tests**:
   ```java
   @Test
   public void testExpiredCouponReturnsError() {
       // EXPIRED10 with expiration 2025-01-01
       // Should return error in 2026
   }
   ```

3. **Mutual Exclusivity Tests**:
   ```java
   @Test
   public void testCannotApplyBothSeniorAndVeteran() {
       // Apply Senior → Success
       // Apply Veteran → Error: "Remove senior discount first"
   }
   ```

### Manual Testing Scenarios

#### Scenario 1: Promotional Discount
1. Start new transaction
2. Add promotional item (light violet button)
3. Verify 15% discount auto-applied
4. Check Current Sale shows discount line item
5. Verify subtotal reflects discount

#### Scenario 2: Senior + Promotional
1. Add promotional item
2. Add regular item
3. Click Discount → Senior Discount
4. Verify promotional applies to promo item
5. Verify senior applies to remaining subtotal
6. Check math: (promo_item - 15%) + regular_item → subtotal → (subtotal - 5%)

#### Scenario 3: Coupon Flow
1. Add items to cart
2. Click Discount → Apply Coupon
3. Enter "SAVE20"
4. Verify 20% applies to cart
5. Try entering "EXPIRED10"
6. Verify error message shown

#### Scenario 4: Remove Discount
1. Apply Senior discount
2. Right-click discount line in Current Sale
3. Select "Remove Discount"
4. Verify totals recalculate without discount

---

## Key Files Reference

### Java Swing (Frontend)

| File | Location | Purpose |
|------|----------|---------|
| **ActionsPanel.java** | `src/main/java/org/possystem/ui/` | Contains Discount button and handleDiscount() placeholder |
| **CurrentSalePanel.java** | `src/main/java/org/possystem/ui/` | Shopping cart display - add discount line items here |
| **QuickKeysPanel.java** | `src/main/java/org/possystem/ui/` | Promotional product buttons (light violet) |
| **TransactionService.java** | `src/main/java/org/possystem/service/` | Transaction logic - integrate discount API calls |
| **PriceBook.java** | `src/main/java/org/possystem/entity/` | Entity with hasPromotion field |
| **PriceBookDao.java** | `src/main/java/org/possystem/dao/` | Reads has_promotion from database |
| **DatabaseManager.java** | `src/main/java/org/possystem/database/` | Schema creation - add new discount tables here |

### Database

| File | Location | Purpose |
|------|----------|---------|
| **pricebook.tsv** | `src/main/resources/` | Product data with has_promotion column (17 items marked true) |
| **DataSeeder.java** | `src/main/java/org/possystem/database/` | Seeds data from TSV - update for coupon table |

### Documentation

| File | Location | Description |
|------|----------|-------------|
| **COPY_PASTE_PROMPT.md** | Root | Complete Phase 3 requirements and specifications |
| **DISCOUNT_BUTTON_ADDED.md** | Root | Details on Discount button implementation |
| **CURRENT_SALE_WIDTH_REDUCED.md** | Root | Details on layout optimization |

---

## Important Notes

### Design Decisions Made

1. **Promotional Discount is Automatic**:
   - No user action required
   - Applies immediately when promotional item added to cart
   - 15% discount per promotional item

2. **Senior/Veteran are Mutually Exclusive**:
   - Only one can be active at a time
   - Both apply to entire cart (after promotional)
   - User must explicitly choose one

3. **Coupons**:
   - One coupon per transaction
   - Can stack with Senior/Veteran + Promotional
   - Must validate expiration date
   - User enters code via on-screen keyboard

4. **Discount Removal**:
   - All discounts can be removed before finalizing
   - Totals recalculate automatically
   - Transaction must be in "IN_PROGRESS" status

### Spring Boot Connection

Ensure Spring Boot backend is running on `http://localhost:8080` before testing discount features. The Java Swing application makes HTTP calls to the REST API.

### Error Handling

All discount operations should handle:
- Network errors (Spring Boot not running)
- Invalid discount codes
- Expired coupons
- Duplicate discount attempts
- Empty cart scenarios

Show user-friendly error dialogs consistent with existing UI style (see `CurrentSalePanel.showErrorDialog()`).

---

## Next Steps

1. **Set up Spring Boot project** with discount tables and services
2. **Implement DiscountService** in Spring Boot with all 4 discount types
3. **Create API client** in Java Swing to communicate with Spring Boot
4. **Implement discount dialog** UI in ActionsPanel
5. **Update Current Sale display** to show discount line items
6. **Test discount stacking** and mutual exclusivity
7. **Add remove discount** functionality

---

## Contact & Collaboration

### Project Repository
- **Java Swing POS**: `/Users/ed/IdeaProjects/POSSystem`
- **Spring Boot API**: `org.discountengine:discount-engine-api`
  - Package: `org.discountengine.api`

### Build & Run
```bash
# Java Swing Application
./gradlew run

# Spring Boot Backend
./mvnw spring-boot:run
# or
./gradlew bootRun
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests DiscountServiceTest
```

---

## Summary

**What's Done**:
✅ UI components for discounts (button, promotional styling)
✅ Database schema with `has_promotion` flag
✅ 17 promotional products configured
✅ Layout optimized for 5-button actions zone

**What's Needed**:
🔨 Spring Boot DiscountService implementation
🔨 REST API endpoints for discount operations
🔨 Discount calculation logic with stacking rules
🔨 Coupon validation and expiration checks
🔨 UI dialogs for discount selection
🔨 Current Sale display updates for discount line items
🔨 Remove discount functionality

**Goal**: Seamless integration between Java Swing frontend and Spring Boot backend for comprehensive discount management supporting 4 discount types with proper stacking and validation.

---

**Good luck with Phase 3 implementation!** 🚀

All specifications are in `COPY_PASTE_PROMPT.md`. This handoff document provides the technical bridge to integrate everything smoothly.
