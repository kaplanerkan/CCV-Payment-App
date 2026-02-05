# CCV Payment App

> **⚠️ Önemli Uyarı**
>
> Bu proje yalnızca elinizde **CCV Debug POS cihazı** varsa çalışır. Uygulama, test ve geliştirme için debug modu etkinleştirilmiş fiziksel bir CCV terminali gerektirir. Debug POS cihazı edinmek için lütfen doğrudan CCV firması ile iletişime geçin.
>
> **İletişim:** [CCV Developer Portal](https://developer.myccv.eu/) | [CCV Web Sitesi](https://www.ccv.eu/)

---

Android ödeme terminal entegrasyonu için geliştirilmiş uygulama. CCV mAPI SDK kullanarak POS terminal işlemlerini yönetir.

## Gereksinimler

- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 2.3.0
- Gradle 8.14.4
- CCV mAPI SDK 1.33

## Kurulum

1. `libs` klasörüne kendi CCV SDK AAR dosyalarınızı ekleyin:
   - `api-hardware-1.33.aar`
   - `pi-api-1.33.aar`

2. Projeyi build edin:
```bash
./gradlew assembleDebug
```

## Proje Yapısı

```
app/src/main/java/com/example/ccvpayment/
├── CCVPaymentApp.kt              # Application sınıfı (MAPI.initialize)
├── helper/
│   ├── PaymentHelper.kt          # Ödeme işlemleri
│   ├── TerminalHelper.kt         # Terminal yönetimi
│   ├── TransactionHelper.kt      # İşlem geçmişi ve raporlar
│   └── CCVPaymentManager.kt      # Genel yönetim sınıfı
├── model/
│   └── PaymentModels.kt          # Veri modelleri
└── ui/
    ├── MainActivity.kt           # Ana ekran
    ├── PaymentActivity.kt        # Ödeme ekranı
    ├── TransactionHistoryActivity.kt
    ├── SettingsActivity.kt
    └── TerminalSettingsActivity.kt
```

---

## Mevcut Özellikler

### Ödeme İşlemleri

| Özellik | Metod | Açıklama |
|---------|-------|----------|
| Satış (SALE) | `PaymentHelper.makePayment()` | Standart kart ile ödeme |
| İade (REFUND) | `PaymentHelper.refund()` | Ödeme iadesi |
| İptal (VOID) | `PaymentHelper.reversal()` | İşlem iptali |
| Ön Provizyon | `PaymentHelper.authorise()` | Rezervasyon/blokaj |
| Provizyon Kapama | `PaymentHelper.capture()` | Rezervasyonu satışa çevir |
| Ödeme Durdur | `PaymentHelper.stopPayment()` | Devam eden ödemeyi iptal et |

### Terminal İşlemleri

| Özellik | Metod | Açıklama |
|---------|-------|----------|
| Terminal Durumu | `TerminalHelper.getStatus()` | Bağlantı ve durum kontrolü |
| Terminal Başlat | `TerminalHelper.startup()` | Terminal başlatma |
| Terminal Aktivasyon | `TerminalHelper.activateTerminal()` | Terminal aktivasyonu |
| Son Mesaj | `TerminalHelper.repeatLastMessage()` | Son mesajı tekrarla |
| Son Fiş | `TerminalHelper.retrieveLastTicket()` | Son fişi al |
| Ödeme Kurtar | `TerminalHelper.recoverPayment()` | Kesilen ödemeyi kurtar |

### Rapor İşlemleri

| Özellik | Metod | Açıklama |
|---------|-------|----------|
| Dönem Kapama | `TerminalHelper.periodClosing()` | Z-Raporu / Gün sonu |
| İşlem Özeti | `TerminalHelper.transactionOverview()` | X-Raporu / Anlık özet |

---

## Eklenebilecek Özellikler

CCV mAPI SDK'nın desteklediği ancak henüz entegre edilmemiş özellikler:

### 1. Kart İşlemleri (Card Operations)

#### Card Read - Ödeme Öncesi Kart Okuma
Ödeme yapmadan önce kartı okuyup bilgileri almak için kullanılır.

```kotlin
// SDK Kullanımı
val opiDEService = OpiDEService()
val cardReadRequest = CardReadRequest.builder()
    .ageVerification(AgeVerification.builder()
        .value("16/18")
        .extendedVerification(true)
        .build())
    .build()
opiDEService.cardRead(terminal, cardReadDelegate, cardReadRequest)
```

**Kullanım Alanları:**
- Yaş doğrulama (alkol, sigara satışı)
- Müşteri tanıma (loyalty programları)
- Kart tipi kontrolü

#### Card Read SecureID - Güvenli Kart Okuma
Hash algoritması ile güvenli kart kimliği alma.

```kotlin
val cardReadRequest = CardReadRequest.builder()
    .hashAlgorithm(HashAlgorithm.DEFAULT)
    .customerSaltIndex("0-255")
    .build()
opiDEService.cardRead(terminal, delegate, cardReadRequest)
```

**Kullanım Alanları:**
- Recurring payment token oluşturma
- Müşteri kimlik doğrulama

#### Card Detection - Kart Tipi Algılama
Kart markası ve tipini algılar.

```kotlin
val cardDetectionRequest = CardDetectionRequest.builder()
    .build()
paymentService.cardDetection(terminal, cardDetectionRequest, delegate)
```

**Kullanım Alanları:**
- Kart markası bazlı indirimler
- Belirli kartları reddetme/kabul etme

#### Card Circuits - Desteklenen Kartlar
Terminal'in desteklediği kart markalarını listeler.

```kotlin
val terminalService = TerminalService()
terminalService.cardCircuits(terminal, delegate)
```

#### Read Mifare UID - NFC Kart ID Okuma
Mifare NFC kartların benzersiz ID'sini okur.

```kotlin
val terminalService = TerminalService()
terminalService.readMifareUID(terminal, tokenDelegate)
```

**Kullanım Alanları:**
- Personel kartı doğrulama
- Müşteri sadakat kartı
- Erişim kontrolü

---

### 2. Token İşlemleri (Tokenization)

#### Token Alma
Kart bilgilerini tokenize ederek saklamak için kullanılır.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(Money(BigDecimal("0.00"), Currency.getInstance("EUR")))
    .hashAlgorithm(HashAlgorithm.DEFAULT)
    .build()
paymentService.payment(terminal, payment, delegate)
```

**Kullanım Alanları:**
- Abonelik ödemeleri
- Tek tıkla ödeme
- Recurring payments

#### Token ile İade
Daha önce alınan token ile iade işlemi.

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

#### Token ile Satış (Card Not Present)
Kart fiziksel olarak olmadan token ile ödeme.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(money)
    .token(savedToken)
    .build()
paymentService.payment(terminal, payment, delegate)
```

---

### 3. Gift Card İşlemleri

#### Gift Card Bakiye Sorgulama
Hediye kartı bakiyesini kontrol eder.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.GIFT_CARD_BALANCE)
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

#### Gift Card Aktivasyon / Yükleme
Hediye kartını aktifleştirir veya bakiye yükler.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.ACTIVATE_RECHARGE_GIFT_CARD)
    .amount(Money(BigDecimal("100.00"), Currency.getInstance("EUR")))
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

---

### 4. Gelişmiş Ödeme Özellikleri

#### Reservation Adjustment - Provizyon Güncelleme
Mevcut ön provizyonun tutarını değiştirir.

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

**Kullanım Alanları:**
- Otel konaklama (ekstra harcamalar)
- Araç kiralama
- Restoran (bahşiş ekleme)

#### Card Not Present - Reservation to Sale
Kart olmadan provizyon kapama (telefon/online).

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

#### Authorisation By Voice - Manuel Yetkilendirme
Telefon ile alınan yetki kodu ile işlem.

```kotlin
val payment = Payment.builder()
    .type(Payment.Type.SALE)
    .amount(money)
    .voiceReferralAID(voiceAuthCode)
    .posTimestamp(timestamp)
    .build()
paymentService.payment(terminal, payment, delegate)
```

**Kullanım Alanları:**
- Online yetkilendirme başarısız olduğunda
- Yüksek tutarlı işlemler
- Banka telefon onayı

#### DCC - Dynamic Currency Conversion
Yabancı kart sahiplerine kendi para biriminde ödeme seçeneği.

```kotlin
// PaymentDelegate içinde
override fun printDccOffer(receipt: PaymentReceipt?) {
    // DCC teklif fişini göster
    // Müşteri kabul/red seçeneği sun
}
```

---

### 5. Terminal Yönetimi

#### Login - Terminal Girişi
Terminal oturumu başlatır.

```kotlin
val terminalService = TerminalService()
terminalService.login(terminal, delegate)
```

#### Diagnosis - Terminal Tanılama
Terminal sağlık kontrolü yapar.

```kotlin
val opiDEService = OpiDEService()
opiDEService.terminalOperation(terminal, delegate, TerminalOperationType.DIAGNOSIS)
```

**Diagnosis Tipleri:**
- `DIAGNOSIS` - Genel tanılama
- `EMV_DIAGNOSIS` - EMV chip tanılama
- `CONFIGURATION_DIAGNOSIS` - Yapılandırma kontrolü

#### Config Data - Yapılandırma Bilgisi
Terminal yapılandırma verilerini alır.

```kotlin
opiDEService.terminalAdministrationOperation(
    terminal,
    delegate,
    TerminalAdministrationOperationType.CONFIG_DATA
)
```

#### Service Menu - Servis Menüsü
Terminal servis menüsünü açar.

```kotlin
opiDEService.startServiceMenu(terminal, delegate)
```

#### Check Password - Şifre Doğrulama
Terminal şifresini kontrol eder.

```kotlin
opiDEService.checkPassword(terminal, delegate)
```

#### Call TMS - Terminal Management System
TMS sunucusuna bağlanarak güncelleme/yapılandırma alır.

```kotlin
opiDEService.callTMS(terminal, delegate, "jobName")
```

#### Factory Reset
Terminal'i fabrika ayarlarına döndürür.

```kotlin
// OPI-DE protokolü için
opiDEService.factoryReset(terminal, delegate)
// veya
opiDEService.resetToFactorySettings(terminal, delegate)
```

#### Elme Version Info
Terminal yazılım versiyon bilgisini alır.

```kotlin
opiDEService.elmeVersionInfo(terminal, delegate)
```

#### Ticket Reprint - Fiş Tekrar Basma
Son dönem kapama fişini tekrar basar.

```kotlin
terminalService.ticketReprintPeriodClosing(terminal, delegate)
```

#### Repeat Last Service Message
Son servis mesajını tekrarlar.

```kotlin
terminalService.repeatLastServiceMessage(terminal, delegate)
```

---

### 6. Donanım Özellikleri (Hardware API)

#### Barkod / QR Kod Tarayıcı
Kamera ile barkod ve QR kod tarama.

```kotlin
class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var paxService: PaxService
    private lateinit var surfaceView: SurfaceView

    fun startScanner() {
        paxService = PaxService()
        paxService.scanBarcode(surfaceView, object : BarcodeScannerDelegate {
            override fun onSuccess(barcodeScannerResult: String) {
                // Barkod/QR içeriği
                handleBarcode(barcodeScannerResult)
            }

            override fun onError(error: Error) {
                // Hata yönetimi
            }
        }, this)
    }

    override fun onPause() {
        paxService.stopScanBarcode()
        super.onPause()
    }
}
```

**Kullanım Alanları:**
- Ürün barkodu okuma
- QR kod ile ödeme
- Loyalty kart tarama
- Fatura ödeme (karekod)

#### Terminal Reboot
Cihazı yeniden başlatır.

```kotlin
val paxService = PaxService()
paxService.reboot(context)
```

#### NFC İşlemleri
NFC kart okuma ve yazma işlemleri.

```kotlin
// PaxService üzerinden NFC erişimi
paxService.readNfc(...)
```

#### Dahili Yazıcı
Terminal'in dahili yazıcısına doğrudan erişim.

```kotlin
// Fiş basma işlemleri
paxService.print(...)
```

---

### 7. Özel İşlemler

#### Tax-Free İşlemleri
Turistlere vergisiz satış.

```kotlin
val terminalCommandRequest = TerminalCommandRequest.builder()
    // Tax-free parametreleri
    .build()
opiDEService.terminalCommand(terminal, delegate, terminalCommandRequest, agent)
```

#### Flexo - Akaryakıt Kartı
Akaryakıt istasyonları için özel kart desteği.

```kotlin
opiDEService.flexo(terminal, delegate)
```

#### Mobile Phone Prepaid
Mobil telefon kontör yükleme.

```kotlin
// Prepaid operatör entegrasyonu
```

#### OAM Server Applications
OAM (Operator Application Module) sunucu işlemleri.

```kotlin
opiDEService.oamServerApplications(terminal, delegate)
```

#### Online Agent
Online acente işlemleri.

```kotlin
val onlineAgentRequest = OnlineAgentRequest.builder()
    .build()
paymentService.onlineAgent(terminal, onlineAgentRequest, delegate)
```

---

## SDK Sınıfları Referansı

### Servis Sınıfları

| Sınıf | Açıklama |
|-------|----------|
| `PaymentService` | Ödeme işlemleri (implements `PaymentApi`) |
| `TerminalService` | Terminal yönetimi (implements `TerminalApi`) |
| `OpiDEService` | OPI-DE protokolü özel işlemleri (implements `OpiDEApi`) |
| `PaxService` | PAX donanım erişimi (barkod, yazıcı, NFC) |

### Delegate Arayüzleri

| Arayüz | Açıklama |
|--------|----------|
| `PaymentDelegate` | Ödeme sonuç callback'leri |
| `TerminalDelegate` | Terminal işlem callback'leri |
| `CardReadDelegate` | Kart okuma callback'leri |
| `CardReaderStatusDelegate` | Kart okuyucu durum callback'leri |
| `TokenDelegate` | Token işlem callback'leri |
| `BarcodeScannerDelegate` | Barkod tarama callback'leri |

### Payment Types

```kotlin
enum Payment.Type {
    SALE,                          // Satış
    REFUND,                        // İade
    VOID,                          // İptal
    RESERVATION,                   // Ön provizyon
    GIFT_CARD_BALANCE,            // Gift card bakiye
    ACTIVATE_RECHARGE_GIFT_CARD,  // Gift card aktivasyon
    // ...
}
```

### Terminal Yapılandırması

```kotlin
val terminal = ExternalTerminal.builder()
    .ipAddress("127.0.0.1")           // Terminal IP
    .port(20002)                       // OPI-DE: 20002, OPI-NL: 4100
    .compatibilityPort(20007)          // OPI-DE: 20007, OPI-NL: 4102
    .socketMode(SocketMode.SINGLE_SOCKET)  // OPI-DE: SINGLE, OPI-NL: DUAL
    .terminalType(TerminalType.OPI_DE)
    .workstationId("WORKSTATION_001")
    .languageCode(LanguageCode.EN)
    .requestToken(true)                // Token almak için true
    .build()
```

---

## Protokol Karşılaştırması

| Özellik | OPI-DE | OPI-NL |
|---------|--------|--------|
| Varsayılan Port | 20002 | 4100 |
| Uyumluluk Portu | 20007 | 4102 |
| Socket Modu | Single Socket | Dual Socket |
| Kullanım Bölgesi | Almanya | Hollanda/Belçika |
| Card Read | Var | Yok |
| Factory Reset | Var | Yok |
| Service Menu | Var | Yok |

---

## Kullanım Örnekleri

### Basit Ödeme

```kotlin
val ccv = CCVPaymentManager.getInstance()

// Callback ile
ccv.makePayment(BigDecimal("25.50")) { result ->
    if (result.isSuccess) {
        Log.d("Payment", "Başarılı: ${result.transactionId}")
    } else {
        Log.e("Payment", "Hata: ${result.errorMessage}")
    }
}

// Coroutine ile
lifecycleScope.launch {
    val result = ccv.makePaymentAsync(BigDecimal("25.50"))
    // ...
}
```

### İade İşlemi

```kotlin
ccv.refund(BigDecimal("10.00"), originalTransactionId = "TXN123") { result ->
    // ...
}
```

### Gün Sonu (Z-Report)

```kotlin
ccv.periodClosing { result ->
    println("Toplam: ${result.totalAmount}")
    println("İşlem Sayısı: ${result.transactionCount}")
}
```

### X-Rapor

```kotlin
ccv.partialPeriodClosing { result ->
    // Gün sonu kapatmadan rapor alır
}
```

### İşlem Geçmişi

```kotlin
ccv.getTransactionOverview { result ->
    result.transactions.forEach { tx ->
        println("${tx.type}: ${tx.amount} - ${tx.status}")
    }
}
```

### Terminal Durumu

```kotlin
ccv.getTerminalStatus { status ->
    println("Bağlı: ${status.isConnected}")
    println("Terminal ID: ${status.terminalId}")
}
```

---

## Hata Kodları

SDK'dan dönen yaygın hata kodları:

| Kod | Açıklama |
|-----|----------|
| `CONNECTION_ERROR` | Terminal bağlantı hatası |
| `TIMEOUT` | İşlem zaman aşımı |
| `CARD_DECLINED` | Kart reddedildi |
| `CARD_EXPIRED` | Kart süresi dolmuş |
| `INSUFFICIENT_FUNDS` | Yetersiz bakiye |
| `INVALID_CARD` | Geçersiz kart |
| `TERMINAL_BUSY` | Terminal meşgul |
| `USER_CANCELLED` | Kullanıcı iptal etti |
| `PRINTER_ERROR` | Yazıcı hatası |

---

## AndroidManifest Yapılandırması

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

## Notlar

1. **MAPI.initialize()** Application sınıfında bir kez çağrılmalı
2. **singleTask** launchMode zorunlu (terminal callback için)
3. Tüm işlemler **async** - UI thread'i bloklanmaz
4. Java 8 desugaring aktif (coreLibraryDesugaring)

---

## Lisans

Bu proje CCV mAPI SDK lisansı altında CCV terminal cihazlarında kullanılmak üzere geliştirilmiştir.

## Kaynaklar

- [CCV Developer Portal](https://developer.myccv.eu/)
- [Android SDK Documentation](https://developer.myccv.eu/documentation/android_sdk/)
- [API Reference](https://developer.myccv.eu/documentation/android_sdk/api_reference/)
