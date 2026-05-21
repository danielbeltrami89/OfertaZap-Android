package br.com.beltramitech.ofertazap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.com.beltramitech.ofertazap.domain.SharedUrlExtractor
import br.com.beltramitech.ofertazap.ui.share.ShareScreen
import br.com.beltramitech.ofertazap.ui.share.ShareViewModel
import br.com.beltramitech.ofertazap.ui.share.ShareViewModelFactory
import br.com.beltramitech.ofertazap.ui.theme.OfertaZapTheme

class ShareActivity : ComponentActivity() {
    private val viewModel: ShareViewModel by viewModels {
        ShareViewModelFactory(applicationContext)
    }
    private val sharedUrlExtractor = SharedUrlExtractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            OfertaZapTheme {
                ShareScreen(
                    viewModel = viewModel,
                    onClose = ::finish,
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

        viewModel.loadOffer(url, sharedText)
    }

    private fun shareMessage(message: String) {
        copyMessage(message)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar oferta"))
        finish()
    }

    private fun copyMessage(message: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OfertaZap", message))
    }
}
