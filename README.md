# CCV Payment App

[![Türkçe](https://img.shields.io/badge/Dil-Türkçe-red)](README.TR.md)

> **⚠️ Important Notice**
>
> This project only works if you have a **CCV Debug POS device**. The application requires a physical CCV terminal with debug mode enabled for testing and development. To obtain a Debug POS device, please contact CCV directly.
>
> **Contact:** [CCV Developer Portal](https://developer.myccv.eu/) | [CCV Website](https://www.ccv.eu/)

---

Android payment terminal integration application. Manages POS terminal operations using CCV mAPI SDK.

## Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 2.3.0
- Gradle 8.14.4
- CCV mAPI SDK 1.33

## Installation

1. Add your CCV SDK AAR files to `libs` folder:
   - `api-hardware-1.33.aar`
   - `pi-api-1.33.aar`

2. Build the project:
```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/example/ccvpayment/
├── CCVPaymentApp.kt              # Application class (MAPI.initialize)
├── helper/
│   ├── PaymentHelper.kt          # Payment operations
│   ├── TerminalHelper.kt         # Terminal management
│   ├── TransactionHelper.kt      # Transaction history and reports
│   └── CCVPaymentManager.kt      # General management class
├── model/
│   └── PaymentModels.kt          # Data models
└── ui/
    ├── MainActivity.kt           # Main screen
    ├── PaymentActivity.kt        # Payment screen
    ├── TransactionHistoryActivity.kt
    ├── SettingsActivity.kt
    └── TerminalSettingsActivity.kt
```

---

## Current Features

### Payment Operations

| Feature | Method | Description |
|---------|--------|-------------|
| Sale | `PaymentHelper.makePayment()` | Standard card payment |
| Refund | `PaymentHelper.refund()` | Payment refund |
| Void | `PaymentHelper.reversal()` | Transaction cancellation |
| Pre-Authorization | `PaymentHelper.authorise()` | Reservation/hold |
| Capture | `PaymentHelper.capture()` | Convert reservation to sale |
| Stop Payment | `PaymentHelper.stopPayment()` | Cancel ongoing payment |

### Terminal Operations

| Feature | Method | Description |
|---------|--------|-------------|
| Terminal Status | `TerminalHelper.getStatus()` | Connection and status check |
| Terminal Startup | `TerminalHelper.startup()` | Start terminal |
| Terminal Activation | `TerminalHelper.activateTerminal()` | Activate terminal |
| Last Message | `TerminalHelper.repeatLastMessage()` | Repeat last message |
| Last Receipt | `TerminalHelper.retrieveLastTicket()` | Get last receipt |
| Payment Recovery | `TerminalHelper.recoverPayment()` | Recover interrupted payment |

### Report Operations

| Feature | Method | Description |
|---------|--------|-------------|
| Period Closing | `TerminalHelper.periodClosing()` | Z-Report / End of day |
| Transaction Overview | `TerminalHelper.transactionOverview()` | X-Report / Current summary |

---

## Features That Can Be Added

Features supported by CCV mAPI SDK but not yet integrated:

### 1. Card Operations

#### Card Read - Pre-Payment Card Reading
Used to read card information before making a payment.

```kotlin
// SDK Usage
val opiDEService = OpiDEService()
val cardReadRequest = CardReadRequest.builder()
    .ageVerification(AgeVerification.builder()
        .value("16/18")
        .extendedVerification(true)
        .build())
    .build()
opiDEService.cardRead(terminal, cardReadDelegate, cardReadRequest)
```

**Use Cases:**
- Age verification (alcohol, tobacco sales)
- Customer recognition (loyalty programs)
- Card type verification

#### Card Read SecureID - Secure Card Reading
Get secure card identity with hash algorithm.

```kotlin
val cardReadRequest = CardReadRequest.builder()
    .hashAlgorithm(HashAlgorithm.DEFAULT)
    .customerSaltIndex("0-255")
    .build()
opiDEService.cardRead(terminal, delegate, cardReadRequest)
```

**Use Cases:**
- Recurring payment token creation
- Customer identity verification

#### Card Detection - Card Type Detection
Detects card brand and type.

```kotlin
val cardDetectionRequest = CardDetectionRequest.builder()
    .build()
paymentService.cardDetection(terminal, cardDetectionRequest, delegate)
```

**Use Cases:**
- Card brand based discounts
- Accept/reject specific cards

#### Card Circuits - Supported Cards
Lists card brands supported by the terminal.

```kotlin
val terminalService = TerminalService()
terminalService.cardCircuits(terminal, delegate)
```

#### Read Mifare UID - NFC Card ID Reading
Reads unique ID of Mifare NFC cards.

```kotlin
val terminalService = TerminalService()
terminalService.readMifareUID(terminal, tokenDelegate)
```

**Use Cases:**
- Employee card verification
- Customer loyalty card
- Access control

---

### 2. Token Operations (Tokenization)

#### Get Token
Used to tokenize and store card information.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(Money(BigDecimal("0.00"), Currency.getInstance("EUR")))
    .hashAlgorithm(HashAlgorithm.DEFAULT)
    .build()
paymentService.payment(terminal, payment, delegate)
```

**Use Cases:**
- Subscription payments
- One-click payment
- Recurring payments

#### Refund with Token
Refund operation using previously obtained token.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.REFUND)
    .amount(money)
    .hashAlgorithm(HashAlgorithm.DEFAULT)
    .hashData(tokenHashData)
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

#### Sale with Token (Card Not Present)
Payment with token without physical card.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(money)
    .token(savedToken)
    .build()
paymentService.payment(terminal, payment, delegate)
```

---

### 3. Gift Card Operations

#### Gift Card Balance Query
Check gift card balance.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.GIFT_CARD_BALANCE)
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

#### Gift Card Activation / Reload
Activate gift card or load balance.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.ACTIVATE_RECHARGE_GIFT_CARD)
    .amount(Money(BigDecimal("100.00"), Currency.getInstance("EUR")))
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

---

### 4. Advanced Payment Features

#### Reservation Adjustment - Pre-Authorization Update
Changes the amount of existing pre-authorization.

```kotlin
fun startReservationAdjustment(
    terminal: ExternalTerminal,
    type: Payment.Type,
    money: Money,
    approvalCode: String
) {
    val payment = Payment.builder()
        .type(type)
        .amount(money)
        .approvalCode(approvalCode)
        .build()
    paymentService.payment(terminal, payment, delegate)
}
```

**Use Cases:**
- Hotel accommodation (extra charges)
- Car rental
- Restaurant (adding tip)

#### Card Not Present - Reservation to Sale
Close pre-authorization without card (phone/online).

```kotlin
fun startSaleAfterReservation(
    terminal: ExternalTerminal,
    money: Money,
    approvalCode: String,
    token: String
) {
    val payment = Payment.builder()
        .type(Payment.Type.SALE)
        .amount(money)
        .approvalCode(approvalCode)
        .token(token)
        .build()
    paymentService.payment(terminal, payment, delegate)
}
```

#### Authorisation By Voice - Manual Authorization
Transaction with authorization code obtained by phone.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(money)
    .voiceReferralAID(voiceAuthCode)
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

**Use Cases:**
- When online authorization fails
- High amount transactions
- Bank phone approval

#### DCC - Dynamic Currency Conversion
Payment option in foreign cardholders' own currency.

```kotlin
// Inside PaymentDelegate
override fun printDccOffer(receipt: PaymentReceipt?) {
    // Show DCC offer receipt
    // Present accept/reject option to customer
}
```

---

### 5. Terminal Management

#### Login - Terminal Login
Starts terminal session.

```kotlin
val terminalService = TerminalService()
terminalService.login(terminal, delegate)
```

#### Diagnosis - Terminal Diagnostics
Performs terminal health check.

```kotlin
val opiDEService = OpiDEService()
opiDEService.terminalOperation(terminal, delegate, TerminalOperationType.DIAGNOSIS)
```

**Diagnosis Types:**
- `DIAGNOSIS` - General diagnostics
- `EMV_DIAGNOSIS` - EMV chip diagnostics
- `CONFIGURATION_DIAGNOSIS` - Configuration check

#### Config Data - Configuration Information
Gets terminal configuration data.

```kotlin
opiDEService.terminalAdministrationOperation(
    terminal,
    delegate,
    TerminalAdministrationOperationType.CONFIG_DATA
)
```

#### Service Menu - Service Menu
Opens terminal service menu.

```kotlin
opiDEService.startServiceMenu(terminal, delegate)
```

#### Check Password - Password Verification
Verifies terminal password.

```kotlin
opiDEService.checkPassword(terminal, delegate)
```

#### Call TMS - Terminal Management System
Connects to TMS server for updates/configuration.

```kotlin
opiDEService.callTMS(terminal, delegate, "jobName")
```

#### Factory Reset
Resets terminal to factory settings.

```kotlin
// For OPI-DE protocol
opiDEService.factoryReset(terminal, delegate)
// or
opiDEService.resetToFactorySettings(terminal, delegate)
```

#### Elme Version Info
Gets terminal software version information.

```kotlin
opiDEService.elmeVersionInfo(terminal, delegate)
```

#### Ticket Reprint - Receipt Reprint
Reprints last period closing receipt.

```kotlin
terminalService.ticketReprintPeriodClosing(terminal, delegate)
```

#### Repeat Last Service Message
Repeats last service message.

```kotlin
terminalService.repeatLastServiceMessage(terminal, delegate)
```

---

### 6. Hardware Features (Hardware API)

#### Barcode / QR Code Scanner
Scan barcodes and QR codes with camera.

```kotlin
class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var paxService: PaxService
    private lateinit var surfaceView: SurfaceView

    fun startScanner() {
        paxService = PaxService()
        paxService.scanBarcode(surfaceView, object : BarcodeScannerDelegate {
            override fun onSuccess(barcodeScannerResult: String) {
                // Barcode/QR content
                handleBarcode(barcodeScannerResult)
            }

            override fun onError(error: Error) {
                // Error handling
            }
        }, this)
    }

    override fun onPause() {
        paxService.stopScanBarcode()
        super.onPause()
    }
}
```

**Use Cases:**
- Product barcode scanning
- QR code payment
- Loyalty card scanning
- Bill payment (QR code)

#### Terminal Reboot
Restarts the device.

```kotlin
val paxService = PaxService()
paxService.reboot(context)
```

#### NFC Operations
NFC card read and write operations.

```kotlin
// NFC access via PaxService
paxService.readNfc(...)
```

#### Internal Printer
Direct access to terminal's internal printer.

```kotlin
// Receipt printing operations
paxService.print(...)
```

---

### 7. Special Operations

#### Tax-Free Operations
Tax-free sales for tourists.

```kotlin
val terminalCommandRequest = TerminalCommandRequest.builder()
    // Tax-free parameters
    .build()
opiDEService.terminalCommand(terminal, delegate, terminalCommandRequest, agent)
```

#### Flexo - Fuel Card
Special card support for gas stations.

```kotlin
opiDEService.flexo(terminal, delegate)
```

#### Mobile Phone Prepaid
Mobile phone top-up.

```kotlin
// Prepaid operator integration
```

#### OAM Server Applications
OAM (Operator Application Module) server operations.

```kotlin
opiDEService.oamServerApplications(terminal, delegate)
```

#### Online Agent
Online agent operations.

```kotlin
val onlineAgentRequest = OnlineAgentRequest.builder()
    .build()
paymentService.onlineAgent(terminal, onlineAgentRequest, delegate)
```

---

## SDK Class Reference

### Service Classes

| Class | Description |
|-------|-------------|
| `PaymentService` | Payment operations (implements `PaymentApi`) |
| `TerminalService` | Terminal management (implements `TerminalApi`) |
| `OpiDEService` | OPI-DE protocol specific operations (implements `OpiDEApi`) |
| `PaxService` | PAX hardware access (barcode, printer, NFC) |

### Delegate Interfaces

| Interface | Description |
|-----------|-------------|
| `PaymentDelegate` | Payment result callbacks |
| `TerminalDelegate` | Terminal operation callbacks |
| `CardReadDelegate` | Card read callbacks |
| `CardReaderStatusDelegate` | Card reader status callbacks |
| `TokenDelegate` | Token operation callbacks |
| `BarcodeScannerDelegate` | Barcode scan callbacks |

### Payment Types

```kotlin
enum Payment.Type {
    SALE,                          // Sale
    REFUND,                        // Refund
    VOID,                          // Void
    RESERVATION,                   // Pre-authorization
    GIFT_CARD_BALANCE,            // Gift card balance
    ACTIVATE_RECHARGE_GIFT_CARD,  // Gift card activation
    // ...
}
```

### Terminal Configuration

```kotlin
val terminal = ExternalTerminal.builder()
    .ipAddress("127.0.0.1")           // Terminal IP
    .port(20002)                       // OPI-DE: 20002, OPI-NL: 4100
    .compatibilityPort(20007)          // OPI-DE: 20007, OPI-NL: 4102
    .socketMode(SocketMode.SINGLE_SOCKET)  // OPI-DE: SINGLE, OPI-NL: DUAL
    .terminalType(TerminalType.OPI_DE)
    .workstationId("WORKSTATION_001")
    .languageCode(LanguageCode.EN)
    .requestToken(true)                // Set true to get token
    .build()
```

---

## Protocol Comparison

| Feature | OPI-DE | OPI-NL |
|---------|--------|--------|
| Default Port | 20002 | 4100 |
| Compatibility Port | 20007 | 4102 |
| Socket Mode | Single Socket | Dual Socket |
| Usage Region | Germany | Netherlands/Belgium |
| Card Read | Available | Not Available |
| Factory Reset | Available | Not Available |
| Service Menu | Available | Not Available |

---

## Usage Examples

### Simple Payment

```kotlin
val ccv = CCVPaymentManager.getInstance()

// With callback
ccv.makePayment(BigDecimal("25.50")) { result ->
    if (result.isSuccess) {
        Log.d("Payment", "Success: ${result.transactionId}")
    } else {
        Log.e("Payment", "Error: ${result.errorMessage}")
    }
}

// With coroutine
lifecycleScope.launch {
    val result = ccv.makePaymentAsync(BigDecimal("25.50"))
    // ...
}
```

### Refund Operation

```kotlin
ccv.refund(BigDecimal("10.00"), originalTransactionId = "TXN123") { result ->
    // ...
}
```

### End of Day (Z-Report)

```kotlin
ccv.periodClosing { result ->
    println("Total: ${result.totalAmount}")
    println("Transaction Count: ${result.transactionCount}")
}
```

### X-Report

```kotlin
ccv.partialPeriodClosing { result ->
    // Get report without closing the day
}
```

### Transaction History

```kotlin
ccv.getTransactionOverview { result ->
    result.transactions.forEach { tx ->
        println("${tx.type}: ${tx.amount} - ${tx.status}")
    }
}
```

### Terminal Status

```kotlin
ccv.getTerminalStatus { status ->
    println("Connected: ${status.isConnected}")
    println("Terminal ID: ${status.terminalId}")
}
```

---

## Error Codes

Common error codes returned from SDK:

| Code | Description |
|------|-------------|
| `CONNECTION_ERROR` | Terminal connection error |
| `TIMEOUT` | Operation timeout |
| `CARD_DECLINED` | Card declined |
| `CARD_EXPIRED` | Card expired |
| `INSUFFICIENT_FUNDS` | Insufficient balance |
| `INVALID_CARD` | Invalid card |
| `TERMINAL_BUSY` | Terminal busy |
| `USER_CANCELLED` | User cancelled |
| `PRINTER_ERROR` | Printer error |

---

## AndroidManifest Configuration

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application android:name=".CCVPaymentApp">
    <activity
        android:name=".ui.MainActivity"
        android:launchMode="singleTask"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
        <intent-filter>
            <action android:name="eu.ccv.service.ECR" />
            <category android:name="android.intent.category.DEFAULT" />
        </intent-filter>
    </activity>
</application>
```

---

## Notes

1. **MAPI.initialize()** must be called once in Application class
2. **singleTask** launchMode is required (for terminal callback)
3. All operations are **async** - UI thread is not blocked
4. Java 8 desugaring is active (coreLibraryDesugaring)

---

## License

This project is developed under CCV mAPI SDK license for use on CCV terminal devices.

## Resources

- [CCV Developer Portal](https://developer.myccv.eu/)
- [Android SDK Documentation](https://developer.myccv.eu/documentation/android_sdk/)
- [API Reference](https://developer.myccv.eu/documentation/android_sdk/api_reference/)
