# Phase 3: Discount Engine Integration - Implementation Plan

**Date:** 2026-03-21 (Updated)
**Status:** 🔨 Phase 3B In Progress - Senior/Veteran Discounts Nearly Complete
**Discount Engine API:** `http://localhost:8080/api/discounts` (configurable)

---

## 🎉 CURRENT STATUS: Phase 3B ~95% Complete

### ✅ COMPLETED TODAY (2026-03-21)

#### Database Schema (NEW APPROACH)
- ✅ Created separate `transaction_discounts` table instead of using `item_type` column
- ✅ Schema: (id, transaction_id, discount_type, discount_amount, item_id, status, created_at)
- ✅ Supports both cart-level discounts (item_id=NULL) and item-specific discounts
- ✅ Entity: `TransactionDiscount` Java record created
- ✅ DAO: `TransactionDiscountDao` with full CRUD operations

#### DTO Classes Created
- ✅ `SeniorVeteranDiscountResponse` - For senior/veteran API responses
- ✅ `PromotionalDiscountResponse` - For promotional API responses (Phase 3A prep)

#### HTTP Client Implemented
- ✅ `DiscountApiClient` created with Java 11+ HttpClient (no new dependencies)
- ✅ `calculateSeniorDiscount(double cartSubtotal)` - GET request to API
- ✅ `calculateVeteranDiscount(double cartSubtotal)` - GET request to API
- ✅ `calculatePromotionalDiscount(String upc, int qty, double price)` - Prepared for Phase 3A
- ✅ `isApiAvailable()` - Health check method
- ✅ Timeout: 5 seconds configured
- ✅ Base URL: `http://localhost:8080/api/discounts` (configurable)

#### DiscountDialog UI - Fully Polished ✨
- ✅ Complete dialog implementation with 2x2 grid layout
- ✅ Buttons: Senior Discount (5%), Veteran Discount (10%), Apply Coupon, Cancel
- ✅ **Purple color scheme** for discount buttons (RGB 138,43,226 for Senior/Veteran)
- ✅ Different shade of purple for Apply Coupon button (RGB 147,51,234)
- ✅ **Black text outlines** on all buttons for better readability
- ✅ "Currently Applied" status panel on right side (shows active discounts)
- ✅ Dynamic button text (changes to "Remove Senior Discount" / "Remove Veteran Discount")
- ✅ Remove buttons (visible only when discounts active)
- ✅ Vertical divider between button grid and status panel
- ✅ Modal dialog, draggable by header, centered on screen
- ✅ Dimensions: 27% width, 32% height, minimum 490x300
- ✅ No hover effects (per user preference)
- ✅ No success dialog popups (silent operation per user preference)

#### Service Layer Integration
- ✅ `TransactionService` updated with discount methods:
  - `getSubtotal()` - Returns subtotal INCLUDING active discounts
  - `getActiveDiscounts()` - Returns list of active TransactionDiscount records
  - `hasDiscountType(String type)` - Checks if specific discount type applied
  - `applySeniorVeteranDiscount(String type, double amount)` - Applies discount
  - `removeDiscountByType(String type)` - Removes discount by type
  - `removeDiscountsByItemId(int itemId)` - Removes item-linked discounts
  - Journal logging for all discount operations (DISCOUNT_APPLY, DISCOUNT_REMOVE)

#### Integration Complete
- ✅ `ActionsPanel` constructor accepts `DiscountApiClient` parameter
- ✅ `ActionsPanel.handleDiscount()` opens `DiscountDialog`
- ✅ `PosInterface` instantiates `DiscountApiClient` and passes to `ActionsPanel`
- ✅ Discount button functional in Transaction Actions zone

#### Testing Completed
- ✅ Senior discount application working
- ✅ Veteran discount application working
- ✅ Mutual exclusivity enforced (switching removes other discount)
- ✅ Warning dialogs show when trying to apply both
- ✅ Discount removal working
- ✅ Status panel updates correctly
- ✅ API calls successful

### 🔨 REMAINING TASKS (Phase 3B)

#### Step 8: Update CurrentSalePanel Display (NEXT)
- [ ] Show active discounts in totals section below Subtotal
- [ ] Format: "Senior Discount (5%): -$X.XX" or "Veteran Discount (10%): -$X.XX"
- [ ] Use gray italic text for discount lines
- [ ] Update totals calculation to include discounts
- [ ] Ensure Tax is calculated on discounted subtotal

#### Step 9: Final Testing & Polish
- [ ] Test discount display in CurrentSalePanel totals
- [ ] Test discount recalculation when items added/removed
- [ ] Test discount recalculation when quantities changed
- [ ] Test with API down (should show error and disable button)
- [ ] Test complete transaction flow with discounts
- [ ] Verify receipt shows discounts correctly

---

## 🎯 Implementation Strategy

### Approach: Incremental (3 Sub-Phases)
- **Phase 3A:** Promotional Discounts (automatic)
- **Phase 3B:** Senior/Veteran Discounts (manual)
- **Phase 3C:** Coupon Support (manual + barcode)

### Benefits:
✅ Test each discount type thoroughly before moving on
✅ Easier to debug issues
✅ Can deploy partial functionality
✅ Clear progress milestones

---

## 📋 Technology Decisions

| Component | Decision | Rationale |
|-----------|----------|-----------|
| **HTTP Client** | Java 11+ HttpClient | No new dependencies, modern, async-capable |
| **API URL Config** | Settings Dialog | Easy to change for cloud deployment |
| **Discount Display** | Separate line items | Clear visual, easy to void, matches docs |
| **UI Layout** | Dialog approach | Clean, single "Discount" button opens options |
| **Error Handling** | Disable discount button only | Transactions can proceed without discounts |
| **Promo Notification** | Toast (upper right, 3s) | Non-intrusive, auto-dismiss |
| **Database Schema** | Add `item_type` column | Clean separation, professional approach |
| **Coupon Entry** | Manual + Barcode | Flexible for both methods |
| **Coupon Limit** | One per transaction | Simpler business logic |

---

## 🗄️ Database Schema Changes

### ✅ IMPLEMENTED: Separate transaction_discounts Table

**Decision**: Instead of adding `item_type` column to `transaction_items`, created a separate `transaction_discounts` table.

**Rationale**:
- Cleaner separation of concerns (discounts vs products)
- Better data model (discounts have different fields than items)
- Easier to query and manage discounts independently
- Supports both cart-level discounts (item_id=NULL) and item-specific discounts

```sql
-- transaction_discounts table (CREATED)
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
);
```

### Discount Types:
- `SENIOR` - 5% senior discount (cart-level, item_id=NULL)
- `VETERAN` - 10% veteran discount (cart-level, item_id=NULL)
- `PROMOTIONAL` - Buy X Get Y% Off (item-specific, item_id NOT NULL) - For Phase 3A
- `COUPON` - Coupon code discount (cart-level, item_id=NULL) - For Phase 3C

---

## 📁 New Files to Create

### 1. DiscountApiClient.java
**Location:** `src/main/java/org/possystem/service/DiscountApiClient.java`

**Purpose:** HTTP client for Discount Engine API calls

**Methods:**
```java
public class DiscountApiClient {
    private final HttpClient httpClient;
    private String baseUrl; // Configurable via Settings

    // Promotional discount
    public PromotionalDiscountResponse calculatePromotion(String upc, int quantity, double unitPrice)

    // Senior discount
    public SeniorVeteranDiscountResponse calculateSenior(double cartSubtotal)

    // Veteran discount
    public SeniorVeteranDiscountResponse calculateVeteran(double cartSubtotal)

    // Coupon validation
    public CouponValidationResponse validateCoupon(String couponCode, double cartSubtotal, List<CartItem> items)

    // Health check
    public boolean isApiAvailable()

    // Configuration
    public void setBaseUrl(String url)
    public String getBaseUrl()
}
```

---

### 2. DTO Classes (Response Models)

**Location:** `src/main/java/org/possystem/dto/`

#### PromotionalDiscountResponse.java
```java
public record PromotionalDiscountResponse(
    boolean hasPromotion,
    String promotionType,
    String description,
    int minimumQuantity,
    double discountPercentage,
    double lineTotal,
    double discountAmount,
    double finalAmount,
    boolean triggered,
    String message
) {}
```

#### SeniorVeteranDiscountResponse.java
```java
public record SeniorVeteranDiscountResponse(
    String discountType,  // "SENIOR" or "VETERAN"
    double discountPercentage,
    double discountAmount,
    double originalSubtotal,
    double newSubtotal
) {}
```

#### CouponValidationResponse.java
```java
public record CouponValidationResponse(
    boolean valid,
    String couponCode,
    String couponType,
    double discountValue,
    double discountAmount,
    double newSubtotal,
    String description,
    String message,
    String errorType,
    String expirationDate
) {}
```

#### CartItem.java
```java
public record CartItem(
    String upc,
    int quantity,
    double unitPrice
) {}
```

---

### 3. DiscountConfig.java
**Location:** `src/main/java/org/possystem/config/DiscountConfig.java`

**Purpose:** JSON persistence for discount API configuration

```java
public record DiscountConfig(
    String apiBaseUrl,
    int timeoutMillis
) {
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/api/discounts";
    public static final int DEFAULT_TIMEOUT = 5000;
}
```

**Config File:** `config/discount-api-config.json`

---

### 4. DiscountDialog.java
**Location:** `src/main/java/org/possystem/ui/DiscountDialog.java`

**Purpose:** Dialog that opens when "Discount" button is clicked

**UI Layout:**
```
┌─────────────────────────────────────────┐
│  Discount Options                    [×]│ (Blue header)
├─────────────────────────────────────────┤
│                                         │
│   Select a discount type:               │
│                                         │
│   [  Senior Discount (5%)  ]            │ (Green button)
│                                         │
│   [ Veteran Discount (10%) ]            │ (Green button)
│                                         │
│   [    Apply Coupon Code   ]            │ (Blue button)
│                                         │
│   ────────────────────────────          │
│                                         │
│   Currently Applied:                    │
│   • Veteran Discount (10%): -$5.00      │ (if active)
│   • Coupon (SAVE20): -$20.00            │ (if active)
│                                         │
│   [  Remove Senior/Veteran  ]           │ (Red, if active)
│   [     Remove Coupon       ]           │ (Red, if active)
│                                         │
├─────────────────────────────────────────┤
│              [ Cancel ]                 │ (Red button)
└─────────────────────────────────────────┘
```

---

### 5. ToastNotification.java
**Location:** `src/main/java/org/possystem/ui/ToastNotification.java`

**Purpose:** Non-intrusive notification for promotional discounts

**Features:**
- Appears in upper-right corner of POS window
- Auto-dismisses after 3 seconds
- Slide-in animation from right
- Stacks vertically if multiple toasts
- Green background for success messages

**Example:**
```
┌───────────────────────────────────┐
│ ✓ Buy 2 or More Get 25% Off!    │
│   You saved $1.49                │
└───────────────────────────────────┘
```

---

## 🔧 Files to Modify

### 1. TransactionService.java
**Changes:**
- Add method: `addDiscountItem(String name, double amount, String itemType)`
- Add method: `removeDiscountsByType(String itemType)`
- Add method: `getActiveDiscounts()`
- Modify: `getCurrentSaleItems()` to include discount items

### 2. TransactionItemDao.java
**Changes:**
- Update `createItem()` to support `item_type` field
- Add `getDiscountsByType(int transactionId, String itemType)`
- Add `deleteDiscountsByType(int transactionId, String itemType)`

### 3. TransactionItem.java (entity)
**Changes:**
- Add field: `String itemType` (default: "PRODUCT")

### 4. ActionsPanel.java
**Changes:**
- Implement `handleDiscount()` method
- Open `DiscountDialog` when clicked
- Disable button when API unavailable

### 5. PosInterface.java
**Changes:**
- Add `DiscountApiClient` instance
- Add API health check on startup
- Handle discount button enable/disable
- Add `ToastNotification` support

### 6. SettingsDialog (in PosInterface.java)
**Changes:**
- Add "Discount API URL" field
- Add "Test Connection" button
- Load/save from `config/discount-api-config.json`

### 7. CurrentSalePanel.java
**Changes:**
- Display discount items with special formatting (dash prefix, negative amounts)
- Different text color for discount lines (gray italic)

---

## 🚀 Phase 3A: Promotional Discounts

### Implementation Steps

#### Step 1: Database Migration
- [ ] Run ALTER TABLE to add `item_type` column
- [ ] Verify migration successful

#### Step 2: Update Entity & DAO
- [ ] Add `itemType` field to `TransactionItem` entity
- [ ] Update `TransactionItemDao.createItem()` to save `item_type`
- [ ] Add `TransactionItemDao.getDiscountsByType()`
- [ ] Update `DataSeeder` if needed

#### Step 3: Create DTO Classes
- [ ] Create `dto/` package
- [ ] Implement `PromotionalDiscountResponse.java`
- [ ] Implement `CartItem.java`

#### Step 4: Create HTTP Client
- [ ] Create `DiscountApiClient.java`
- [ ] Implement `calculatePromotion()` method
- [ ] Implement `isApiAvailable()` health check
- [ ] Add timeout handling (5 seconds)
- [ ] Test with running Discount Engine API

#### Step 5: Create Toast Notification UI
- [ ] Create `ToastNotification.java`
- [ ] Implement slide-in animation
- [ ] Implement auto-dismiss timer
- [ ] Test positioning (upper-right corner)

#### Step 6: Update TransactionService
- [ ] Add `addDiscountItem()` method
- [ ] Add `removeDiscountsByType()` method
- [ ] Add `getPromotionalItems()` helper
- [ ] Integrate with `DiscountApiClient`

#### Step 7: Integrate with Quick Keys & Scanner
- [ ] When promotional item added → Call API
- [ ] If discount triggers → Add discount line item
- [ ] If first time triggering → Show toast notification
- [ ] Update totals display

#### Step 8: Handle Quantity Changes
- [ ] When qty changed via "Change Qty" → Recalculate discount
- [ ] Update discount line item amount

#### Step 9: Handle Item Removal
- [ ] When promotional item voided → Remove associated discount
- [ ] When discount line voided → Just remove discount (keep product)

#### Step 10: Update CurrentSalePanel Display
- [ ] Format discount items with dash prefix: `"- Buy 2 or More Get 25% Off"`
- [ ] Gray italic text for discount lines
- [ ] Negative amounts displayed correctly

#### Step 11: Testing
- [ ] Test with TB Polar Pop (UPC: 041594899038)
- [ ] Test qty 1 → no discount
- [ ] Test qty 2 → discount triggers, toast shown
- [ ] Test qty 3 → discount amount updates, no new toast
- [ ] Test void promotional item → discount removed
- [ ] Test void discount line → discount removed
- [ ] Test multiple different promotional items

---

## 🚀 Phase 3B: Senior/Veteran Discounts

### Implementation Steps

#### Step 1: Create DTO ✅ COMPLETE
- [x] Implement `SeniorVeteranDiscountResponse.java`

#### Step 2: Update DiscountApiClient ✅ COMPLETE
- [x] Implement `calculateSenior()` method
- [x] Implement `calculateVeteran()` method

#### Step 3: Create Discount Dialog ✅ COMPLETE
- [x] Create `DiscountDialog.java`
- [x] Add 4 buttons in 2x2 grid: Senior (5%), Veteran (10%), Apply Coupon, Cancel
- [x] Add "Currently Applied" status section on right side
- [x] Add "Remove" buttons for active discounts (dynamic visibility)
- [x] Wire up to ActionsPanel
- [x] Purple color scheme for all discount buttons
- [x] Black text outlines for better readability
- [x] Vertical divider between buttons and status panel

#### Step 4: Implement Senior Discount ✅ COMPLETE
- [x] Button click → Call API with cart subtotal
- [x] Parse response → Add discount to transaction_discounts table
- [x] Update status display
- [x] If Veteran already applied → Show warning, require removal first

#### Step 5: Implement Veteran Discount ✅ COMPLETE
- [x] Button click → Call API with cart subtotal
- [x] Parse response → Add discount to transaction_discounts table
- [x] Update status display
- [x] If Senior already applied → Show warning, require removal first

#### Step 6: Implement Remove Functionality ✅ COMPLETE
- [x] "Remove Senior/Veteran" button → Delete discount record
- [x] Update status display
- [x] Dynamic button text based on active discount

#### Step 7: Update Settings Dialog ⏭️ DEFERRED
- [ ] Add "Discount API URL" text field (not needed yet, using default)
- [ ] Add "Test Connection" button (not needed yet)
- [ ] Load from `config/discount-api-config.json` (not needed yet)
- [ ] Save on change (not needed yet)

#### Step 8: CurrentSalePanel Display 🔨 NEXT
- [ ] Show active discounts in totals section below Subtotal
- [ ] Format: "Senior Discount (5%): -$X.XX" or "Veteran Discount (10%): -$X.XX"
- [ ] Use gray italic text for discount lines
- [ ] Update totals calculation to include discounts
- [ ] Ensure Tax is calculated on discounted subtotal

#### Step 9: Testing 🔨 IN PROGRESS
- [x] Test Senior discount application
- [x] Test Veteran discount application
- [x] Test switching from Senior → Veteran (warning dialog)
- [x] Test switching from Veteran → Senior (warning dialog)
- [x] Test removing discounts
- [ ] Test discount display in CurrentSalePanel
- [ ] Test with API down (button disabled - not yet implemented)
- [ ] Test complete transaction with discounts

---

## 🚀 Phase 3C: Coupon Support

### Implementation Steps

#### Step 1: Create DTO
- [ ] Implement `CouponValidationResponse.java`

#### Step 2: Update DiscountApiClient
- [ ] Implement `validateCoupon()` method
- [ ] Build request body with cart items

#### Step 3: Create Coupon Entry Dialog
- [ ] Reuse on-screen keyboard from QuickKeysPanel
- [ ] Or create new `CouponEntryDialog.java`
- [ ] Case-insensitive input

#### Step 4: Wire to Discount Dialog
- [ ] "Apply Coupon" button → Open coupon entry dialog
- [ ] User enters code → Call API
- [ ] If valid → Add discount line item, show success toast
- [ ] If invalid → Show error dialog with reason

#### Step 5: Integrate with Barcode Scanner
- [ ] Detect coupon code pattern (optional, or all codes)
- [ ] Call API when coupon scanned
- [ ] Same validation logic as manual entry

#### Step 6: Enforce One Coupon Rule
- [ ] Check if coupon already applied
- [ ] If yes → Show dialog: "Replace existing coupon?"
- [ ] If confirmed → Remove old, apply new

#### Step 7: Handle Coupon Errors
- [ ] Expired → Show warning: "Coupon expired on [date]"
- [ ] Not found → Show error: "Coupon code not found"
- [ ] Min purchase not met → Show warning: "Minimum purchase of $X required"

#### Step 8: Testing
- [ ] Test SAVE20 (valid, $100 min purchase)
- [ ] Test ITEM15OFF (valid, highest item discount)
- [ ] Test EXPIRED10 (expired)
- [ ] Test MEMBER10 (valid, 10% off)
- [ ] Test invalid code
- [ ] Test below minimum purchase
- [ ] Test barcode scanning
- [ ] Test replacing existing coupon

---

## 🎨 UI Visual Guidelines

### Discount Line Item Formatting

**In Current Sale Table:**
```
Item Name                    Qty    Price         Line Total
TB Polar Pop 30OZ FOAM       3      $1.99         $5.97
- Buy 2 or More Get 25% Off  -      -             -$1.49    (gray italic)
Coca Cola                    2      $1.99         $3.98
Bread                        1      $2.50         $2.50

Subtotal: $9.46
- Senior Discount (5%):                          -$0.47    (gray italic)
- Coupon (SAVE20):                               -$0.00    (not met)
Tax (7%):                                        +$0.62
Total: $9.08
```

### Discount Dialog Colors
- **Header:** Blue `(23, 162, 184)`
- **Senior/Veteran buttons:** Green `(40, 167, 69)`
- **Apply Coupon button:** Blue `(0, 123, 255)`
- **Remove buttons:** Red `(220, 53, 69)`
- **Cancel button:** Red `(220, 53, 69)`

### Toast Notification
- **Background:** Light green `(212, 237, 218)`
- **Border:** Green `(40, 167, 69)`
- **Text:** Dark green `(20, 100, 40)`
- **Icon:** ✓ (checkmark)

---

## 🧪 Testing Checklist

### Phase 3A: Promotional Discounts
- [ ] Scan promotional item once → No discount
- [ ] Scan same item again → Discount triggers, toast shown
- [ ] Scan same item 3rd time → Discount updates, no new toast
- [ ] Change qty via button → Discount recalculates
- [ ] Void promotional item → Discount removed
- [ ] Void discount line → Discount removed
- [ ] Multiple different promotional items → Each gets own discount
- [ ] API down → Promotional items still add, no discount

### Phase 3B: Senior/Veteran
- [ ] Apply Senior → Discount added
- [ ] Apply Veteran → Discount added
- [ ] Apply Senior, then Veteran → Senior removed, Veteran added
- [ ] Apply Veteran, then Senior → Veteran removed, Senior added
- [ ] Remove discount → Discount line removed
- [ ] API down → Discount button disabled
- [ ] Change API URL in settings → New URL used

### Phase 3C: Coupons
- [ ] Manual entry valid coupon → Discount added
- [ ] Manual entry expired coupon → Error shown
- [ ] Manual entry invalid code → Error shown
- [ ] Scan valid coupon → Discount added
- [ ] Apply second coupon → Prompt to replace
- [ ] Remove coupon → Discount line removed
- [ ] SAVE20 below $100 → Error: minimum purchase
- [ ] SAVE20 above $100 → Discount applied

### Integration Tests
- [ ] Promotional + Senior → Both stack correctly
- [ ] Promotional + Veteran → Both stack correctly
- [ ] Promotional + Coupon → Both stack correctly
- [ ] Promotional + Senior + Coupon → All stack correctly
- [ ] Senior + Coupon → Both stack correctly
- [ ] Void transaction → All discounts cleared
- [ ] Complete payment → Discounts saved in journal
- [ ] Receipt displays all discounts correctly

---

## 📦 Configuration Files

### config/discount-api-config.json
```json
{
  "apiBaseUrl": "http://localhost:8080/api/discounts",
  "timeoutMillis": 5000
}
```

---

## 🚨 Error Handling Matrix

| Error Scenario | Behavior | User Experience |
|----------------|----------|-----------------|
| API Unreachable (startup) | Disable "Discount" button | Error dialog: "Discount service unavailable" |
| API Timeout (during call) | Show error, allow retry | Error dialog: "Request timed out. Try again?" |
| Invalid API Response | Log error, skip discount | Warning dialog: "Could not apply discount" |
| Coupon Expired | Show validation error | Warning: "Coupon expired on [date]" |
| Coupon Not Found | Show validation error | Error: "Coupon code not found" |
| Min Purchase Not Met | Show validation error | Warning: "Minimum purchase of $X required" |
| Network Error | Show error, allow manual | Error: "Network error. Try again?" |

---

## 📊 Database Schema Reference

### transaction_items Table (After Migration)

```
┌─────────────────┬──────────────┬──────────────┬──────────┬───────────────┐
│ Column          │ Type         │ Nullable     │ Default  │ Description   │
├─────────────────┼──────────────┼──────────────┼──────────┼───────────────┤
│ id              │ INT          │ NO           │ AI       │ Primary key   │
│ transaction_id  │ INT          │ NO           │ -        │ FK to txn     │
│ upc             │ VARCHAR(50)  │ NO           │ -        │ Product code  │
│ name            │ VARCHAR(255) │ NO           │ -        │ Item name     │
│ quantity        │ INT          │ NO           │ -        │ Quantity      │
│ unit_price      │ DOUBLE       │ NO           │ -        │ Price per unit│
│ subtotal        │ DOUBLE       │ NO           │ -        │ Line total    │
│ status          │ VARCHAR(10)  │ NO           │ 'ACTIVE' │ ACTIVE/VOIDED │
│ item_type       │ VARCHAR(20)  │ NO           │ 'PRODUCT'│ Item type     │ ← NEW
└─────────────────┴──────────────┴──────────────┴──────────┴───────────────┘
```

### item_type Values:
- `PRODUCT` - Regular product
- `PROMOTIONAL_DISCOUNT` - Buy X Get Y% Off
- `SENIOR_DISCOUNT` - 5% senior discount
- `VETERAN_DISCOUNT` - 10% veteran discount
- `COUPON_DISCOUNT` - Coupon code discount

---

## 🎯 Success Criteria

### Phase 3A Complete When:
✅ Promotional items trigger discounts automatically
✅ Toast notifications appear on first trigger
✅ Discount amounts update when qty changes
✅ Discount line items display correctly in table
✅ Voiding items removes associated discounts
✅ All tests pass

### Phase 3B Complete When:
✅ Senior discount applies correctly
✅ Veteran discount applies correctly
✅ Switching between them works smoothly
✅ Settings dialog allows API URL configuration
✅ Discount button disables when API down
✅ All tests pass

### Phase 3C Complete When:
✅ Manual coupon entry works
✅ Barcode coupon scanning works
✅ All 4 coupon codes validate correctly
✅ Expired/invalid coupons show proper errors
✅ One coupon per transaction enforced
✅ All tests pass

### Full Phase 3 Complete When:
✅ All discount types work independently
✅ All discount types stack correctly
✅ Transactions complete with all discounts
✅ Receipts show all discounts
✅ Journal logs include discount details
✅ Error handling works gracefully
✅ API URL is configurable
✅ Documentation updated

---

## 📝 Next Steps

1. **Review this plan** with team/stakeholders
2. **Start with Phase 3A** (Promotional Discounts)
3. **Run database migration** first
4. **Implement step-by-step** following checklist
5. **Test thoroughly** after each phase
6. **Deploy incrementally** if desired

---

**Ready to begin implementation? Start with Phase 3A, Step 1! 🚀**
