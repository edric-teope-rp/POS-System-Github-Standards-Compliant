# POS System

A professional Java-based Point of Sale system with multi-terminal journal synchronization capabilities.

## Features

### Phase 1: Standalone POS System (Complete)
- **Modern POS Interface**
  - Quick keys grid with product search and filtering
  - Real-time shopping cart with quantity management
  - Transaction actions (void items, void transaction)
  - Payment processing (cash/card)
  - Custom undecorated dialogs with drag-to-move functionality
  - On-screen numeric keyboards
  - Settings dialog with gear icon access

- **Receipt System**
  - Custom-styled receipt display
  - New transaction workflow

- **Transaction Logging**
  - Automated journal logging with SLF4J + Logback
  - Format: `YYYY-MM-DD HH:mm:ss.SSS|ACTION|DETAILS`
  - 10-year retention with monthly rotation
  - Logs stored in `logs/transactions/journal.log`

### Phase 2: Multi-POS Journal Viewer (Complete)
- **Real-Time Synchronization**
  - TCP socket server/client architecture (bidirectional)
  - UDP broadcast auto-discovery (port 9999)
  - Live journal synchronization across multiple POS terminals
  - Automatic dead connection cleanup

- **Hybrid Protocol Support**
  - Native pipe-delimited format: `TIMESTAMP|ACTION|DETAILS`
  - JSON format for external POS integration
  - Auto-detection of incoming data format

- **Socket Configuration UI**
  - Configure POS name and server port
  - Start/stop server with one click
  - Active clients monitoring (IP, connection time, duration)
  - Available POS systems table (discovered + manual connections)
  - Live journal viewer with merged chronological display
  - Manual connection support (IP:Port)
  - Connection persistence (saved to `config/socket-config.json`)

- **Advanced Features**
  - Console color-coding (GREEN for local, BLUE/YELLOW for remote)
  - Remote journal storage: `logs/remote-journals/[POS-NAME]/journal.log`
  - Thread-safe operations with ExecutorService
  - Listener interfaces for UI updates

### Phase 3: Discount Service (Future)
- Spring Boot microservice for centralized discount management (planned)

## Tech Stack

- **Java**: 25
- **Build Tool**: Gradle
- **Database**: H2 (embedded)
- **UI Framework**: Swing
- **Logging**: SLF4J + Logback
- **Serialization**: Gson (JSON)

## Project Structure

```
src/main/java/org/possystem/
├── Main.java                          # Application entry point
├── dao/                              # Data Access Objects
│   ├── PriceBookDao.java
│   ├── TransactionHeaderDao.java
│   └── TransactionItemDao.java
├── database/                         # Database management
│   ├── DatabaseManager.java
│   └── DataSeeder.java
├── entity/                          # Data models
│   ├── PriceBook.java
│   ├── TransactionHeader.java
│   └── TransactionItem.java
├── event/                           # Event system
│   ├── PosEvent.java
│   ├── PosEventListener.java
│   └── PosEventDispatcher.java
├── service/                         # Business logic
│   ├── PriceBookService.java
│   └── TransactionService.java
├── socket/                          # Phase 2: Multi-POS communication
│   ├── SocketService.java           # Server + Client + Discovery
│   ├── PosSystemInfo.java           # Discovered POS model
│   ├── RemoteJournalEntry.java      # Journal entry model
│   └── SocketConfig.java            # Configuration persistence
└── ui/                              # User Interface
    ├── PosInterface.java            # Main frame coordinator
    ├── ActionsPanel.java            # Transaction actions + payment
    ├── CurrentSalePanel.java        # Shopping cart (42% width)
    ├── QuickKeysPanel.java          # Product grid + search
    └── SocketConfigDialog.java      # Socket configuration UI
```

## Getting Started

### Prerequisites
- Java 25 or higher
- Gradle (wrapper included)

### Running the Application

```bash
./gradlew run
```

### Testing Multi-POS Synchronization

**Terminal 1: First POS**
```bash
./gradlew run
```
- Open Settings → Socket Configuration
- Set POS Name: "Counter-1"
- Set Server Port: 9000
- Click "Start Server"

**Terminal 2: Second POS**
```bash
./gradlew run
```
- Open Settings → Socket Configuration
- Set POS Name: "Counter-2"
- Set Server Port: 9001 (must be different)
- Click "Start Server"

**Connect the terminals:**
- Wait 5-10 seconds for auto-discovery, or click "Refresh"
- Click "Connect" next to the discovered POS
- Create transactions on one terminal and watch them appear in the other

## Configuration

### Socket Ports
- **UDP Discovery**: 9999 (auto-discovery broadcasts)
- **TCP Server**: Configurable per POS (default: 9000)

### Logs
- **Local transactions**: `logs/transactions/journal.log` (compliance - never delete)
- **Remote journals**: `logs/remote-journals/[POS-NAME]/journal.log`

### Configuration Files
- **Socket config**: `config/socket-config.json` (connection persistence)
- **Logging config**: `src/main/resources/logback.xml`

## Architecture

### Transaction Service
- Business logic for all financial operations
- Automatic transaction journal logging
- Integrated with SocketService for broadcasting
- Tax rate: 7% (TAX_RATE = 0.07)

### Socket Service
- Manages server (broadcasting), client (receiving), and discovery
- Thread-safe with ExecutorService and ScheduledExecutorService
- Hybrid protocol parser (JSON + pipe-delimited auto-detection)
- Automatic dead connection cleanup with error detection
- Active client tracking with connection metadata

### UI Architecture
1. **Quick Keys Panel**: Product grid with search and keyboard
2. **Current Sale Panel**: 42% width, custom table with checkboxes
3. **Actions Panel**: Transaction actions (60%) + Payment (40%)
4. **Socket Configuration Dialog**: Multi-POS management interface

## Console Output

The application uses color-coded console output for multi-POS viewing:
- **GREEN**: Local journal entries
- **BLUE/YELLOW/MAGENTA/CYAN/RED**: Remote POS entries (rotated)

Format: `YYYY-MM-DD HH:mm:ss.SSS|POS-NAME (IP)|ACTION|DETAILS`

## Documentation

- `PHASE2_IMPLEMENTATION.md` - Technical details of multi-POS synchronization
- `PHASE2_QUICK_START.md` - Step-by-step testing guide
- `SOCKET_DIAGNOSTIC.md` - Troubleshooting guide
- `HANDOFF_PROMPT.md` - Detailed project context
- `copy_paste_prompt.txt` - Development session context

## Development Notes

- All dialogs are undecorated and moveable by header drag
- Socket connections persist but don't auto-connect on restart
- Transaction journal files must never be deleted (compliance requirement)
- Debug output enabled for Phase 2 socket operations

## Current Branch

`v1.7-keyboard-polish` (active development)

## Build & Test

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew run
```

## Status

- Phase 1: Complete and Production Ready
- Phase 2: Complete and Fully Tested
- Phase 3: Not started (future work)

## License

Proprietary
