package dev.beltramitech.ofertazap

import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.beltramitech.ofertazap.data.AnalyticsTracker
import dev.beltramitech.ofertazap.ui.settings.ContentView
import dev.beltramitech.ofertazap.ui.settings.SettingsViewModel
import dev.beltramitech.ofertazap.ui.settings.SettingsViewModelFactory
import dev.beltramitech.ofertazap.ui.theme.OfertaZapTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(applicationContext)
    }
    private lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        analyticsTracker = AnalyticsTracker(applicationContext)
        analyticsTracker.logScreenView(AnalyticsTracker.SCREEN_HOME)

        setContent {
            OfertaZapTheme {
                var showRatingPrompt by remember { mutableStateOf(shouldShowRatingPrompt()) }

                ContentView(
                    viewModel = viewModel,
                    analyticsTracker = analyticsTracker,
                    onShare = ::shareMessage,
                    showRatingPrompt = showRatingPrompt,
                    onRatingPromptDismiss = {
                        analyticsTracker.logClick("home_rating_prompt_dismiss", AnalyticsTracker.SCREEN_HOME)
                        markRatingPromptShown()
                        showRatingPrompt = false
                    },
                    onRatingPromptConfirm = {
                        analyticsTracker.logClick("home_rating_prompt_open_store", AnalyticsTracker.SCREEN_HOME)
                        markRatingPromptShown()
                        showRatingPrompt = false
                        openPlayStoreRating()
                    }
                )
            }
        }
    }

    private fun shareMessage(message: String) {
        analyticsTracker.logClick("home_share_message", AnalyticsTracker.SCREEN_HOME)
        copyMessage(message)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar oferta"))
    }

    private fun copyMessage(message: String) {
        analyticsTracker.logClick("home_copy_message", AnalyticsTracker.SCREEN_HOME)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OfertaZap", message))
    }

    private fun shouldShowRatingPrompt(): Boolean {
        val preferences = getSharedPreferences(RATING_PROMPT_PREFERENCES, Context.MODE_PRIVATE)
        return preferences.getInt(RATING_PROMPT_VERSION_KEY, 0) < BuildConfig.VERSION_CODE
    }

    private fun markRatingPromptShown() {
        getSharedPreferences(RATING_PROMPT_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putInt(RATING_PROMPT_VERSION_KEY, BuildConfig.VERSION_CODE)
            .apply()
    }

    private fun openPlayStoreRating() {
        val packageName = applicationContext.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            setPackage("com.android.vending")
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )

        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }

    private companion object {
        const val RATING_PROMPT_PREFERENCES = "rating_prompt_preferences"
        const val RATING_PROMPT_VERSION_KEY = "last_prompt_version_code"
    }
}
