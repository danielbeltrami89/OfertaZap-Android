package br.com.beltramitech.ofertazap.domain

import br.com.beltramitech.ofertazap.data.MercadoLivreItem
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class OfferMessageBuilder {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun buildMessage(
        item: MercadoLivreItem,
        shareUrl: String? = null,
        headline: String? = null,
        footer: String? = null
    ): String {
        val headlineLine = headline?.let { "🔥 $it 🔥" }
        val footerLine = footer?.let { "_${it}_" }
        val originalPriceLine = item.originalPrice?.let { "💰 de: ${formatCurrency(it)}" }
        val priceLine = item.price?.let { "✅ por: ${formatCurrency(it)}" }
        val linkUrl = shareUrl ?: item.permalink

        return listOfNotNull(
            headlineLine,
            headlineLine?.let { "" },
            "✨ ${item.title.uppercase()}",
            "",
            originalPriceLine,
            priceLine,
            "",
            "🚚 Confira no Mercado Livre",
            "",
            "🛒 $linkUrl",
            footerLine?.let { "" },
            footerLine
        ).joinToString("\n")
    }

    fun buildFallbackMessage(url: String, headline: String? = null, footer: String? = null): String {
        val headlineLine = headline?.let { "🔥 $it 🔥" }
        val footerLine = footer?.let { "_${it}_" }

        return listOfNotNull(
            headlineLine,
            headlineLine?.let { "" },
            "🚚 Confira no Mercado Livre",
            "",
            "🛒 $url",
            footerLine?.let { "" },
            footerLine
        ).joinToString("\n")
    }

    private fun formatCurrency(value: BigDecimal): String {
        return currencyFormatter.format(value)
    }
}
