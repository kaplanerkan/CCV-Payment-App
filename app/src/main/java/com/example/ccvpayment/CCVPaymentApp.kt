package com.example.ccvpayment

import android.app.Application
import eu.ccvlab.api_android.MAPI

/**
 * CCV Payment Uygulaması
 *
 * MAPI SDK'nın başlatılması bu sınıfta yapılmalıdır.
 */
class CCVPaymentApp : Application() {

    companion object {
        lateinit var instance: CCVPaymentApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // CCV mAPI SDK'yı başlat
        MAPI.initialize(this)

        initializeApp()
    }

    private fun initializeApp() {
        // Uygulama başlangıç konfigürasyonları
    }
}
