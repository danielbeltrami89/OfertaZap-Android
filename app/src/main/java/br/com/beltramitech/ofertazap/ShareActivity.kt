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
import dev.beltramitech.ofertazap.domain.SharedUrlExtractor
import dev.beltramitech.ofertazap.ui.share.ShareScreen
import dev.beltramitech.ofertazap.ui.share.ShareViewModel
import dev.beltramitech.ofertazap.ui.share.ShareViewModelFactory
import dev.beltramitech.ofertazap.ui.theme.OfertaZapTheme

class ShareActivity : ComponentActivity() {
    private val viewModel: ShareViewModel by viewModels {
        ShareViewModelFactory(applicationContext)
    }
    private val sharedUrlExtractor = SharedUrlExtractor()
    private lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsTracker = AnalyticsTracker(applicationContext)
        analyticsTracker.logScreenView(AnalyticsTracker.SCREEN_SHARE)
        handleIncomingIntent(intent)

        setContent {
            OfertaZapTheme {
                ShareScreen(
                    viewModel = viewModel,
                    analyticsTracker = analyticsTracker,
                    onClose = {
                        analyticsTracker.logClick("share_close", AnalyticsTracker.SCREEN_SHARE)
                        finish()
                    },
                    onShare = ::shareMessage,
                    onCopy = ::copyMessage
                )
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
        val sharedText = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        val url = sharedText?.let(sharedUrlExtractor::firstUrl)

        if (url == null) {
            finish()
            return
        }

        analyticsTracker.logImpression("incoming_share_link", AnalyticsTracker.SCREEN_SHARE)
        viewModel.loadOffer(url, sharedText)
    }

    private fun shareMessage(message: String) {
        analyticsTracker.logClick("share_send_message", AnalyticsTracker.SCREEN_SHARE)
        copyMessage(message)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar oferta"))
        finish()
    }

    private fun copyMessage(message: String) {
        analyticsTracker.logClick("share_copy_message", AnalyticsTracker.SCREEN_SHARE)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OfertaZap", message))
    }
}
