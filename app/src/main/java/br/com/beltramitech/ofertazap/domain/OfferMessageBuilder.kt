package br.com.beltramitech.ofertazap.domain

import br.com.beltramitech.ofertazap.data.MercadoLivreItem
import br.com.beltramitech.ofertazap.data.ShopeeItem
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
        return buildFallbackMessage(
            url = url,
            platformName = ProductPlatform.MercadoLivre.displayName,
            headline = headline,
            footer = footer
        )
    }

    fun buildMessage(
        item: ShopeeItem,
        shareUrl: String? = null,
        headline: String? = null,
        footer: String? = null
    ): String {
        val headlineLine = headline?.let { "🔥 $it 🔥" }
        val footerLine = footer?.let { "_${it}_" }
        val originalPriceLine = item.originalPrice?.let { "💰 de: ${formatCurrency(it)}" }
        val priceLine = item.price?.let { "✅ por: ${formatCurrency(it)}" }
        val titleLine = item.title?.let { "✨ ${it.uppercase()}" }
        val linkUrl = shareUrl ?: item.permalink

        return listOfNotNull(
            headlineLine,
            headlineLine?.let { "" },
            titleLine,
            titleLine?.let { "" },
            originalPriceLine,
            priceLine,
            priceLine?.let { "" },
            "🚚 Confira na Shopee",
            "",
            "🛒 $linkUrl",
            footerLine?.let { "" },
            footerLine
        ).joinToString("\n")
    }

    fun buildFallbackMessage(
        url: String,
        platformName: String,
        headline: String? = null,
        footer: String? = null
    ): String {
        val headlineLine = headline?.let { "🔥 $it 🔥" }
        val footerLine = footer?.let { "_${it}_" }

        return listOfNotNull(
            headlineLine,
            headlineLine?.let { "" },
            "🚚 Confira ${platformArticle(platformName)} $platformName",
            "",
            "🛒 $url",
            footerLine?.let { "" },
            footerLine
        ).joinToString("\n")
    }

    private fun platformArticle(platformName: String): String {
        return if (platformName.equals("Shopee", ignoreCase = true)) "na" else "no"
    }

    private fun formatCurrency(value: BigDecimal): String {
        return currencyFormatter.format(value)
    }
}
