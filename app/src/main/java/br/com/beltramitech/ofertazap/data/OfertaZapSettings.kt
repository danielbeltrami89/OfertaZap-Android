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

    val normalizedHeadline: String?
        get() = messageHeadline.trim().ifEmpty { null }

    private companion object {
        const val HEADLINE_KEY = "messageHeadline"
    }
}
