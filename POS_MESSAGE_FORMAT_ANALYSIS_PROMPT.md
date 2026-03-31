# POS Message Format Analysis Request

**From:** POS System Integration Team
**To:** Your POS System Team
**Purpose:** Analyze your message format for socket communication compatibility

---

## 🚀 HOW TO USE THIS FILE

**Step 1:** Save this file to your POS project directory

**Step 2:** Open Claude Code in your project

**Step 3:** Run this command in Claude Code:
```
Read POS_MESSAGE_FORMAT_ANALYSIS_PROMPT.md and follow all instructions.
Generate a complete MESSAGE_FORMAT_RESPONSE.md file with all requested information.
```

**Step 4:** Send the generated `MESSAGE_FORMAT_RESPONSE.md` file back to us

**That's it!** Your Claude Code will do all the analysis and create a structured response file.

---

## 📋 INSTRUCTIONS FOR CLAUDE CODE

Dear Claude Code analyzing this file:

Please perform a comprehensive analysis of this POS system's message format and socket communication protocol. Generate a file named `MESSAGE_FORMAT_RESPONSE.md` with all findings.

### Analysis Tasks:

I need to understand the exact message format this POS system sends to connected socket clients. Please analyze and provide detailed information WITHOUT making any changes:

---

### TASK 1: IDENTIFY MESSAGE BROADCASTING

Search for code that:
- Sends messages to connected clients/sockets
- Broadcasts transaction events
- Writes to output streams or socket connections

Look for:
- `PrintWriter.println()` or `out.println()`
- `socket.getOutputStream().write()`
- `send()`, `broadcast()`, or similar method names
- Keywords: "broadcast", "send", "write", "output"

Show me the exact code that formats and sends messages to clients.

---

### TASK 2: FIND MESSAGE FORMAT/STRUCTURE

Search for:
- JSON serialization code (Gson, Jackson, org.json)
- Message creation or formatting methods
- Classes/records that define message structure
- Any DTO, Message, Event, or Protocol classes

Show me:
- The complete message structure (field names and types)
- How messages are serialized (JSON, plain text, XML?)
- Example of what a message looks like on the wire

---

### TASK 3: TRANSACTION EVENT EXAMPLES

For these specific events, show me EXACTLY what gets sent:

**A. Item Added to Cart:**
- Find the code that broadcasts when a product is added
- Show the exact JSON/message format
- Include all field names and example values

**B. Transaction Started:**
- Find the code for new transaction events
- Show the message format

**C. Payment Processed:**
- Find the payment completion broadcast
- Show the message format

---

### TASK 4: READ KEY FILES

Please read and show these files (if they exist):

1. Any file with "Message", "Event", "Protocol" in the name
2. Any file containing socket write/send operations
3. Your transaction service/logger that broadcasts events
4. Configuration files showing message format

---

### TASK 5: SHOW ACTUAL LOG EXAMPLES

If available, show me:
- Sample messages from your log files
- Contents of `logs/journal.log` (last 10 lines)
- Any console output showing sent messages
- Network capture or debug logs

---

### OUTPUT FILE GENERATION

Create a file named `MESSAGE_FORMAT_RESPONSE.md` with the following structure:

### MESSAGE STRUCTURE
```
Field names and types in your messages:
- Field 1: [name] ([type])
- Field 2: [name] ([type])
- etc.
```

### EXAMPLE MESSAGES (ACTUAL JSON)

**Item Added:**
```json
{paste actual JSON here}
```

**Transaction Started:**
```json
{paste actual JSON here}
```

**Payment Processed:**
```json
{paste actual JSON here}
```

### CODE THAT CREATES/SENDS MESSAGES

```java
// Paste the actual code that formats and sends messages
```

### ADDITIONAL INFO

- Message format: JSON / Plain Text / Other?
- Line termination: \n / \r\n / none?
- Encoding: UTF-8 / ASCII / Other?
- Any special prefixes or wrappers?

---

**IMPORTANT INSTRUCTIONS:**
- Show ACTUAL messages with REAL field names (not placeholders)
- Include example values in JSON examples
- Show complete code snippets that create/send messages
- Do NOT make any code changes to the project
- This is for integration compatibility analysis only

---

## 📄 MESSAGE_FORMAT_RESPONSE.md FILE TEMPLATE

Generate a file with this exact structure:

```markdown
# POS Message Format Response

**Generated:** [Date]
**POS System Name:** [Your system name/identifier]
**Analyzed By:** Claude Code

---

## 1. SYSTEM IDENTIFICATION

- **POS System Name:** [e.g., "Journal Server", "RetailPOS", etc.]
- **Version:** [if available]
- **Broadcast IP:Port:** [e.g., 0.0.0.0:9999 or specific IP]
- **Network Protocol:** [TCP/UDP]
- **Message Format:** [JSON/XML/Plain Text/Other]

---

## 2. MESSAGE STRUCTURE

### Top-Level Fields

List all top-level fields in your message structure:

| Field Name | Data Type | Required | Description | Example Value |
|------------|-----------|----------|-------------|---------------|
| [field1] | [String/Number/Object] | [Yes/No] | [Brief description] | [Sample value] |
| [field2] | [String/Number/Object] | [Yes/No] | [Brief description] | [Sample value] |

### Nested Objects/Payload Structure

If your messages have nested objects (e.g., "payload", "data", "details"), describe them:

**[Object Name] Fields:**

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| [field1] | [Type] | [Description] | [Example] |
| [field2] | [Type] | [Description] | [Example] |

---

## 3. ACTUAL MESSAGE EXAMPLES

Provide REAL examples from your codebase (copy exact JSON):

### Example 1: Item Added to Cart

```json
[Paste actual JSON message here]
```

**File:** [Source file location]
**Method:** [Method that creates this message]

---

### Example 2: Transaction Started

```json
[Paste actual JSON message here]
```

**File:** [Source file location]
**Method:** [Method that creates this message]

---

### Example 3: Payment Processed

```json
[Paste actual JSON message here]
```

**File:** [Source file location]
**Method:** [Method that creates this message]

---

### Example 4: Transaction Voided

```json
[Paste actual JSON message here]
```

**File:** [Source file location]
**Method:** [Method that creates this message]

---

### Example 5: [Any Other Important Event]

```json
[Paste actual JSON message here]
```

---

## 4. CODE SNIPPETS

### Message Creation Code

**File:** [Full file path]
**Lines:** [Line numbers]

```java
[Paste the complete code that creates/serializes messages]
```

---

### Message Broadcasting Code

**File:** [Full file path]
**Lines:** [Line numbers]

```java
[Paste the complete code that sends messages to clients]
```

---

### Message Class/Record Definition

**File:** [Full file path]

```java
[Paste the complete class/record that defines message structure]
```

---

## 5. PROTOCOL DETAILS

### Serialization

- **Library Used:** [Gson/Jackson/Manual/Other]
- **Configuration:** [Any special serialization settings]
- **Date/Time Format:** [ISO-8601/Epoch/Custom]

### Network Transmission

- **Line Termination:** [\n / \r\n / none]
- **Character Encoding:** [UTF-8 / ASCII / Other]
- **Message Delimiter:** [newline / null byte / length-prefixed / other]
- **Maximum Message Size:** [if any limit]

### Special Formatting

- **Prefixes:** [Any prefix before JSON, e.g., "MSG:"]
- **Wrappers:** [Any wrapper around messages]
- **Escaping:** [Special character handling]
- **Compression:** [gzip/none/other]

---

## 6. EVENT TYPES

List all possible event types your system broadcasts:

| Event Type String | Description | Frequency |
|-------------------|-------------|-----------|
| [EVENT_NAME_1] | [What triggers this] | [Common/Rare] |
| [EVENT_NAME_2] | [What triggers this] | [Common/Rare] |
| [EVENT_NAME_3] | [What triggers this] | [Common/Rare] |

---

## 7. SAMPLE LOG OUTPUT

If available, provide actual log file samples:

### From: logs/journal.log

```
[Paste 10-15 lines of actual log output showing real messages]
```

### Console Output

```
[If your system prints to console, paste sample output]
```

---

## 8. TIMESTAMP FORMAT DETAILS

**Critical for parsing!**

- **Field Name:** [e.g., "timestamp", "eventTimestamp", "time"]
- **Data Type:** [Long/Double/String]
- **Format:** [Describe exact format]
- **Examples:**
  - `1774877029.297165` = [Interpretation]
  - `2026-03-30T21:23:49.297` = [Interpretation]
  - [Show 2-3 real examples with explanations]

---

## 9. COMPATIBILITY NOTES

### Known Issues

- [Any known parsing issues]
- [Any special cases to handle]
- [Any version-specific differences]

### Dependencies

- [External libraries required to parse your messages]
- [Any specific Java version requirements]

---

## 10. TESTING INFORMATION

### How to Test Connection

```bash
# Commands to test connectivity to your system
[Provide actual commands with your real IP/port]
```

### Sample Client Code

```java
// Minimal code to connect and receive messages from your system
[Provide actual working example]
```

---

## APPENDIX: File Locations

List all relevant files analyzed:

- [File 1 path and purpose]
- [File 2 path and purpose]
- [File 3 path and purpose]

---

**End of Report**
**Ready for Integration Analysis**
```

---

## ✅ FINAL STEPS

After generating `MESSAGE_FORMAT_RESPONSE.md`:

1. Review the file for completeness
2. Verify all JSON examples are ACTUAL messages (not templates)
3. Check that all code snippets compile
4. Confirm IP addresses and ports are correct
5. Save the file
6. Send `MESSAGE_FORMAT_RESPONSE.md` to the integration team

---

**DO NOT MAKE ANY CODE CHANGES - ANALYSIS ONLY**
