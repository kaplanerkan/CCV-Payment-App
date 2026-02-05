package com.example.ccvpayment.model

/**
 * İşlem Durumları
 */
enum class TransactionStatus {
    SUCCESS,    // Başarılı
    FAILED,     // Başarısız
    CANCELLED,  // İptal Edildi
    PENDING,    // Beklemede
    TIMEOUT,    // Zaman Aşımı
    UNKNOWN     // Bilinmiyor
}
