# POS System - Project Handoff Document

I'm continuing work on my Java POS System project. Here's the context from my previous session:

**PROJECT LOCATION**: `/Users/ed/IdeaProjects/POSSystem`

---

## PROJECT STATUS

All phases complete and deployed to production! The discount engine API has been deployed to the web and successfully tested with the POS system.

---

## CURRENT STATE

- **Branch**: feature/provisioned-for-phase-3 (current working branch)
- **Main Branch**: main
- **Build**: ✅ Successful
- **Tech**: Java 25, Gradle, H2 Database, Swing GUI, SLF4J+Logback, Gson (for JSON)
- **Status**: Phase 1 ✅ COMPLETE, Phase 2 🔧 NEEDS POLISH, **Phase 3A ✅ COMPLETE, Phase 3B ✅ COMPLETE, Phase 3C ✅ COMPLETE**

---

## PROJECT ROADMAP (3 PHASES)

### PHASE 1: Standalone POS System ✅ COMPLETE

#### Component 1: Authentication/Lock Screen ✅ COMPLETE
- **Status**: ✅ PRODUCTION READY
- **Implementation**: Username/password authentication for accessing Settings dialogs
  - API Configuration access protection (requires login)
  - Socket Configuration access protection (requires login)
  - 2-minute session timeout with activity refresh
  - SHA-256 password hashing
  - Default admin user (username: admin, password: admin)
  - Logout buttons in config dialogs
  - LoginDialog with on-screen keyboard support
  - Physical keyboard input fully functional

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

---

### PHASE 2: Multi-POS Journal Viewer 🔧 NEEDS POLISH

#### Core Features: ✅ IMPLEMENTED
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

#### Known Issues / Polish Needed: 🔧
- ⚠️ **Multi-POS compatibility**: Works on one POS but not connecting properly with other POS instances
- 🔧 **Disconnect buttons**: Need to review and polish disconnect functionality
- 🔧 **Socket discovery**: Implement automatic reading/scanning of all available Java sockets
- **Status**: Core features implemented, requires debugging and polish for production use

---

### PHASE 3: Discount Service (Spring Boot REST API) ✅ ALL PHASES COMPLETE!

- **Discount Engine API**: ✅ Deployed to production (web)
- **API Configuration**: Configurable via POS Settings → API Configuration dialog
- **Implementation Approach**: Incremental (3A → 3B → 3C)
- **Current Status**:
  - **Phase 3A (Promotional Discounts)**: ✅ COMPLETE
  - **Phase 3B (Senior/Veteran Discounts)**: ✅ COMPLETE
  - **Phase 3C (Coupon Support)**: ✅ COMPLETE
- **Planning Document**: See `phase_3_on_progress.md` for current progress and next steps
- **API Reference**: See `hand_off_for_phase_3.md` for API endpoints and schemas

#### Phase 3 UI Provisions ✅ COMPLETE
- **Promotional Products**: 12 promotional items with light violet buttons (RGB: 230, 200, 255)
- **Featured Products**: 48 total products (12 promotional + 36 regular) across 4 pages
- **Randomization**: Promotional products distributed across all pages (~3 per page)
- **Database**: `has_promotion` boolean column added to `price_book` table
- **Discount Button**: Purple button added to Transaction Actions (fully integrated)
- **Layout**: Current Sale width reduced to 35% for better button readability
- **Performance**: API optimization ensures regular items add instantly (no lag)

#### Phase 3 Integration Plan ✅ COMPLETE
- **HTTP Client**: Java 11+ HttpClient (no new dependencies)
- **API Configuration**: Settings Dialog with URL field and Test Connection button
- **Discount Display**: Separate line items in Current Sale table (easier to void)
- **UI Approach**: Single "Discount" button opens dialog with 3 options
- **Error Handling**: Disable discount button if API unavailable, allow transactions to proceed
- **Promo Notifications**: Toast popup (upper-right corner, 3s auto-dismiss)
- **Database Schema**: Add `item_type` column to `transaction_items` table
- **Coupon Entry**: Manual input + barcode scanning support
- **Coupon Limit**: One coupon per transaction
- **Senior/Veteran**: Mutually exclusive, auto-remove when switching

---

## RECENT WORK COMPLETED (Latest Session)

### v4.0 - Authentication System & UI Polish (2026-03-25) ✅

#### Authentication System - PRODUCTION READY ✅

✅ **Complete Authentication Implementation:**
- Created `User.java` entity (Java record with SHA-256 password hashing)
- Created `UserDao.java` with password verification methods
- Created `AuthService.java` with 2-minute session timeout management
- Created `LoginDialog.java` with full UI and keyboard support
- Updated `DatabaseManager.java` to include users table
- Updated `DataSeeder.java` to seed default admin user (username: admin, password: admin)
- Integrated authentication into `ApiConfigDialog.java` and `SocketConfigDialog.java`
- Added Logout buttons (red, rounded with black outline) to both config dialogs

✅ **LoginDialog Features:**
- Modal authentication dialog with upper center positioning (15% from top)
- Horizontal layout: labels on left, input fields maximize full width
- Double-click activation for on-screen keyboard
- Physical keyboard input fully functional
- Simplified lowercase-only keyboard (no shift/uppercase)
- Colored special keys with rounded style and darker outline:
  - Backspace (←): Amber #FFC107
  - Clear: Red #DC3545
  - Enter: Green #28A745
- Cancel and Login buttons with rounded corners and black outline
- Dialog height optimized (reduced from 400 to 300)
- GlobalBarcodeScanner properly disabled before showing dialog

✅ **Session Management:**
- 2-minute session timeout with activity refresh
- `checkAuthAndRefresh()` method for automatic session validation
- Logout functionality with session cleanup
- Username display in config dialog headers

✅ **Database Schema:**
- `users` table with SHA-256 password hashing
- Default admin user seeded on first run
- Proper foreign key relationships maintained

✅ **Security Fixes:**
- Fixed keyboard input capturing by GlobalBarcodeScanner
- Proper dialog exclusion from global listeners
- Session expiration handling

#### Data Fixes ✅

✅ **Pricebook Cleanup:**
- Removed duplicate "CIR K POLAR POP LARG" entry (line 713, UPC: 028400726580)
- Kept original entry at quick_key_position 1 (UPC: 041594899038)
- DataSeeder automatically reflects changes on database reset

**Status:** Authentication system fully implemented and production ready! 🔐

---

### v3.5 - Performance Optimization & Promotional Products Enhancement (2026-03-25) ✅

#### API Call Optimization - PRODUCTION READY ✅

✅ **Conditional API Calls Implementation:**
- Added `hasActiveCoupons()` helper method to check coupon existence
- Added `hasPromotionalItems()` helper method to check promotional item existence
- Added `checkAndApplyPromotionalDiscount(String upc)` for single-item checks
- Optimized `addItem()` method to skip API calls for regular items
- Optimized `voidItem()` method to conditionally check promotions/coupons
- Optimized `updateQuantity()` method to check only the specific item
- Optimized `deleteSelectedItems()` method to conditionally recalculate

✅ **Performance Improvements:**
- **Regular items**: Add instantly with ZERO API calls (no lag)
- **Promotional items**: Only 1 API call per item addition (not all items)
- **Coupons**: Only recalculated when coupons actually exist in cart
- Console logs show optimization messages confirming skipped API calls
- Dramatic performance boost for mixed carts (regular + promotional items)

✅ **Optimization Logic:**
```
Add regular item + no coupons → SKIP all API calls
Add regular item + has coupons → Only recalculate coupons
Add promotional item → Only call API for that ONE item
Void item → Only recalculate if promotional items/coupons remain
```

#### Promotional Products Enhancement ✅

✅ **48 Featured Products (4 Pages):**
- 12 promotional products (has_promotion=true) with purple buttons
- 36 regular products (has_promotion=false) to fill remaining positions
- Products randomized across 4 pages of Quick Keys grid
- Promotional products distributed evenly: ~3 per page

✅ **Promotional Product Positions:**
- Page 1 (1-12): Positions 1, 7, 10
- Page 2 (13-24): Positions 15, 18, 22
- Page 3 (25-36): Positions 26, 30, 34
- Page 4 (37-48): Positions 39, 42, 45
- First CIR K POLAR POP LARG kept at position 1 (highest priority)

✅ **Database Reset Scripts:**
- Created `reset_database.sh` - Deletes ~/possystemdb.mv.db to force reseed
- Created `apply_promotion_update.sh` - Helper with database update instructions
- Created `update_promotions.sql` - SQL script for manual promotional flag updates
- Updated `pricebook.tsv` with randomized positions for all 48 featured products

✅ **Testing Completed:**
- All discount types working flawlessly (promotional, senior, veteran, coupon)
- Performance verified: regular items add instantly (no lag)
- Promotional items trigger discounts correctly at quantity 2+
- Coupons validate and recalculate in real-time
- Mixed scenarios tested (promotions + coupons + senior/veteran)

**Status:** Performance optimization complete, promotional products enhanced! 🚀

---

### v3.4 - Phase 3C COMPLETE! (2026-03-23) ✅

#### Phase 3C: Coupon Support - PRODUCTION READY ✅

✅ **Refactored Discount Engine API Integration:**
- Updated `CouponValidationResponse` DTO with new fields: `triggered`, `remainingAmount`, `errorType`
- API now separates `valid` (coupon exists/not expired) from `triggered` (minimum purchase met)
- Progressive disclosure pattern: Accept coupons early, validate at Total

✅ **Empty Cart Validation:**
- Dummy data approach: Sends $0.01 validation item to API when cart is empty
- Validates coupon existence without requiring items in cart
- Rejects invalid codes (e.g., "FAKEXXXX") with proper error messages
- Accepts valid coupons with $0.00 discount (recalculates when items added)

✅ **Triggered Flag Implementation (Option 1):**
- Added check for `triggered` field when applying coupons to non-empty carts
- Prevents premature discount application (e.g., $0.99 item + ITEM15OFF doesn't make cart $0)
- Discount amount = $0.00 when `triggered: false`
- Discount amount = API value when `triggered: true`
- Fixes "Nothing to Total" issue with low-priced items

✅ **Progressive Threshold Crossing:**
- `recalculateCouponDiscounts()` auto-updates discounts as cart changes
- Coupon starts at $0.00, grows to full discount when minimum reached
- Real-time updates in Current Sale panel
- Seamless UX for building cart with coupon already applied

✅ **Payment Flow:**
- Non-blocking warning dialog at Total if minimum not met (yellow/amber)
- Payment buttons remain ENABLED (allows customer to proceed or add items)
- `removeUntriggeredCoupons()` silently removes invalid coupons before payment
- Receipt only shows triggered discounts

✅ **Receipt Enhancement:**
- Fixed HashMap persistence issue (was being cleared before receipt generation)
- Receipt now shows specific coupon codes: "Coupon (SAVE20): -$2.50"
- HashMap persists through receipt generation, cleared only on new transaction

✅ **Discount Dialog Height Increase:**
- Increased dialog height from 32% to 38% of screen (minimum 300px → 360px)
- Better spacing for multiple discounts (Senior/Veteran + Coupon)
- Remove buttons clearly visible without cramming

✅ **Single Scanner Architecture:**
- **DISABLED ActionsPanel's redundant barcode scanner**
- GlobalBarcodeScanner is now the ONLY active scanner
- Handles both products AND coupons via single handler
- Eliminated conflicting "Product Not Found" dialogs for coupons
- Eliminated redundant database queries (was querying twice per scan)
- Cleaner architecture: Single source of truth for all scanning

✅ **Toast Notifications:**
- Success toast on coupon application (manual or barcode)
- Format: "SAVE20 applied"
- Non-intrusive, auto-dismiss

✅ **Debug Logging:**
- Comprehensive HashMap lifecycle tracking
- Coupon validation debug output
- Empty cart detection logging
- Triggered/not triggered status logging

✅ **All Coupon Types Tested:**
- SAVE20 (percentage, $20 minimum) ✅
- ITEM15OFF ($1.50 off highest item, $2 minimum) ✅
- MEMBER10 (10% off, no minimum) ✅
- EXPIRED10 (expired for testing) ✅

✅ **Test Scenarios Passed:**
- Empty cart + coupon ✅
- Below minimum + coupon ✅
- Above minimum + coupon ✅
- Progressive threshold crossing ✅
- Invalid coupon rejection ✅
- Expired coupon rejection ✅
- Receipt shows specific codes ✅
- Payment with untriggered coupon ✅
- Senior/Veteran + Coupon stacking ✅
- One coupon per transaction rule ✅
- Barcode scanning (products + coupons) ✅

**Status:** Phase 3C PRODUCTION READY! 🎉

---

### v3.3 - Phase 3A & 3B COMPLETE! (2026-03-21) ✅

#### Phase 3B: Senior/Veteran Discounts - COMPLETE ✅

✅ **Database Schema (New Approach):**
- Created separate `transaction_discounts` table (cleaner than item_type column approach)
- Schema: id, transaction_id, discount_type, discount_amount, item_id (nullable), status, created_at
- Supports cart-level discounts (item_id=NULL) and item-specific discounts
- Created `TransactionDiscount` entity (Java record)
- Created `TransactionDiscountDao` with full CRUD operations

✅ **DTO Classes:**
- Created `SeniorVeteranDiscountResponse` record
- Created `PromotionalDiscountResponse` record

✅ **HTTP Client:**
- Created `DiscountApiClient` with Java 11+ HttpClient (no new dependencies)
- Implemented `calculateSeniorDiscount(double cartSubtotal)` method
- Implemented `calculateVeteranDiscount(double cartSubtotal)` method
- Implemented `calculatePromotionalDiscount()` method
- Implemented `isApiAvailable()` health check
- Base URL: `http://localhost:8080/api/discounts` (configurable)
- Timeout: 5 seconds

✅ **DiscountDialog UI - Fully Polished:**
- Complete 2x2 grid layout (Senior, Veteran, Apply Coupon, Cancel buttons)
- Purple color scheme: RGB(138,43,226) for Senior/Veteran, RGB(147,51,234) for Apply Coupon
- Black text outlines on all buttons for better readability
- "Currently Applied" status panel on right side with vertical divider
- Dynamic button text changes based on active discount
- Remove buttons visible only when discounts active
- Modal, draggable, centered dialog (27% width, 32% height, min 490x300)
- No hover effects, no success dialog popups (per user preference)
- **Dialog auto-closes after discount application**

✅ **CurrentSalePanel Display - Totals Section:**
- Senior/Veteran discounts display in totals section
- Format: "Senior Discount (5%): -$X.XX" / "Veteran Discount (10%): -$X.XX"
- Gray italic text (18pt) for discount lines
- Tax calculated on discounted subtotal
- Dynamic BoxLayout for flexible discount display

✅ **Receipt Updates:**
- Senior/Veteran discounts shown in totals section
- Consistent formatting with CurrentSalePanel
- Discounts calculated correctly before tax

✅ **TransactionService Integration:**
- Added `getSubtotal()` - Returns subtotal including discounts
- Added `getActiveDiscounts()` - Returns list of TransactionDiscount records
- Added `hasDiscountType(String type)` - Checks if discount type applied
- Added `applySeniorVeteranDiscount(String type, double amount)` - Applies discount
- Added `removeDiscountByType(String type)` - Removes discount
- Added `removeDiscountsByItemId(int itemId)` - Removes item-linked discounts
- Journal logging for all discount operations

✅ **Integration Complete:**
- ActionsPanel wired to open DiscountDialog
- PosInterface passes DiscountApiClient to ActionsPanel
- Discount button functional in Transaction Actions zone
- All tests passing

#### Phase 3A: Promotional Discounts - COMPLETE ✅

✅ **Data Synchronization:**
- Created `DISCOUNT_API_DATA_SYNC_PROMPT.md` with all 12 promotional product UPCs
- Documented data sync issue between POS and API databases
- Provided instructions for API team to configure discount_percentages table
- Emphasized UPCs must be stored as STRING type (not numeric)

✅ **ToastNotification UI Component:**
- Created `ToastNotification.java` - JWindow-based notification
- Upper-right corner positioning with slide-in animation
- Green success theme (RGB 212,237,218 background, RGB 40,167,69 border)
- 3-second auto-dismiss timer
- Static helper method: `showToast(Window parent, String title, String message)`
- **Enhanced with product name**: Title: "Buy 2+ Get 25%" / Message: "{Product Name} - You saved $X.XX"

✅ **Service Layer Integration:**
- `TransactionService.checkAndApplyPromotionalDiscounts()` implemented
- Iterates cart items with has_promotion flag
- Calls API for each promotional item
- Creates discount records linked to item_id
- Toast shown only on first trigger (using triggeredPromotions HashSet)
- Removes discounts when threshold not met
- ToastCallback interface for UI integration

✅ **CurrentSalePanel - INLINE PROMOTIONAL DISCOUNTS:**
- **Promotional discounts show INLINE below their items** (not in totals)
- Format: "  Buy {qty}+ Save {percent}%"
- Example: "  Buy 2+ Save 25%" appears right below the item
- Gray italic text (14pt) for discount rows
- Percentage calculated dynamically from discount amount
- Table rows alternate between items and their discounts
- Custom TableRow wrapper class for mixed row types
- Custom DiscountAwareTextRenderer for styling
- Senior/Veteran discounts remain in totals section (cart-level)

✅ **Receipt - INLINE PROMOTIONAL DISCOUNTS:**
- **Promotional discounts show INLINE below their items**
- Format matches CurrentSalePanel: "  Buy {qty}+ Save {percent}%"
- Map-based approach to link discounts to items
- Senior/Veteran/Coupon discounts remain in totals section
- Clean, consistent formatting across UI and receipt

✅ **ActionsPanel Integration:**
- Payment handlers fetch discounts BEFORE payment
- `showReceiptDialog()` and `buildReceiptText()` accept discounts parameter
- Receipt displays all discount types correctly
- Discounts shown before tax calculation

✅ **All Tests Passing:**
- Promotional discount triggers on quantity threshold
- Toast notification appears with product name
- Discount displays inline below item in CurrentSalePanel
- Discount displays inline below item on receipt
- Discount amount updates when quantity changes
- Discount removed when item voided
- Multiple promotional items each get their own discount
- Senior/Veteran discounts display in totals
- Data sync resolved between POS and API

⏭️ **Next:** Phase 3C - Coupon Support

### v3.1 - Phase 3 Integration Planning Complete + UI Improvement

✅ **Void Item UX Improvement:**
- Removed confirmation dialog from "Void Item" button
- Item deletion now instant when row selected + button clicked
- Faster workflow for cashiers
- Modified: `PosInterface.java:648-674`

✅ **Phase 3 Integration Plan Complete:**
- Comprehensive implementation plan created: `PHASE_3_IMPLEMENTATION_PLAN.md`
- All technology decisions finalized
- Database schema changes defined
- Incremental approach: Phase 3A (Promotional) → 3B (Senior/Veteran) → 3C (Coupons)
- HTTP Client: Java 11+ (no new dependencies)
- API URL: Configurable via Settings Dialog
- Discount display: Separate line items in Current Sale table
- UI approach: Single "Discount" button opens dialog with 3 options
- Error handling: Disable discount button only, transactions can proceed
- Promotional notifications: Toast popup (upper-right, 3s auto-dismiss)
- One coupon per transaction rule enforced

### v3.0 - Phase 3 UI Provisions Complete

✅ **Promotional Products Display:**
- Added `has_promotion` boolean column to `price_book` table
- Updated `PriceBook` entity with `hasPromotion` field
- Updated `PriceBookDao` to read new column
- Updated `DataSeeder` to parse 6-column TSV format
- 12 products marked as promotional (API-verified only)
- 48 featured products total (12 promotional + 36 regular) across 4 pages
- Light violet button color (RGB: 230, 200, 255) for promotional items
- Dark violet text (RGB: 80, 40, 120) with green price display
- Rounded corners (12px radius) using custom `RoundedBorder` class
- HTML rendering support maintained
- Randomized positions to distribute promotions across all pages

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
- **Discount API client**: `src/main/java/org/possystem/service/DiscountApiClient.java`
- **Authentication**: `src/main/java/org/possystem/service/AuthService.java` ✨ NEW

### UI Files (ui package)
- `PosInterface.java` (main frame, global scanner integration, authentication integration)
- `ActionsPanel.java` (transaction actions + payment zone, 5 buttons including Discount)
- `CurrentSalePanel.java` (shopping cart table, 4 columns, 35% width)
- `QuickKeysPanel.java` (product grid + search, promotional items with light violet buttons)
- `SocketConfigDialog.java` (Socket Configuration UI with authentication and logout button)
- `ApiConfigDialog.java` (API Configuration UI with authentication and logout button)
- `DiscountDialog.java` (discount options dialog - senior, veteran, coupon)
- `LoginDialog.java` (authentication dialog with on-screen keyboard) ✨ NEW
- `GlobalBarcodeScanner.java` (global keyboard interceptor)

### Discount System Files ✨
- **Entity**: `src/main/java/org/possystem/entity/TransactionDiscount.java`
- **DAO**: `src/main/java/org/possystem/dao/TransactionDiscountDao.java`
- **DTOs**:
  - `src/main/java/org/possystem/dto/SeniorVeteranDiscountResponse.java`
  - `src/main/java/org/possystem/dto/PromotionalDiscountResponse.java`
  - `src/main/java/org/possystem/dto/CouponValidationResponse.java`
- **UI Components**:
  - `src/main/java/org/possystem/ui/DiscountDialog.java`
  - `src/main/java/org/possystem/ui/ToastNotification.java`
- **API Client**:
  - `src/main/java/org/possystem/service/DiscountApiClient.java`

### Authentication System Files ✨ NEW
- **Entity**: `src/main/java/org/possystem/entity/User.java`
- **DAO**: `src/main/java/org/possystem/dao/UserDao.java`
- **Service**: `src/main/java/org/possystem/service/AuthService.java`
- **UI Component**: `src/main/java/org/possystem/ui/LoginDialog.java`
- **Features**:
  - SHA-256 password hashing
  - 2-minute session timeout
  - On-screen keyboard support
  - Physical keyboard input
  - Logout functionality in config dialogs

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

### Global Barcode Scanner (SINGLE SCANNER ARCHITECTURE)
- Application-level keyboard interception via `KeyEventDispatcher`
- Detects rapid typing patterns (scanner signature)
- **Handles ALL scanning**: Products AND Coupons (single handler)
- Automatic item lookup with fallback to coupon validation
- **State management**: ENABLED / DISABLED_FINALIZED / DISABLED_DIALOG
- Excludes specific dialogs: Change Qty keypad, Socket Configuration
- Maintains duplicate prevention and scan indicators
- No focus management required - works universally
- **ActionsPanel's local scanner DISABLED** (no longer needed, eliminates conflicts)

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
- ✅ Void Item button deletes immediately without confirmation (faster workflow)
- ✅ Discount Engine API deployed to production web server (configurable via Settings)
- ✅ Phase 3 complete: Promotional, Senior/Veteran, and Coupon discounts fully operational
- ✅ **API optimization**: Regular items add instantly (no API calls), promotional items call API only once
- ✅ **48 featured products**: 12 promotional (purple buttons) + 36 regular, randomized across 4 pages
- ✅ **Authentication system**: Username/password protection for Settings dialogs (default: admin/admin)
- ✅ **Session management**: 2-minute timeout with activity refresh, logout buttons in config dialogs
- ✅ **Data cleanup**: Removed duplicate pricebook entries

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
2. Check current branch
3. Confirm build is successful (`./gradlew build`)
4. Acknowledge you understand the step-by-step approach and the "?" protocol
5. **Current status**:
   - Phase 1: ✅ Core POS functionality - COMPLETE (including authentication system)
   - Phase 2: 🔧 Multi-POS journal viewer - NEEDS POLISH (connection issues between POS instances)
   - Phase 3: ✅ Full discount system - DEPLOYED TO PRODUCTION
   - Authentication: ✅ Username/password system - COMPLETE
6. Wait for me to provide the next task or direction

---

## ROADMAP STATUS

1. ✅ **Phase 1**: Core POS functionality (POS interface with icon removal and global scanner)
   - ✅ Authentication system COMPLETE (username/password, 2-minute session timeout)
2. 🔧 **Phase 2**: Multi-POS journal viewer (needs polish - multi-POS compatibility issues)
   - ⚠️ Works on single POS but connection issues between multiple POS instances
   - 🔧 Disconnect buttons need review
   - 🔧 Automatic socket discovery needs implementation
3. ✅ **Phase 3**: Full discount system deployed to production
   - ✅ Phase 3A: Promotional Discounts (inline display, toast notifications)
   - ✅ Phase 3B: Senior/Veteran Discounts (totals display, dialog auto-close)
   - ✅ Phase 3C: Coupon Support (progressive disclosure, barcode scanning, validation)
4. 🎉 **Phases 1 & 3 PRODUCTION READY AND DEPLOYED!**
5. 🔧 **IN PROGRESS**: Phase 2 debugging and polish
6. ✅ **COMPLETE**: Authentication for Settings access (API/Socket Configuration protection)

---

## PHASE 3 STATUS

### ✅ COMPLETED

#### 1. Discount Engine API (Spring Boot)
- ✅ Deployed to production web server
- ✅ API URL configurable via POS Settings → API Configuration dialog
- ✅ 4 REST endpoints: promotional, senior, veteran, coupon validation
- ✅ Successfully tested with production deployment
- ✅ Project details:
  - Group ID: `org.discountengine`
  - Artifact ID: `discount-engine-api`
  - Package: `org.discountengine.api`

#### 2. POS UI Provisions (Java Swing)
- ✅ Promotional products database column (`has_promotion`)
- ✅ Light violet button styling for promotional items
- ✅ Discount button added to Transaction Actions
- ✅ Layout optimized (Current Sale 35%, Actions 65%)
- ✅ 12 promotional products (API-verified) + 36 regular products
- ✅ 48 featured products randomized across 4 pages
- ✅ All entities and DAOs updated
- ✅ Build successful
- ✅ Void Item button improved (no confirmation dialog)
- ✅ API call optimization for instant regular item additions

#### 3. Integration Complete
- ✅ All technology decisions finalized
- ✅ Database schema implemented (`transaction_discounts` table)
- ✅ UI/UX fully implemented (dialog-based discount selection)
- ✅ Error handling strategy implemented
- ✅ Incremental implementation approach completed (3A → 3B → 3C)

### ✅ COMPLETED WORK

#### Phase 3A: Promotional Discounts ✅ COMPLETE
- [x] Created PromotionalDiscountResponse DTO
- [x] Created DiscountApiClient with calculatePromotionalDiscount method
- [x] Created ToastNotification UI component with slide-in animation
- [x] Updated TransactionService with checkAndApplyPromotionalDiscounts()
- [x] Integrated with Quick Keys and barcode scanner
- [x] Toast notification shows on discount trigger with product name
- [x] **Promotional discounts display INLINE below items** (CurrentSalePanel)
- [x] **Promotional discounts display INLINE below items** (Receipt)
- [x] Custom TableRow wrapper for mixed row types
- [x] Custom rendering for discount rows (gray italic, 14pt)
- [x] Data sync resolved between POS and API
- [x] All tests passing

#### Phase 3B: Senior/Veteran Discounts ✅ COMPLETE
- [x] Created separate `transaction_discounts` table (cleaner approach)
- [x] Created TransactionDiscount entity and DAO
- [x] Created SeniorVeteranDiscountResponse DTO
- [x] Created DiscountApiClient with calculateSenior/Veteran methods
- [x] Created DiscountDialog UI with full polish (purple theme, text outlines)
- [x] Integrated with ActionsPanel and TransactionService
- [x] Tested senior/veteran discount application and removal
- [x] Updated CurrentSalePanel to display discounts in totals section
- [x] **Dialog auto-closes after discount application**
- [x] Updated Receipt to display discounts in totals section
- [x] All tests passing

#### Phase 3C: Coupon Support ✅ COMPLETE
- [x] Implement coupon validation via DiscountDialog "Apply Coupon" button
- [x] Create coupon entry dialog with on-screen keyboard
- [x] Integrate with barcode scanner for coupon codes
- [x] Enforce one coupon per transaction rule
- [x] Progressive disclosure pattern (accept early, validate at Total)
- [x] API Integration with `valid` vs `triggered` fields
- [x] Empty cart validation with dummy data approach
- [x] Triggered flag checking (Option 1) to prevent premature discounts
- [x] Receipt shows specific coupon codes (not generic "Coupon Discount")
- [x] Non-blocking payment flow with warning dialogs
- [x] Auto-recalculation as cart crosses minimum threshold
- [x] Single scanner architecture (GlobalBarcodeScanner only)
- [x] Test all 4 coupon codes (SAVE20, ITEM15OFF, MEMBER10, EXPIRED10)

---

### Phase 3C: Final Implementation Status (2026-03-23) ✅ COMPLETE

#### ✅ PRODUCTION READY - Progressive Coupon Validation

**Problem Solved:**
Users can now add coupons at any time during cart building. Validation only happens when clicking Total/Pay buttons.

**Implementation Details:**

1. **TransactionService.java:**
   - Added `pendingCouponCodes` HashMap to track coupon codes (discount_id → coupon_code)
   - Added `validatePendingCoupons()` method - called before payment
   - Added `recalculateCouponDiscounts()` - auto-updates discounts as cart changes
   - Coupon codes stored when `applyCouponDiscount()` called
   - Map cleared on transaction completion/void

2. **DiscountDialog.java:**
   - Modified `validateAndApplyCoupon()` to always accept coupons
   - Shows $0 discount if minimum not met (instead of error dialog)
   - Only shows errors for expired/invalid codes
   - Empty cart validation added

3. **PosInterface.java:**
   - Updated `tryValidateCouponFromScan()` to allow scanned coupons
   - Shows toast "Will validate at checkout" for pending coupons
   - Empty cart validation added (silent for barcode scans)

4. **ActionsPanel.java:**
   - Added `validatePendingCoupons()` call in `handlePayCard()` and `handlePayCash()`
   - Shows warning dialog and blocks payment if validation fails
   - Refreshes UI after validation to show updated discounts
   - Recalculates total AFTER validation (discount amounts may change)

5. **Dialog Styling Updates:**
   - All simple dialogs (error/warning/success/info) now have:
     - Bigger text with dynamic scaling (1.4x header, 1.6x body, 1.5x button)
     - Visible colored OK buttons (opaque, no border)
     - Responsive sizing (500x300 scaled)
     - Better padding and layout

**How It Works:**
1. User applies coupon early (e.g., $15 cart, SAVE20 requires $20)
2. Coupon added with $0 discount amount (triggered: false)
3. As items added, `recalculateCouponDiscounts()` updates amount automatically
4. Discount grows from $0 → full amount as cart reaches minimum
5. When user clicks Total, warning shown if minimum not met (non-blocking)
6. Payment proceeds - untriggered coupons silently removed before processing
7. Receipt shows only triggered discounts with specific coupon codes

#### ✅ COMPLETED - Single Scanner Architecture

**Problem Solved:**
Eliminated redundant ActionsPanel barcode scanner that was causing conflicts.

**Implementation:**
1. **GlobalBarcodeScanner**: Now the ONLY active scanner
   - Handles products (primary path)
   - Handles coupons (fallback path via error handler)
   - Universal keyboard interception

2. **ActionsPanel Scanner**: DISABLED
   - DocumentListener commented out
   - processScan() method deprecated
   - Focus management removed
   - Keeps UI element dormant for potential future use

**Benefits:**
- No redundant database queries (was querying twice per scan)
- No conflicting error dialogs (was showing "Product Not Found" for coupons)
- Cleaner codebase (single source of truth)
- Better performance (single query per scan)
- Better UX (silent fail for invalid codes, no annoying popups)

---

## PHASE 3 INTEGRATION DECISIONS

### Technology Stack
- **HTTP Client**: Java 11+ HttpClient (no new Gradle dependencies)
- **API URL Config**: Settings Dialog (easy cloud deployment)
- **JSON Parsing**: Gson (already available in project)

### Implementation Strategy
- **Approach**: Incremental (Phase 3A → 3B → 3C)
- **Phase 3A**: Promotional discounts (automatic)
- **Phase 3B**: Senior/Veteran discounts (manual)
- **Phase 3C**: Coupon support (manual + barcode)

### UI/UX Decisions
- **Discount Button**: Opens dialog with 3 options (Senior, Veteran, Coupon)
- **Discount Display**: Separate line items in Current Sale table
- **Promo Notifications**: Toast popup (upper-right, 3s auto-dismiss, non-intrusive)
- **Error Handling**: Disable discount button if API down, allow transactions to proceed
- **Senior/Veteran**: Mutually exclusive, auto-remove when switching
- **Coupon Entry**: Manual input + barcode scanning
- **Coupon Limit**: One coupon per transaction

### Database Changes
- **Schema Migration**: Add `item_type VARCHAR(20) DEFAULT 'PRODUCT'` to `transaction_items` table
- **Values**: PRODUCT, PROMOTIONAL_DISCOUNT, SENIOR_DISCOUNT, VETERAN_DISCOUNT, COUPON_DISCOUNT

### Discount Behavior
- **Promotional**: Call API on every add/remove/qty change, show toast on first trigger
- **Senior/Veteran**: Applied to cart subtotal, recalculate on cart changes
- **Coupon**: Validate via API before applying, show error if invalid
- **Void**: Can void individual discount line items like regular items

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

- ❌ **One coupon per transaction**: Only one coupon code can be applied per transaction
- ❌ **No coupon stacking**: Different coupon codes cannot be combined in same transaction
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

**📋 Follow the complete step-by-step guide in `PHASE_3_IMPLEMENTATION_PLAN.md`**

### ✅ 1. POS Database Changes (H2) - COMPLETE
- ✅ Added `has_promotion` boolean column to `price_book` table
- ✅ Marked 12 promotional items in price book (working API promotions only)
- ✅ Created 48 featured products (12 promotional + 36 regular) for Quick Keys
- ✅ Randomized positions across 4 pages of Quick Keys grid
- ✅ Updated all entities, DAOs, and seed data
- ✅ Database reset scripts created (reset_database.sh, update_promotions.sql)

### ✅ 2. POS UI Updates - COMPLETE
- ✅ Light violet button color for promotional items in Quick Keys
- ✅ 48 featured products across 4 pages (12 promotional + 36 regular)
- ✅ Discount button added to Transaction Actions zone (fully functional)
- ✅ Current Sale width reduced from 42% to 35%
- ✅ Transaction Actions now has 5 buttons with better readability
- ✅ Custom `RoundedBorder` class for promotional buttons
- ✅ Void Item button improved (no confirmation dialog)
- ✅ Dialogs follow POS window location across multiple monitors

### ✅ 3. Spring Boot Discount Service - COMPLETE
- ✅ Spring Boot 3.x project created at `/Users/ed/IdeaProjects/discount-engine-api`
- ✅ Group ID: `org.discountengine`, Artifact ID: `discount-engine-api`
- ✅ Spring Data JPA configured (H2 database)
- ✅ Swagger/OpenAPI documentation available
- ✅ Database schema for promotions and coupons defined

### ✅ 4. API Implementation - COMPLETE
- ✅ Implemented `/api/discounts/calculate-promotion` endpoint (GET)
- ✅ Implemented `/api/discounts/calculate-senior` endpoint (GET)
- ✅ Implemented `/api/discounts/calculate-veteran` endpoint (GET)
- ✅ Implemented `/api/discounts/validate-coupon` endpoint (POST)
- ✅ Seeded database with 4 coupon codes (SAVE20, ITEM15OFF, EXPIRED10, MEMBER10)
- ✅ All endpoints tested and working via Swagger UI

### ✅ 5. Integration Planning - COMPLETE
- ✅ Implementation plan document created (`PHASE_3_IMPLEMENTATION_PLAN.md`)
- ✅ All technology decisions finalized
- ✅ Database migration script prepared
- ✅ UI/UX approach defined
- ✅ Error handling strategy defined

### 🔨 6. POS HTTP Client Integration - READY TO START

**Start with Phase 3A: Promotional Discounts**

#### Step 1: Database Migration
- Run ALTER TABLE to add `item_type` column to `transaction_items`
- Verify migration successful

#### Step 2: Create Foundation Classes
- Create `dto/` package with response models
- Create `DiscountApiClient.java` with HTTP client
- Create `ToastNotification.java` UI component
- Create `DiscountConfig.java` for JSON persistence

#### Step 3: Update TransactionService
- Add methods for discount item management
- Integrate with DiscountApiClient

#### Step 4: Integrate with UI
- Wire promotional discount API calls to item add/remove/qty change
- Display discount line items in Current Sale table
- Show toast notifications when discounts trigger

**See `PHASE_3_IMPLEMENTATION_PLAN.md` for complete step-by-step checklist**

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

### Authentication System
- **Default Credentials**: username: `admin`, password: `admin`
- **Session Timeout**: 2 minutes with activity refresh
- **Protected Dialogs**: API Configuration, Socket Configuration
- **Features**: SHA-256 password hashing, logout buttons, on-screen keyboard
- **Access**: Settings → API/Socket Configuration (requires login)

### Current Sale Table
- **4 columns**: Name (408px), Qty (70px), Price (100px), Line Total (110px)
- No checkboxes, no icons
- Row selection for operations
- Void Item button for deletion
- Change Qty button for quantity changes

---

## 🎉 ALL PHASE 3 COMPLETE - PRODUCTION READY!

**Current Status:**
- ✅ Discount Engine API running and tested
- ✅ POS UI provisions complete
- ✅ Integration plan finalized
- ✅ Database schema created (transaction_discounts table)
- ✅ DiscountApiClient implemented with all methods
- ✅ DiscountDialog UI fully polished (purple theme, text outlines, auto-close, increased height)
- ✅ TransactionService integrated with discount methods
- ✅ **Phase 3A (Promotional Discounts) COMPLETE**
  - Promotional discounts display INLINE below items
  - Toast notifications with product name
  - Data sync resolved between POS and API
- ✅ **Phase 3B (Senior/Veteran Discounts) COMPLETE**
  - Senior/Veteran discounts display in totals section
  - Dialog auto-closes after application
  - Receipt shows all discounts correctly
- ✅ **Phase 3C (Coupon Support) COMPLETE**
  - Progressive disclosure pattern implemented
  - Empty cart validation with dummy data
  - Triggered flag checking prevents premature discounts
  - Receipt shows specific coupon codes (e.g., "Coupon (SAVE20)")
  - Non-blocking payment flow with warning dialogs
  - Single scanner architecture (GlobalBarcodeScanner only)
  - All 4 coupon types tested and working

**Display Implementation:**
- **Promotional discounts**: Inline below items (CurrentSalePanel & Receipt)
  - Format: "  Buy 2+ Save 25%"
  - Gray italic 14pt
- **Senior/Veteran discounts**: In totals section (CurrentSalePanel & Receipt)
  - Format: "Senior Discount (5%): -$X.XX"
  - Gray italic 18pt
- **Coupon discounts**: In totals section with specific codes
  - Format: "Coupon (SAVE20): -$2.50"
  - Gray italic 18pt

**Scanner Architecture:**
- **GlobalBarcodeScanner**: Single handler for ALL scanning (products + coupons)
- **ActionsPanel scanner**: DISABLED (no longer needed, eliminates conflicts)
- **Benefits**: No duplicate queries, no conflicting error dialogs, cleaner codebase

**Discount Engine API:**
- ✅ Deployed to production web server
- ✅ Configurable via POS Settings → API Configuration dialog
- ✅ All 4 endpoints tested and working in production
- ✅ Status: Production ready and operational

---

**🎉 Phase 3 COMPLETE - All Features Production Ready! 🚀**

**Latest Release (2026-03-23) - v3.4:**
- ✅ Coupon support with progressive disclosure
- ✅ Empty cart validation (accepts coupons early, validates with dummy data)
- ✅ Triggered flag implementation (prevents $0 cart totals)
- ✅ Receipt enhancement (specific coupon codes displayed)
- ✅ Single scanner architecture (GlobalBarcodeScanner exclusive)
- ✅ Dialog height increased for multiple discounts
- ✅ All coupon types tested (SAVE20, ITEM15OFF, MEMBER10, EXPIRED10)
- ✅ Non-blocking payment flow with informational warnings
- ✅ Auto-recalculation as cart crosses minimum thresholds

**System Features:**
- ✅ Promotional discounts (automatic, inline display)
- ✅ Senior/Veteran discounts (manual, totals display)
- ✅ Coupon support (manual + barcode, progressive validation)
- ✅ Discount stacking (all types can combine)
- ✅ Toast notifications (non-intrusive success feedback)
- ✅ Global barcode scanning (products + coupons)
- ✅ Multi-POS journal viewer (Phase 2)
- ✅ Transaction management (Phase 1)

**System Status:**
- ✅ Authentication system complete and deployed
- System is fully functional for production use!

---

## 🚀 FUTURE FEATURE IDEAS

### ⭐ Priority Features (User Selected)

#### 1. Return/Refund System ↩️ [HIGH PRIORITY]
- **Purpose**: Essential for real retail operations
- **Features**:
  - Receipt lookup by transaction ID
  - Partial or full refunds
  - Reason codes (damaged, wrong item, customer changed mind, etc.)
  - Refund journal separate from sales journal
  - Optional inventory restock on refund
  - Manager approval for large refunds (optional with authentication)
- **Database Changes**: New `refund_transactions` table, link to original transaction
- **Complexity**: Medium - new transaction type with reverse logic
- **Business Impact**: High - required for professional retail operation

#### 2. Receipt Options 📧 [HIGH PRIORITY]
- **Purpose**: Modern convenience and eco-friendly
- **Features**:
  - Email receipt (customer enters email address)
  - SMS receipt (customer enters phone number)
  - QR code on printed receipt for digital copy
  - Save receipt as PDF to local storage
  - Receipt history lookup by email/phone
- **Integration**: Email service (SMTP), SMS service (Twilio/similar)
- **Complexity**: Medium - external service integration
- **Business Impact**: Medium - customer satisfaction and modernization

#### 3. Product Search Enhancement 🔍 [HIGH PRIORITY]
- **Purpose**: Faster item lookup and better UX
- **Features**:
  - Search by category/brand/price range
  - Voice search (speak product name)
  - Recent items list (last 20 scanned)
  - Favorites/frequently sold items
  - Fuzzy matching (typo tolerance - "coke" finds "Coca Cola")
  - Search history
- **Database Changes**: Add `category`, `brand` columns to price_book
- **Complexity**: Low to Medium
- **Business Impact**: High - speeds up checkout significantly

#### 4. Split Payment 💳💵 [HIGH PRIORITY]
- **Purpose**: Common customer request
- **Features**:
  - Pay with multiple methods (part cash, part card)
  - Split bill between multiple cards
  - Visual breakdown of payments applied
  - Remaining balance display
  - Support up to 5 payment methods per transaction
- **Database Changes**: `transaction_payments` table (multiple payment records per transaction)
- **Complexity**: Medium - modify payment flow logic
- **Business Impact**: Medium - customer convenience

---

### 💡 Other Recommended Features

#### 5. Sales Analytics Dashboard 📊
- **Purpose**: Real-time business insights
- **Features**:
  - Today's sales summary (total, transaction count, average sale)
  - Best-selling products (top 10 items by quantity/revenue)
  - Hourly sales chart (see peak hours)
  - Discount usage statistics (how much given away)
  - Revenue by payment method (cash vs card breakdown)
  - Week/month/year comparisons
- **Complexity**: Medium - data aggregation and charting
- **Business Impact**: High - helps with inventory and staffing decisions

#### 6. Customer-Facing Display 👀
- **Purpose**: Modern customer experience
- **Features**:
  - Second window showing items being scanned
  - Running total visible to customer
  - Product images (if available)
  - "Thank you" message after payment
  - Promotional messages during idle
- **Complexity**: Low - just another Swing window
- **Business Impact**: Medium - transparency reduces disputes
- **Note**: Works great with existing multi-POS socket architecture!

#### 7. Low Stock Alerts 📦
- **Purpose**: Prevent stockouts
- **Features**:
  - Track inventory levels during sales
  - Alert when product hits minimum threshold
  - "Almost Out" badge on Quick Keys buttons
  - Daily low stock report
  - Auto-generate reorder list
- **Database Changes**: Add `quantity_on_hand`, `min_quantity` to price_book
- **Complexity**: Low - increment/decrement on sales
- **Business Impact**: High - better inventory management

#### 8. End-of-Day Report 📝
- **Purpose**: Required for cash reconciliation
- **Features**:
  - Total sales by payment method
  - Expected cash drawer amount
  - Transaction count (including voids)
  - Voided items summary
  - Discount breakdown (promo/senior/veteran/coupon totals)
  - Tax collected
  - Print or export to PDF
- **Complexity**: Low - parse transaction journal
- **Business Impact**: High - accounting accuracy and compliance

#### 9. Transaction History Viewer 📜
- **Purpose**: Customer service and dispute resolution
- **Features**:
  - Search past transactions by date, amount, items
  - Search by transaction ID or receipt number
  - View detailed receipt
  - Reprint receipt
  - Link to refund processing
  - Export to CSV
- **Complexity**: Low - read existing transaction journal
- **Business Impact**: Medium - resolve customer issues quickly

#### 10. Offline Mode 🔌
- **Purpose**: Network reliability and business continuity
- **Features**:
  - Queue transactions when discount API is down
  - Continue operating without discounts (with warning)
  - Sync queued discounts when connection restored
  - Visual indicator of offline status (red banner)
  - Fallback to cached discount rules
- **Complexity**: Medium - transaction queue system
- **Business Impact**: High - prevents lost sales during outages

#### 11. Product Images 🖼️
- **Purpose**: Visual confirmation
- **Features**:
  - Show product photo in Current Sale panel
  - Customer display shows images
  - Visual confirmation of scanned item
  - Thumbnail grid in Quick Keys
- **Database Changes**: Add `image_path` column to price_book
- **Complexity**: Low - image display in Swing
- **Business Impact**: Low - nice-to-have polish

#### 12. Hot Keys/Keyboard Shortcuts ⌨️
- **Purpose**: Speed up power users
- **Features**:
  - F1-F12 for common actions
  - Ctrl+Q for Change Qty
  - Ctrl+V for Void Item
  - Ctrl+D for Discount
  - Ctrl+T for Total
  - Customizable key bindings
- **Complexity**: Low - add KeyListener mappings
- **Business Impact**: Medium - faster for experienced cashiers

#### 13. Mini Receipt Printer Support 🖨️
- **Purpose**: Professional retail printing
- **Features**:
  - Integrate with thermal printers (Epson, Star, etc.)
  - Auto-print on payment
  - Print logo/header
  - Kitchen order printing (if food/beverage)
  - Configurable printer selection
- **Complexity**: Medium - hardware integration
- **Business Impact**: High - professional appearance

#### 14. Sound Effects 🔊
- **Purpose**: Audio feedback
- **Features**:
  - Scan beep (success/error sounds)
  - Payment confirmation chime
  - Low stock alert sound
  - Error buzz
  - Customizable sound pack
- **Complexity**: Low - Java audio playback
- **Business Impact**: Low - UX polish

#### 15. Idle Screen Saver/Attract Mode 🎬
- **Purpose**: Marketing and security
- **Features**:
  - Show promotional offers when idle
  - Product highlights carousel
  - Daily specials
  - Auto-lock after X minutes (with authentication)
- **Complexity**: Low - timer + carousel display
- **Business Impact**: Low - marketing opportunity
- **Note**: Git history shows "working carousel with trigger upon idle" already exists!

---

### 🏆 Recommended Implementation Order

**Phase 4A - Core Operations** (Essential):
1. Return/Refund System (⭐ selected)
2. End-of-Day Report
3. Transaction History Viewer

**Phase 4B - Customer Experience** (High Value):
4. Split Payment (⭐ selected)
5. Receipt Options (⭐ selected)
6. Product Search Enhancement (⭐ selected)

**Phase 4C - Business Intelligence** (Analytics):
7. Sales Analytics Dashboard
8. Low Stock Alerts

**Phase 4D - Polish & Advanced** (Nice-to-Have):
9. Customer-Facing Display
10. Offline Mode
11. Thermal Printer Support
12. Hot Keys/Keyboard Shortcuts

---

### 📝 Implementation Notes

- **Quick Wins** (Low effort, high value):
  - Transaction History Viewer (parse existing journals)
  - Low Stock Alerts (one column to database)
  - End-of-Day Report (analyze transaction log)
  - Product Search by Category (filter existing data)

- **Database Schema Planning**:
  - `refund_transactions` table for returns
  - `transaction_payments` table for split payments
  - Add `category`, `brand`, `image_path`, `quantity_on_hand`, `min_quantity` to `price_book`
  - `customer_receipts` table for email/SMS receipt tracking

- **External Integrations Needed**:
  - Email service (SMTP or SendGrid/Mailgun)
  - SMS service (Twilio or similar)
  - Thermal printer drivers (JavaPOS or manufacturer SDKs)

---

**User's Selected Priorities for Phase 4:**
- ⭐ Return/Refund System
- ⭐ Receipt Options
- ⭐ Product Search Enhancement
- ⭐ Split Payment
