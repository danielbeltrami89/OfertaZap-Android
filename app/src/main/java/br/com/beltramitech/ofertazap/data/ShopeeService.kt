package dev.beltramitech.ofertazap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.text.Normalizer

class ShopeeService(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchItem(sharedUrl: String, sharedText: String? = null): ShopeeItem = withContext(Dispatchers.IO) {
        val sharedTextItem = extractItemFromSharedText(sharedText, sharedUrl)
        val resolvedPage = resolveFinalUrl(sharedUrl)
        val html = normalizeHtml(resolvedPage.body)

        val title = extractTitle(html) ?: sharedTextItem?.title
        val price = extractCurrentPrice(html) ?: sharedTextItem?.price
        val originalPrice = extractOriginalPrice(html) ?: sharedTextItem?.originalPrice
        val permalink = extractCanonicalUrl(html)
            ?: extractMetaUrl(html)
            ?: resolvedPage.finalUrl

        if (title == null && price == null) {
            throw ShopeeServiceError.InvalidResponse
        }

        ShopeeItem(
            title = title?.let(::decodeHtmlText),
            price = price,
            originalPrice = originalPrice,
            permalink = permalink
        )
    }

    private fun resolveFinalUrl(url: String): ResolvedPage {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        client.newCall(request).execute().use { response ->
            return ResolvedPage(
                finalUrl = response.request.url.toString(),
                body = response.body?.string().orEmpty()
            )
        }
    }

    private fun extractItemFromSharedText(text: String?, permalink: String): ShopeeItem? {
        if (text.isNullOrBlank()) return null

        val textWithoutUrl = text
            .replace(Regex("""https?://\S+"""), "")
            .trim()
        val title = extractTitleFromSharedText(textWithoutUrl)
        val price = extractPriceFromSharedText(textWithoutUrl)

        if (title == null && price == null) return null

        return ShopeeItem(
            title = title,
            price = price,
            originalPrice = null,
            permalink = permalink
        )
    }

    private fun extractTitleFromSharedText(text: String): String? {
        val patterns = listOf(
            """Confira\s+(.+?)\s+com\s+\d+%\s+de\s+desconto""",
            """Confira\s+(.+?)\s+Somente\s+R\$""",
            """Confira\s+(.+?)\s+Encontre\s+na\s+Shopee"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(text, pattern)?.let(::decodeHtmlText)?.trim()
        }
    }

    private fun extractPriceFromSharedText(text: String): BigDecimal? {
        val patterns = listOf(
            """Somente\s+R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)""",
            """R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)"""
        )

        return extractDecimal(text, patterns)
    }

    private fun extractTitle(html: String): String? {
        val patterns = listOf(
            """<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']og:title["']""",
            """<title>\s*([^<]+)\s*</title>""",
            """\"name\"\s*:\s*\"([^\"]+)\"""",
            """\"title\"\s*:\s*\"([^\"]+)\""""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)
                ?.let(::cleanTitle)
                ?.takeUnless(::isGenericShopeeTitle)
        }
    }

    private fun extractCurrentPrice(html: String): BigDecimal? {
        val patterns = listOf(
            """<meta\s+property=["']product:price:amount["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']product:price:amount["']""",
            """\"price\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"price_min\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"price\"\s*:\s*\"([^\"]+)\"""",
            """R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)"""
        )

        return extractDecimal(html, patterns)
    }

    private fun extractOriginalPrice(html: String): BigDecimal? {
        val patterns = listOf(
            """\"price_before_discount\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"price_max_before_discount\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"originalPrice\"\s*:\s*\"([^\"]+)\""""
        )

        return extractDecimal(html, patterns)
    }

    private fun extractDecimal(text: String, patterns: List<String>): BigDecimal? {
        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(text, pattern)?.let(::parseDecimal)
        }
    }

    private fun parseDecimal(rawValue: String): BigDecimal? {
        val cleanedValue = rawValue
            .replace("R$", "")
            .replace("&nbsp;", "")
            .replace(" ", "")
            .trim()

        val normalizedValue = when {
            cleanedValue.contains(",") -> cleanedValue
                .replace(".", "")
                .replace(",", ".")
            cleanedValue.substringAfterLast(".", "").length == 3 -> cleanedValue.replace(".", "")
            else -> cleanedValue
        }

        return normalizedValue.toBigDecimalOrNull()
    }

    private fun extractMetaUrl(html: String): String? {
        val patterns = listOf(
            """<meta\s+property=["']og:url["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']og:url["']"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.let(::makeUrl)
        }
    }

    private fun extractCanonicalUrl(html: String): String? {
        val patterns = listOf(
            """<link\s+rel=["']canonical["']\s+href=["']([^"']+)["']""",
            """<link\s+href=["']([^"']+)["']\s+rel=["']canonical["']"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.let(::makeUrl)
        }
    }

    private fun firstMatch(text: String, pattern: String): String? {
        return Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun makeUrl(rawValue: String): String? {
        val cleanedValue = decodeHtmlText(rawValue)
            .replace("\\u002F", "/")
            .replace("\\/", "/")

        return when {
            cleanedValue.startsWith("http") -> cleanedValue
            cleanedValue.startsWith("//") -> "https:$cleanedValue"
            else -> cleanedValue.takeIf { it.isNotBlank() }
        }
    }

    private fun normalizeHtml(html: String): String {
        return html
            .replace("""\\u002F""", "/")
            .replace("""\u002F""", "/")
            .replace("""\\/""", "/")
            .replace("""\""", "\"")
            .replace("&quot;", "\"")
    }

    private fun decodeHtmlText(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun cleanTitle(title: String): String {
        val decodedTitle = decodeHtmlText(title)
        val separators = listOf(" | Shopee Brasil", " | Shopee", " - Shopee Brasil", " - Shopee")
        val separatorIndex = separators
            .map { decodedTitle.lastIndexOf(it, ignoreCase = true) }
            .filter { it >= 0 }
            .minOrNull()

        return if (separatorIndex == null) decodedTitle.trim() else decodedTitle.substring(0, separatorIndex).trim()
    }

    private fun isGenericShopeeTitle(title: String): Boolean {
        val normalizedTitle = Normalizer.normalize(title.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        return "shopee brasil" in normalizedTitle && "ofertas incriveis" in normalizedTitle
    }

    private data class ResolvedPage(
        val finalUrl: String,
        val body: String
    )
}

sealed class ShopeeServiceError(message: String) : Exception(message) {
    data object InvalidResponse : ShopeeServiceError("Nao foi possivel buscar os dados do produto na Shopee.")
}
