# CCV Payment App

[![Türkçe](https://img.shields.io/badge/Dil-Türkçe-red)](README.TR.md)

> **⚠️ Important Notice**
>
> This project only works if you have a **CCV Debug POS device**. The application requires a physical CCV terminal with debug mode enabled for testing and development. To obtain a Debug POS device, please contact CCV directly.
>
> **Contact:** [CCV Developer Portal](https://developer.myccv.eu/) | [CCV Website](https://www.ccv.eu/)

---

Android payment terminal integration application. Manages POS terminal operations using CCV mAPI SDK with **Flow-based architecture**.

## Screenshots

<p align="center">
  <img src="screenshots/main_screen.png" width="200" alt="Main Screen"/>
  <img src="screenshots/payment_screen.png" width="200" alt="Payment Screen"/>
  <img src="screenshots/terminal_payment.png" width="200" alt="Terminal Payment"/>
</p>

<p align="center">
  <img src="screenshots/settings_connection.png" width="200" alt="Settings - Connection"/>
  <img src="screenshots/settings_operations.png" width="200" alt="Settings - Operations"/>
</p>

## Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 2.0.0
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

## Architecture

This project uses a **Flow-based architecture** similar to the official CCV mAPI demo application:

```
┌─────────────────────────────────────────────────────────────────┐
│                      PaymentActivity                             │
│                    (extends FlowActivity)                        │
├─────────────────────────────────────────────────────────────────┤
│  - Numpad UI for amount entry                                    │
│  - Payment type selection (Sale, Refund, Pre-Auth)              │
│  - Real-time log display                                         │
│  - onPaymentSuccess() / onPaymentError() callbacks              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       FlowActivity                               │
│                (implements DelegationFactory)                    │
├─────────────────────────────────────────────────────────────────┤
│  - activeFlow: Flow? (state management)                          │
│  - startFlow() / finishFlow() pattern                           │
│  - createPaymentDelegate() / createTerminalDelegate()            │
│  - launchPaymentScreen() for Android 10+                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    OpiDeFlowHandler                              │
│               (implements PaymentFlowHandler)                    │
├─────────────────────────────────────────────────────────────────┤
│  - startPayment() → PaymentService.payment()                     │
│  - startRefund() → PaymentService.payment()                      │
│  - startAbort() → PaymentService.abort()                         │
│  - performPeriodClosing() → TerminalService.periodClosing()      │
└─────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/com/example/ccvpayment/
├── CCVPaymentApp.kt              # Application class (MAPI.initialize)
├── flow/                         # Flow-based architecture
│   ├── Flow.kt                   # Flow enum (PAYMENT, REFUND, etc.)
│   ├── DelegationFactory.kt      # Delegate factory interface
│   ├── PaymentFlowHandler.kt     # Handler interface
│   ├── OpiDeFlowHandler.kt       # OPI-DE protocol implementation
│   └── FlowActivity.kt           # Base activity with state management
├── helper/
│   ├── PaymentHelper.kt          # Payment operations
│   ├── TerminalHelper.kt         # Terminal management
│   ├── TransactionHelper.kt      # Transaction history and reports
│   ├── CCVPaymentManager.kt      # Facade class
│   └── CCVLogger.kt              # Logging utilities
├── model/
│   └── PaymentModels.kt          # Data models
└── ui/
    ├── MainActivity.kt           # Main screen
    ├── PaymentActivity.kt        # Payment screen (extends FlowActivity)
    ├── TransactionHistoryActivity.kt
    ├── SettingsActivity.kt
    └── TerminalSettingsActivity.kt
```

---

## Flow Pattern

The application uses a state machine pattern for payment operations:

### Flow States
```kotlin
enum class Flow {
    PAYMENT,           // Sale transaction
    REFUND,            // Refund transaction
    VOID,              // Void/reversal
    AUTHORISE,         // Pre-authorization
    CAPTURE,           // Capture pre-auth
    STATUS,            // Terminal status
    ABORT,             // Abort current operation
    PERIOD_CLOSING,    // Z-Report
    TRANSACTION_OVERVIEW, // X-Report
    // ... more flows
}
```

### Flow Lifecycle
```kotlin
// 1. Check if flow is active
if (activeFlow == null) {
    // 2. Start the flow
    startFlow(Flow.PAYMENT)

    // 3. Call flow handler (registers delegate, non-blocking)
    flowHandler.startPayment(terminal, payment)

    // 4. Launch payment UI (Android 10+)
    launchPaymentScreen()
}

// 5. SDK invokes delegate callback
override fun onPaymentSuccess(result) {
    // Handle success
    finishFlow()  // Sets activeFlow = null
}

// 6. onFlowFinished() is called
override fun onFlowFinished() {
    // Update UI, show dialog
}
```

---

## Current Features

### Payment Operations

| Feature | Flow | Method |
|---------|------|--------|
| Sale | `Flow.PAYMENT` | `flowHandler.startPayment()` |
| Refund | `Flow.REFUND` | `flowHandler.startRefund()` |
| Void | `Flow.VOID` | `flowHandler.startVoid()` |
| Pre-Authorization | `Flow.AUTHORISE` | `flowHandler.startReservation()` |
| Capture | `Flow.CAPTURE` | `flowHandler.startCapture()` |
| Abort | `Flow.ABORT` | `flowHandler.startAbort()` |

### Terminal Operations

| Feature | Flow | Method |
|---------|------|--------|
| Terminal Status | `Flow.STATUS` | `flowHandler.startStatus()` |
| Last Receipt | `Flow.RETRIEVE_LAST_TICKET` | `flowHandler.startRetrieveLastTicket()` |
| Repeat Last Message | `Flow.REPEAT_LAST_MESSAGE` | `flowHandler.startRepeatLastMessage()` |

### Report Operations

| Feature | Flow | Method |
|---------|------|--------|
| Period Closing (Z-Report) | `Flow.PERIOD_CLOSING` | `flowHandler.performPeriodClosing()` |
| Transaction Overview (X-Report) | `Flow.TRANSACTION_OVERVIEW` | `flowHandler.getTransactionOverview()` |

---

## Terminal Configuration

```kotlin
// OPI-DE Protocol (Germany)
val terminal = ExternalTerminal.builder()
    .ipAddress("127.0.0.1")        // Local terminal
    .port(30002)                    // OPI-DE port (default)
    .compatibilityPort(30007)       // OPI-DE compatibility
    .socketMode(SocketMode.SINGLE_SOCKET)
    .terminalType(TerminalType.OPI_DE)
    .workstationId("WORKSTATION_001")
    .languageCode(LanguageCode.EN)
    .build()
```

> **Note:** OPI-DE protocol uses port **30002** by default. This matches the official CCV mAPI demo app configuration.

---

## Usage Example

### Simple Payment
```kotlin
class MyPaymentActivity : FlowActivity() {

    fun startPayment(amount: BigDecimal) {
        if (!isFlowActive()) {
            startFlow(Flow.PAYMENT)

            val payment = buildPayment(Payment.Type.SALE, amount)
            flowHandler.startPayment(getLocalTerminal(), payment)
            launchPaymentScreen()
        }
    }

    override fun onPaymentSuccess(result: PaymentResult) {
        // Payment successful
        showSuccessDialog(result)
    }

    override fun onPaymentError(errorCode: String, errorMessage: String) {
        // Payment failed
        showErrorDialog(errorMessage)
    }

    override fun onFlowFinished() {
        // Reset UI state
        hideLoading()
    }
}
```

---

## Logging

The application includes comprehensive logging via `CCVLogger`:

```kotlin
// Request logging
CCVLogger.logPaymentRequest("SALE", terminal, request)

// Response logging
CCVLogger.logPaymentResponse("SALE", result)

// Event logging
CCVLogger.logEvent("FLOW_START", "Starting flow: PAYMENT")

// Error logging
CCVLogger.logError("PAYMENT", "DECLINED", "Card declined")
```

Log output format:
```
CCV_REQUEST  >>> SALE REQUEST
CCV_REQUEST  {"timestamp":"09:44:33","operation":"SALE","amount":"10,00 €"}
CCV_EVENT    --- EVENT: TERMINAL_OUTPUT
CCV_EVENT    {"event":"TERMINAL_OUTPUT","data":"Karte bitte"}
CCV_RESPONSE <<< SALE RESPONSE [SUCCESS]
```

---

## AndroidManifest Configuration

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Android 11+ intent visibility -->
<queries>
    <intent>
        <action android:name="eu.ccv.payment.action.SHOW_PAYMENT" />
    </intent>
</queries>

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
2. **singleTask** launchMode is required for MainActivity (ECR callback)
3. All operations are **async** - delegates handle callbacks
4. **Flow pattern** ensures only one operation at a time
5. **finishFlow()** must be called after every operation (success or error)
6. **Port 30002** is the default for OPI-DE protocol (not 20002)
7. **finishFlow()** should NOT call `bringToForeground()` - just reset state like the demo app

---

## License

This project is developed under CCV mAPI SDK license for use on CCV terminal devices.

## Resources

- [CCV Developer Portal](https://developer.myccv.eu/)
- [Android SDK Documentation](https://developer.myccv.eu/documentation/android_sdk/)
- [API Reference](https://developer.myccv.eu/documentation/android_sdk/api_reference/)
