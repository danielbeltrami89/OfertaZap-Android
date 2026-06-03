package dev.beltramitech.ofertazap.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsTracker(context: Context) {
    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

    fun logScreenView(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
        )
    }

    fun logClick(name: String, screenName: String? = null) {
        logInteraction(EVENT_CLICK, name, screenName)
    }

    fun logImpression(name: String, screenName: String? = null) {
        logInteraction(EVENT_IMPRESSION, name, screenName)
    }

    private fun logInteraction(eventName: String, name: String, screenName: String?) {
        analytics.logEvent(
            eventName,
            Bundle().apply {
                putString(PARAM_NAME, name)
                screenName?.let { putString(FirebaseAnalytics.Param.SCREEN_NAME, it) }
            }
        )
    }

    companion object {
        const val SCREEN_HOME = "home"
        const val SCREEN_SHARE = "share"

        private const val EVENT_CLICK = "click"
        private const val EVENT_IMPRESSION = "impression"
        private const val PARAM_NAME = "name"
    }
}
