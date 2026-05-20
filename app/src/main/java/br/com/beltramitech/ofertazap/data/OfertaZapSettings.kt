package br.com.beltramitech.ofertazap.data

import android.content.Context

class OfertaZapSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "ofertazap_settings",
        Context.MODE_PRIVATE
    )

    var messageHeadline: String
        get() = preferences.getString(HEADLINE_KEY, "") ?: ""
        set(value) {
            preferences.edit()
                .putString(HEADLINE_KEY, value.trim())
                .apply()
        }

    var messageFooter: String
        get() = preferences.getString(FOOTER_KEY, "") ?: ""
        set(value) {
            preferences.edit()
                .putString(FOOTER_KEY, value.trim())
                .apply()
        }

    val normalizedHeadline: String?
        get() = messageHeadline.trim().ifEmpty { null }

    val normalizedFooter: String?
        get() = messageFooter.trim().ifEmpty { null }

    private companion object {
        const val HEADLINE_KEY = "messageHeadline"
        const val FOOTER_KEY = "messageFooter"
    }
}
