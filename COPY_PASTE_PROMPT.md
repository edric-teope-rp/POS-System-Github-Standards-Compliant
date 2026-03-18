# POS System - Project Handoff Document

I'm continuing work on my Java POS System project. Here's the context from my previous session:

**PROJECT LOCATION**: `/Users/ed/IdeaProjects/POSSystem`

## CURRENT STATE

- **Branch**: develop (current working branch)
- **Main Branch**: main
- **Build**: ✅ Successful
- **Tech**: Java 25, Gradle, H2 Database, Swing GUI, SLF4J+Logback, Gson (for JSON)
- **Status**: Phase 1 ✅ COMPLETE, Phase 2 ✅ COMPLETE, Phase 3 UI Provisions ✅ COMPLETE

---

## PROJECT ROADMAP (3 PHASES)

### PHASE 1: Standalone POS System ✅ COMPLETE

#### Component 1: Lock Screen
- **Status**: ❌ DEFERRED (will implement later if needed)

#### Component 2: POS Interface ✅ COMPLETE + POLISHED
- Main transaction interface (Quick Keys, Current Sale, Actions zones)
- All dialogs undecorated (no macOS title bar)
- Custom close buttons with drag-to-move functionality
- Settings dialog (clean, no icons)
- Socket Configuration button (no icons, clean text)
- ✅ Icons removed from UI (gear, wrench, trash, checkmarks)
- ✅ Simplified Current Sale table (4 columns: Name, Qty, Price, Line Total)
- ✅ Quantity column now read-only display (70px width)
- ✅ Item deletion via "Void Item" button only
- ✅ On-screen keyboard for search (no input field, auto-closes on outside click)
- ✅ Global barcode scanner (works everywhere except finalized transactions and excluded dialogs)

#### Component 3: Receipt ✅ COMPLETE
- Receipt display with custom styling
- "New Transaction" button flow

#### Component 4: Theme Modes
- **Status**: ❌ DEFERRED (will implement later if needed)

---

### PHASE 2: Multi-POS Journal Viewer ✅ COMPLETE

#### Core Features: ✅ PRODUCTION READY
- Java socket server/client architecture (bidirectional)
- UDP broadcast auto-discovery (port 9999)
- Real-time journal synchronization
- Hybrid protocol support (JSON + Pipe-delimited formats)
- Dead connection cleanup (automatic zombie client removal)
- Active clients monitoring (see who's connected to your server)
- Remote journal storage: `logs/remote-journals/[POS-NAME]/journal.log`
- Console color-coding (GREEN for local, BLUE/YELLOW/etc for remote)
- Socket Configuration Dialog UI with Active Clients section
- Manual connection support (IP:Port) with proper table display
- Connection persistence (saved to `config/socket-config.json`)
- **Status**: ✅ Production Ready, Fully Tested

---

### PHASE 3: Discount Service (Spring Boot REST API) 🔨 UI READY - BACKEND NEXT

- New Spring Boot project (separate from this Java Swing app)
- REST API endpoints for discount calculations
- This POS will connect via HTTP REST calls
- **Status**: UI provisions complete - ready for Spring Boot backend implementation
- **Handoff Document**: See `hand_off_for_phase_3.md` for complete integration guide

#### Phase 3 UI Provisions ✅ COMPLETE
- **Promotional Products**: 17 items with light violet buttons (RGB: 230, 200, 255)
- **Database**: `has_promotion` boolean column added to `price_book` table
- **Discount Button**: Purple button added to Transaction Actions (placeholder ready)
- **Layout**: Current Sale width reduced to 35% for better button readability
- **Ready for Integration**: All UI components styled and positioned for Spring Boot API calls

---

## RECENT WORK COMPLETED (Latest Session)

### v3.0 - Phase 3 UI Provisions Complete

✅ **Promotional Products Display:**
- Added `has_promotion` boolean column to `price_book` table
- Updated `PriceBook` entity with `hasPromotion` field
- Updated `PriceBookDao` to read new column
- Updated `DataSeeder` to parse 6-column TSV format
- 17 products marked as promotional (6 featured + 11 random)
- Light violet button color (RGB: 230, 200, 255) for promotional items
- Dark violet text (RGB: 80, 40, 120) with green price display
- Rounded corners (12px radius) using custom `RoundedBorder` class
- HTML rendering support maintained

✅ **Discount Button Added:**
- New "Discount" button in Transaction Actions zone
- Purple/violet color (RGB: 147, 51, 234)
- Positioned as 4th button (between Change Qty and Total)
- Placeholder handler method `handleDiscount()` ready for integration
- Transaction Actions now has 5 buttons total

✅ **Layout Optimization:**
- Current Sale panel width reduced from 42% to 35%
- Provides +134px more space for Transaction Actions buttons
- Better readability with 5-button layout
- Quick Keys + Actions zone now 65% of screen width
- Responsive to different screen resolutions with 400px minimum

✅ **Database Schema:**
- `price_book` table includes `has_promotion` column
- TSV seed data updated to 6 columns
- All DAO queries updated to handle new field
- Build successful, all tests passing

---

## KEY FILES

### Core Application
- **Main entry**: `src/main/java/org/possystem/Main.java`
- **Transaction logic**: `src/main/java/org/possystem/service/TransactionService.java`
- **Price book**: `src/main/java/org/possystem/service/PriceBookService.java`

### UI Files (ui package)
- `PosInterface.java` (main frame, global scanner integration)
- `ActionsPanel.java` (transaction actions + payment zone, 5 buttons including Discount)
- `CurrentSalePanel.java` (shopping cart table, 4 columns, 35% width)
- `QuickKeysPanel.java` (product grid + search, promotional items with light violet buttons)
- `SocketConfigDialog.java` (Socket Configuration UI)
- `GlobalBarcodeScanner.java` (global keyboard interceptor)

### Socket Files (socket package)
- `SocketService.java` (server + client + discovery logic)
- `PosSystemInfo.java` (model for discovered POS)
- `RemoteJournalEntry.java` (model for journal entries)
- `SocketConfig.java` (JSON persistence model)

### Configuration & Logs
- **Logging config**: `src/main/resources/logback.xml`
- **Local transaction logs**: `logs/transactions/journal.log` (NEVER delete - compliance)
- **Remote journals**: `logs/remote-journals/[POS-NAME]/journal.log`
- **Socket config**: `config/socket-config.json`

### Dependencies (build.gradle.kts)
- H2 Database
- SLF4J + Logback
- Gson (for JSON persistence)

---

## SYSTEM ARCHITECTURE

### Global Barcode Scanner (NEW)
- Application-level keyboard interception via `KeyEventDispatcher`
- Detects rapid typing patterns (scanner signature)
- Automatic item lookup and addition
- **State management**: ENABLED / DISABLED_FINALIZED / DISABLED_DIALOG
- Excludes specific dialogs: Change Qty keypad, Socket Configuration
- Maintains duplicate prevention and scan indicators
- No focus management required - works universally

### Current Sale Table (Simplified)
- **4 columns**: Item Name, Qty, Price, Line Total
- Row selection for single-item operations
- Void via "Void Item" button only
- Quantity changes via "Change Qty" button only
- No checkboxes, no trash icons, no +/- controls
- Clean, minimal interface

### Socket Communication (Phase 2)
- **Server Mode**: Broadcast journal on TCP port (default: 9000)
- **Client Mode**: Connect to multiple remote POS systems
- **Discovery**: UDP broadcast on port 9999
- **Hybrid protocol**: Pipe-delimited + JSON auto-detection
- Dead connection cleanup
- Active client monitoring

### UI Architecture

#### 1. Quick Keys Panel (QuickKeysPanel.java)
- Product grid with search
- Promotional products display with light violet buttons (RGB: 230, 200, 255)
- Dark violet text with green price display for promotional items
- Rounded corners (12px) for promotional buttons
- On-screen keyboard (no input field, auto-closes)
- Filter dropdowns
- Pagination

#### 2. Current Sale Panel (CurrentSalePanel.java)
- **4 columns**: Name (408px), Qty (70px), Price (100px), Line Total (110px)
- 35% of screen width (reduced from 42% for better action button readability)
- **Font sizes**: 16pt (table), 18-22pt (totals)
- Read-only quantity display
- Row selection for operations

#### 3. Actions Panel (ActionsPanel.java)
- **Transaction Actions (60%)** + **Payment (40%)**
- Transaction Actions: 5 buttons
  - Void Item (single-item deletion)
  - Void Basket
  - Change Qty
  - Discount (placeholder - ready for Phase 3 integration)
  - Total
- Scan indicator (green dot)

#### 4. Socket Configuration Dialog (SocketConfigDialog.java)
- This POS Configuration section
- Active Clients Connected to This Server
- Available POS Systems table
- Live Journal Viewer
- No icons, clean text-based buttons

### Transaction Service
- Business logic for all financial operations
- Automatic transaction journal logging
- Integrated with SocketService for broadcasting
- **Methods**: `createTransaction()`, `addItem()`, `voidItem()`, `voidTransaction()`, `processCash()`, `processCard()`
- **Tax rate**: 7% (`TAX_RATE = 0.07`)

---

## IMPORTANT NOTES

- ❌ Do NOT add `System.out.println()` for debugging
- ✅ Transaction operations logged automatically via `TransactionService`
- ✅ Console output color-coded for multi-POS journal viewing
- ❌ Never delete transaction journal files (compliance requirement)
- ✅ All dialogs are undecorated and moveable by header drag
- ✅ Socket connections persist but don't auto-connect on restart
- ✅ Global barcode scanner works everywhere (except excluded dialogs)
- ✅ No icons in UI (clean, text-based interface)

---

## PERSONALIZED DIALOG PATTERN

✅ **All UI dialogs use custom "personalized" dialogs (no JOptionPane)**

✅ **Pattern**: Undecorated dialog with colored header, draggable, centered details, rounded buttons

✅ **Dialog types with corresponding header colors:**
- `showErrorDialog()` → Red header (220, 53, 69)
- `showSuccessDialog()` → Green header (40, 167, 69)
- `showInfoDialog()` → Blue header (23, 162, 184)
- `showWarningDialog()` → Amber header (255, 193, 7)

✅ **All dialogs have:**
- `setUndecorated(true)` + `setResizable(false)`
- `APPLICATION_MODAL`
- `BorderLayout`: NORTH (colored header), CENTER (white details), SOUTH (buttons)
- Header draggable via mouse listeners
- Responsive font scaling (1080p baseline)
- `applyRoundedStyle()` for buttons
- `setLocationRelativeTo()` for centering

✅ **Method signature**: `showXDialog(String title, String message, String details)`

❌ **NEVER use JOptionPane** - always create personalized dialogs

---

## COMMUNICATION PROTOCOL

⚠️ **IMPORTANT**: If my prompt starts AND ends with a "?" (question mark), I'm asking for your opinion or suggestions FIRST before implementing changes. Do NOT make code changes until I confirm which approach to take.

**Example:**
- "? What do you think about adding a search feature here ?" → Give opinion, wait for confirmation
- "Add a search feature here" → Proceed with implementation

---

## MY PREFERENCES

- Professional, efficient, and scalable code
- Step-by-step approach - ONE improvement at a time
- Clean code - no unnecessary changes or over-engineering
- Test after each change before moving to the next
- Clear explanations of what you're doing and why
- When I use "?" at start and end of prompt, give opinion first, don't implement yet

---

## GIT WORKFLOW

✅ User handles all git operations manually (learning git proficiency)
✅ Claude does NOT create commits or push to remote
✅ Claude provides implementation only
✅ User will: commit, push, create PRs, merge branches

---

## SESSION START INSTRUCTIONS

1. Confirm you see the project at `/Users/ed/IdeaProjects/POSSystem`
2. Check current branch (likely develop)
3. Confirm build is successful (`./gradlew build`)
4. Acknowledge you understand the step-by-step approach and the "?" protocol
5. Current priority: Phase 3 UI provisions complete - ready for Spring Boot backend work
6. Reference `hand_off_for_phase_3.md` for integration details
7. Wait for me to provide the next task or direction

---

## ROADMAP PRIORITIES

1. ✅ **DONE**: Phase 1 Complete (POS interface with icon removal and global scanner)
2. ✅ **DONE**: Phase 2 Complete (multi-POS journal viewer)
3. ✅ **DONE**: Phase 3 UI Provisions (promotional buttons, discount button, layout optimization)
4. 🔨 **NEXT**: Spring Boot backend implementation for discount calculations
5. ⏭️ **FUTURE**: Phase 1 deferred items (Lock Screen, Theme Modes) - if needed

---

## PHASE 3 STATUS

### ✅ COMPLETED (Java Swing UI Provisions)
- Promotional products database column (`has_promotion`)
- Light violet button styling for promotional items
- Discount button added to Transaction Actions
- Layout optimized (Current Sale 35%, Actions 65%)
- 17 products marked as promotional
- All entities and DAOs updated
- Build successful

### 🔨 NEXT (Spring Boot Backend)
- Create new Spring Boot project
- **Actual naming chosen**:
  - Group ID: `org.discountengine`
  - Artifact ID: `discount-engine-api`
  - Package: `org.discountengine.api`
- Implement discount calculation REST API
- Three discount types: Promotional, Senior/Veteran, Coupons
- HTTP client integration in POS
- **Complete details**: See `hand_off_for_phase_3.md`

---

# PHASE 3: DISCOUNT SERVICE - DETAILED PLANNING

## OVERVIEW

- New Spring Boot REST API project (separate from Java Swing POS app)
- Three discount types: Promotional, Senior/Veteran, Coupons
- HTTP REST client integration in existing POS
- Discount stacking priority: Promotions → Coupons → Senior/Veteran → Tax

---

## 1. PROMOTIONAL DISCOUNTS ✅ REQUIREMENTS FINALIZED

### Promotion Types (2 types)
- ✅ Buy 1 Get 1 Free (BOGO)
- ✅ Buy X Get Y% Off (e.g., Buy 2 Get 50% Off)

### Business Rules
- ❌ NO time-based validity (no date ranges, no Mon-Fri restrictions)
- ❌ NO expiration dates (keep implementation simple)
- ✅ Auto-apply functionality with user notifications
- ✅ One item = One promotion maximum (no multi-promo conflicts)
- ✅ Stackable with Senior/Veteran discounts

### Database Architecture (UPDATED)

#### POS Database (H2):
- Add `has_promotion` boolean flag to products table
- POS only knows "this item is eligible for A promotion" (not which type)
- **No redundant promotion details stored locally**

#### Spring Boot Discount Service:
- Owns ALL promotion logic and details (BOGO, Buy X Get Y%, etc.)
- Stores promotion types, rules, and item mappings
- **Single source of truth for discount calculations**

### Product Configuration
- Random products from existing price book will be eligible for promos
- **Specific item to add**: UPC `"041594899038"`, Name: `"TB Polar pop 30OZ FOAM"`
  - Gets its own promotional discount
  - Scannable via barcode scanner
- Each item eligible for only ONE promotion

### UI Requirements

#### Quick Keys Zone:
- **Promotional items have PURPLE/VIOLET button color**
- Distinct from existing colors (Green, Red, Blue, Amber)
- Easy visual identification of promo items at first glance
- **One color for ALL promotional items** (promotion type retrieved from Spring Boot)

#### Current Sale Zone:
- Show promotion details clearly (e.g., "Buy 1 Get 1 Free")
- Display as separate discount line items:

```
Item Name              Qty    Price    Line Total
Coca Cola              2      $1.99    $3.98
- Buy 1 Get 1 Free     -1     -$1.99   -$1.99
```

- Transparent display for easier void operations

### API Endpoint

**POST /api/discounts/calculate-promotions**

Request:
```json
{
  "items": [
    {"upc": "string", "quantity": 2, "price": 1.99}
  ]
}
```

Response:
```json
{
  "promotions": [
    {
      "promoId": "string",
      "promoName": "Buy 1 Get 1 Free",
      "affectedItems": ["upc"],
      "discountAmount": 1.99,
      "message": "Buy 1 Get 1 Free applied!"
    }
  ],
  "totalDiscount": 1.99
}
```

### POS Integration
- **When to call**: After every item addition/removal
- **Notification**: Show popup/indicator when promo auto-applies
- **Display**: Show discount as separate line item in Current Sale table
- **Receipt**: List promo name and savings

---

## 2. SENIOR/VETERAN DISCOUNT ✅ REQUIREMENTS FINALIZED

### Discount Rates
- **Veteran Discount**: 10% off
- **Senior Discount**: 5% off

### Application Rules
- **Scope**: Applied to entire cart subtotal (all items, including promotional items)
- **Before tax**: Calculated on subtotal before tax
- **Stacking**:
  - ✅ Can stack with promotional discounts
  - ✅ Can stack with coupons (if implemented)
  - ❌ **Mutually exclusive**: Cannot apply both Senior AND Veteran in same transaction

### Authorization
- **No verification required**: Any cashier can apply with button press
- **No manager override**: Simple button click
- **No ID tracking**: No customer verification/ID logging needed
- **No audit trail**: Standard transaction logging is sufficient

### User Flow
- **When to apply**: Anytime during active transaction (before Total button clicked)
- **Disabled after Total**: Once Total is clicked, discount cannot be applied
- **Removal**: Cashier can remove discount anytime before Total (no manager approval needed)
- **One active at a time**: Applying Senior removes Veteran (and vice versa)

### UI Requirements

#### Actions Panel - New Discount Buttons:

```
Transaction Actions:
  - Change Qty
  - Void Item
  - Void Basket

Discount Actions: (NEW SECTION)
  - Senior Discount (5%)
  - Veteran Discount (10%)
  - Remove Discount (visible only when discount applied)
```

#### Current Sale Display:

```
Item Name              Qty    Price    Line Total
Coca Cola              2      $1.99    $3.98
- Buy 1 Get 1 Free     -1     -$1.99   -$1.99
Bread                  1      $2.50    $2.50

Subtotal: $4.49
Veteran Discount (10%): -$0.45
Tax (7%): $0.28
Total: $4.32
```

### API Endpoint

**POST /api/discounts/apply-senior-veteran**

Request:
```json
{
  "cartSubtotal": 4.49,
  "discountType": "VETERAN" | "SENIOR"
}
```

Response:
```json
{
  "discountType": "VETERAN",
  "discountRate": 0.10,
  "discountAmount": 0.45,
  "newSubtotal": 4.04
}
```

---

## 3. COUPON SUPPORT (BARCODE SCANNER) ✅ REQUIREMENTS FINALIZED

### Available Coupon Codes

The system supports exactly **4 hardcoded coupon codes**:

#### 1. SAVE20
- **Type**: Fixed amount discount
- **Value**: $20.00 off cart total
- **Validation**: Minimum cart subtotal of $100.00 required
- **Expiration**: Set to expire 1 year from current date (2027-03-18)

#### 2. ITEM15OFF
- **Type**: Item-specific discount
- **Value**: $1.50 off the single highest-priced item in cart
- **Validation**: No minimum purchase requirement
- **Expiration**: Set to expire 1 year from current date (2027-03-18)

#### 3. EXPIRED10
- **Type**: Expired coupon (for testing)
- **Value**: Any discount type
- **Validation**: Already expired (expired 1 year ago: 2025-03-18)
- **Purpose**: Test expiration validation logic

#### 4. MEMBER10
- **Type**: Percentage discount
- **Value**: 10% off cart total
- **Validation**: No membership verification (all coupon holders are members)
- **Expiration**: Set to expire 1 year from current date (2027-03-18)

### Business Rules

- ✅ **Multiple applications**: Same coupon can be applied multiple times in one transaction
- ✅ **Coupon stacking**: Different coupon codes can be combined in same transaction
- ✅ **Expiration validation**: Only validate based on expiration date
- ❌ **No one-time use tracking**: Coupons can be reused indefinitely (until expired)
- ❌ **No prefix/length detection**: Direct lookup only - validate if code exists in approved list
- ✅ **Stackable with other discounts**: Can combine with Promotions and Senior/Veteran discounts

### Validation Logic

**Coupon is valid if:**
1. Coupon code exists in approved list (SAVE20, ITEM15OFF, EXPIRED10, MEMBER10)
2. Current date is before expiration date
3. Minimum purchase requirement met (if applicable - only SAVE20)

**Coupon is invalid if:**
- Code not found in system
- Expired (current date >= expiration date)
- Minimum purchase not met (SAVE20 only)

### UI Requirements

#### Manual Entry Flow:
- **Button**: "Apply Coupon" button in Discount Actions section
- **Input**: On-screen keyboard dialog for coupon code entry
- **Case-insensitive**: Accept "save20", "SAVE20", "SaVe20"
- **Display**: Show as separate line item in Current Sale table
- **Format**: "Coupon (SAVE20): -$20.00" or "Coupon (ITEM15OFF) Highest Item: -$1.50"

#### Barcode Scanner Integration:
- Use existing `GlobalBarcodeScanner` for coupon barcode scanning
- Scanned coupon codes trigger same validation as manual entry
- Same duplicate prevention logic (800ms window)

#### Current Sale Display:

```
Item Name              Qty    Price    Line Total
Coca Cola              2      $1.99    $3.98
- Buy 1 Get 1 Free     -1     -$1.99   -$1.99
Bread                  3      $5.00    $15.00
- Coupon (ITEM15OFF)                   -$1.50

Subtotal: $14.49
- Coupon (MEMBER10)                    -$1.45
Tax (7%): $0.91
Total: $13.95
```

#### Error Handling:
- **Not found**: "Coupon code not found"
- **Expired**: "Coupon expired on [date]"
- **Min purchase not met**: "Minimum purchase of $100 required for SAVE20"
- **Display**: Use personalized warning dialog (amber header)

#### Removal:
- Cashier can void individual coupon line items
- Same as voiding regular items (select + "Void Item" button)

### API Endpoint

**POST /api/discounts/validate-coupon**

Request:
```json
{
  "couponCode": "SAVE20",
  "cartItems": [
    {"upc": "string", "quantity": 2, "price": 5.00}
  ],
  "cartSubtotal": 120.00,
  "currentDate": "2026-03-18"
}
```

Response (Valid):
```json
{
  "valid": true,
  "couponCode": "SAVE20",
  "discountType": "FIXED_AMOUNT",
  "discountAmount": 20.00,
  "description": "$20 off your purchase",
  "expirationDate": "2027-03-18"
}
```

Response (Invalid - Expired):
```json
{
  "valid": false,
  "couponCode": "EXPIRED10",
  "reason": "EXPIRED",
  "message": "Coupon expired on 2025-03-18"
}
```

Response (Invalid - Min Purchase):
```json
{
  "valid": false,
  "couponCode": "SAVE20",
  "reason": "MIN_PURCHASE_NOT_MET",
  "message": "Minimum purchase of $100.00 required",
  "currentSubtotal": 50.00,
  "requiredSubtotal": 100.00
}
```

### POS Integration

- **When to apply**: Manual button press or barcode scan
- **Validation**: Call API before adding coupon to cart
- **Display**: Add as separate line item if valid
- **Notification**: Show success/error dialog based on validation result
- **Receipt**: List coupon code and savings amount

---

## DISCOUNT STACKING PRIORITY (Recommended)

1. **Automatic Promotions** (item-level) - Applied first
2. **Coupons** - Applied to subtotal after promos
3. **Senior/Veteran Discount** - Applied last to eligible items
4. **Tax** - Calculated on final discounted subtotal

### Example Calculation:

```
Cart Items:
  Coca Cola (promo item): $10.00 × 2 = $20.00
  Bread: $5.00

Step 1 - Apply Promotional Discounts:
  Buy 1 Get 1 Free on Coca Cola: -$10.00
  Subtotal after promos: $15.00

Step 2 - Apply Coupons:
  MEMBER10 (10% off): -$1.50
  Subtotal after coupons: $13.50

Step 3 - Apply Senior/Veteran Discount:
  Senior (5% off $13.50): -$0.68
  Subtotal after all discounts: $12.82

Step 4 - Calculate Tax:
  Tax (7% of $12.82): +$0.90

Final Total: $13.72
```

---

## SPRING BOOT API ARCHITECTURE (Recommended)

### Project Structure

```
discount-service/
├── src/main/java/
│   ├── controller/
│   │   └── DiscountController.java
│   ├── service/
│   │   ├── PromotionService.java
│   │   ├── SeniorVeteranService.java
│   │   └── CouponService.java
│   ├── model/
│   │   ├── Promotion.java
│   │   ├── Coupon.java
│   │   ├── DiscountRequest.java
│   │   └── DiscountResponse.java
│   ├── repository/
│   │   ├── PromotionRepository.java
│   │   └── CouponRepository.java
│   └── config/
│       └── DiscountConfig.java
└── src/main/resources/
    └── application.yml
```

### Tech Stack
- Spring Boot 3.x
- Spring Data JPA (H2 or PostgreSQL for prod)
- Spring Web (REST endpoints)
- Lombok (reduce boilerplate)
- MapStruct (DTO mapping)
- Optional: Spring Cache (for active promotions)

---

## POS UI CHANGES NEEDED

### Actions Panel - New Discount Section

**Transaction Actions (current buttons):**
- Change Qty
- Void Item
- Void Basket

**NEW Discount Section:**
- Senior Discount (5%)
- Veteran Discount (10%)
- Apply Coupon (manual entry or barcode scan)
- Remove Discount (visible only when discount applied)

### Current Sale Table - Discount Display

Show discounts as separate line items (cleaner, easier to void):

```
Item Name              Qty    Price    Line Total
Coca Cola              2      $1.99    $3.98
- Buy 1 Get 1 Free     -1     -$1.99   -$1.99
Bread                  3      $5.00    $15.00
- Coupon (ITEM15OFF)                   -$1.50

Subtotal: $14.49
- Coupon (MEMBER10)                    -$1.45
Veteran Discount (10%): -$1.30
Tax (7%): $0.82
Total: $12.76
```

---

## DISCOUNT MANAGEMENT & ADMINISTRATION

### Admin Interface
- **Implementation**: REST API endpoints with Swagger/OpenAPI documentation
- **Purpose**: Create, update, delete promotions and coupons via API testing tools (Postman, Swagger UI)
- **No Web UI**: Admin operations through API only (simplifies Phase 3 scope)

### Audit Trail
- ❌ **No audit logging**: Do not implement discount application tracking
- ✅ **Standard logging only**: Transaction journal already logs cart items and totals
- **Rationale**: Keep Phase 3 focused on core discount calculation logic

---

## NEXT STEPS - PHASE 3 IMPLEMENTATION

### ✅ 1. POS Database Changes (H2) - COMPLETE
- ✅ Added `has_promotion` boolean column to `price_book` table
- ✅ Marked 17 promotional items in price book (6 featured + 11 random)
- ✅ Updated all entities, DAOs, and seed data

### ✅ 2. POS UI Updates - COMPLETE
- ✅ Light violet button color for promotional items in Quick Keys
- ✅ Discount button added to Transaction Actions zone (placeholder)
- ✅ Current Sale width reduced from 42% to 35%
- ✅ Transaction Actions now has 5 buttons with better readability
- ✅ Custom `RoundedBorder` class for promotional buttons

### 🔨 3. Spring Boot Discount Service Setup - TODO
- Create new Spring Boot 3.x project structure
  - **Recommended**: `org.possystem` / `pos-backend` / `org.possystem.backend`
- Setup Spring Data JPA (H2 database)
- Configure Swagger/OpenAPI documentation
- Define database schema for promotions and coupons

### 🔨 4. API Implementation - TODO
- Implement `/api/discounts/calculate-promotions` endpoint
- Implement `/api/discounts/apply-senior-veteran` endpoint
- Implement `/api/discounts/validate-coupon` endpoint
- Seed database with 4 coupon codes (SAVE20, ITEM15OFF, EXPIRED10, MEMBER10)

### 🔨 5. POS HTTP Client Integration - TODO
- Add HTTP client library (Java 11+ HttpClient or OkHttp)
- Create DiscountApiClient class for API communication
- Wire up `handleDiscount()` method in ActionsPanel
- Handle network failures gracefully (offline mode support)
- Configure discount service URL in settings
- Update Current Sale table to display discount line items
- Implement discount selection dialog (Senior/Veteran/Coupon options)

### 🔨 6. Testing & Documentation - TODO
- ✅ Handoff document created (`hand_off_for_phase_3.md`)
- Test all discount types and stacking scenarios
- Verify expiration validation
- Test minimum purchase requirements

---

## DO NOT

- Run the application automatically
- Make suggestions or ask questions unless prompted with "?"
- Start making changes without my specific instruction
- Work on multiple items at once
- Create git commits or push to remote

## DO

- Wait for me to provide ONE specific improvement or task
- Implement exactly what I ask for
- Build and verify compilation
- Provide clear explanations
- Wait for my feedback before moving to the next item

---

## QUICK REFERENCE

### Start POS
```bash
./gradlew run
```

### Socket Configuration Location
Settings (gear icon removed - now clean button) → Socket Configuration button

### Socket Ports
- **Discovery**: UDP 9999 (auto-discovery broadcasts)
- **Server**: TCP 9000, 9001, 9002, etc. (configurable per POS)

### Barcode Scanner
- Works globally via `KeyEventDispatcher`
- Enabled during active transaction
- Disabled when: Total clicked, Change Qty dialog, Socket Config dialog
- Re-enabled: New transaction, Back to Cart confirmed

### Current Sale Table
- **4 columns**: Name (408px), Qty (70px), Price (100px), Line Total (110px)
- No checkboxes, no icons
- Row selection for operations
- Void Item button for deletion
- Change Qty button for quantity changes

---

**Phase 3 UI provisions complete! Ready to implement Spring Boot backend or work on other improvements based on your direction!**

**Next Session**: Implement Spring Boot discount service - see `hand_off_for_phase_3.md` for complete integration guide.
