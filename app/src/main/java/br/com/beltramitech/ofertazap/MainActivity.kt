package dev.beltramitech.ofertazap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
        super.onCreate(savedInstanceState)
        analyticsTracker = AnalyticsTracker(applicationContext)
        analyticsTracker.logScreenView(AnalyticsTracker.SCREEN_HOME)

        setContent {
            OfertaZapTheme {
                ContentView(
                    viewModel = viewModel,
                    analyticsTracker = analyticsTracker,
                    onShare = ::shareMessage
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
}
