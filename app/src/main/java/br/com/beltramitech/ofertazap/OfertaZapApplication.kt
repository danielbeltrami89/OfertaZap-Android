package dev.beltramitech.ofertazap

import android.app.Application
import com.google.android.gms.ads.MobileAds

class OfertaZapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
