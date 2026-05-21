package br.com.beltramitech.ofertazap.data

import br.com.beltramitech.ofertazap.domain.ProductPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.text.Normalizer

class AffiliateService(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchItem(
        sharedUrl: String,
        platform: ProductPlatform,
        sharedText: String? = null
    ): AffiliateItem = withContext(Dispatchers.IO) {
        val sharedTextItem = extractItemFromSharedText(sharedText, sharedUrl)
        val resolvedPage = resolveFinalUrl(sharedUrl)
        val html = normalizeHtml(resolvedPage.body)
        val title = extractTitle(html, platform) ?: sharedTextItem?.title
        val price = extractCurrentPrice(html) ?: sharedTextItem?.price
        val originalPrice = extractOriginalPrice(html) ?: sharedTextItem?.originalPrice
        val permalink = extractCanonicalUrl(html)
            ?: extractMetaUrl(html)
            ?: resolvedPage.finalUrl

        if (title == null && price == null) {
            throw AffiliateServiceError.InvalidResponse(platform.displayName)
        }

        AffiliateItem(
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

    private fun extractItemFromSharedText(text: String?, permalink: String): AffiliateItem? {
        if (text.isNullOrBlank()) return null

        val textWithoutUrl = text
            .replace(Regex("""https?://\S+"""), "")
            .trim()
        val price = extractDecimal(
            textWithoutUrl,
            listOf(
                """R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)""",
                """([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2})"""
            )
        )
        val title = extractTitleFromSharedText(textWithoutUrl, price)

        if (title == null && price == null) return null

        return AffiliateItem(
            title = title,
            price = price,
            originalPrice = null,
            permalink = permalink
        )
    }

    private fun extractTitleFromSharedText(text: String, price: BigDecimal?): String? {
        val patterns = listOf(
            """Confira\s+(.+?)\s+com\s+\d+%\s+de\s+desconto""",
            """Confira\s+(.+?)\s+Somente\s+R\$""",
            """Confira\s+(.+?)\s+por\s+R\$""",
            """(.+?)\s+por\s+R\$""",
            """(.+?)\s+R\$"""
        )

        patterns.forEach { pattern ->
            firstMatch(text, pattern)?.let(::decodeHtmlText)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                return it
            }
        }

        return if (price == null) text.takeIf { it.isNotBlank() } else null
    }

    private fun extractTitle(html: String, platform: ProductPlatform): String? {
        val patterns = listOf(
            """<span[^>]+id=["']productTitle["'][^>]*>\s*([^<]+)\s*</span>""",
            """<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']og:title["']""",
            """<meta\s+name=["']title["']\s+content=["']([^"']+)["']""",
            """\"name\"\s*:\s*\"([^\"]+)\"""",
            """\"title\"\s*:\s*\"([^\"]+)\"""",
            """<title>\s*([^<]+)\s*</title>"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)
                ?.let { cleanTitle(it, platform) }
                ?.takeUnless { isGenericTitle(it, platform) }
        }
    }

    private fun extractCurrentPrice(html: String): BigDecimal? {
        val patterns = listOf(
            """<meta\s+property=["']product:price:amount["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']product:price:amount["']""",
            """<meta\s+name=["']twitter:data1["']\s+content=["']R\$\s*([^"']+)["']""",
            """\"price\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?""",
            """\"priceToPay\"\s*:\s*\{[^}]*\"amount\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?""",
            """\"lowPrice\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?""",
            """R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)"""
        )

        return extractDecimal(html, patterns)
    }

    private fun extractOriginalPrice(html: String): BigDecimal? {
        val patterns = listOf(
            """\"listPrice\"\s*:\s*\{[^}]*\"amount\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?""",
            """\"wasPrice\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?""",
            """\"originalPrice\"\s*:\s*\"?([0-9]+(?:\.[0-9]+)?)\"?"""
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
            .replace("\u00a0", "")
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

    private fun cleanTitle(title: String, platform: ProductPlatform): String {
        val decodedTitle = decodeHtmlText(title)
        val separators = when (platform) {
            ProductPlatform.Amazon -> listOf(
                ": Amazon.com.br",
                "| Amazon.com.br",
                "- Amazon.com.br",
                ": Amazon",
                "| Amazon"
            )
            ProductPlatform.MagazineLuiza -> listOf(
                "| Magazine Luiza",
                "- Magazine Luiza",
                "| Magalu",
                "- Magalu"
            )
            else -> emptyList()
        }
        val separatorIndex = separators
            .map { decodedTitle.lastIndexOf(it, ignoreCase = true) }
            .filter { it >= 0 }
            .minOrNull()

        return if (separatorIndex == null) decodedTitle.trim() else decodedTitle.substring(0, separatorIndex).trim()
    }

    private fun isGenericTitle(title: String, platform: ProductPlatform): Boolean {
        val normalizedTitle = Normalizer.normalize(title.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        return when (platform) {
            ProductPlatform.Amazon -> "amazon.com.br" in normalizedTitle ||
                "desculpe" in normalizedTitle ||
                "sorry" in normalizedTitle
            ProductPlatform.MagazineLuiza -> ("magazine luiza" in normalizedTitle || "magalu" in normalizedTitle) &&
                "ofertas" in normalizedTitle
            else -> false
        }
    }

    private data class ResolvedPage(
        val finalUrl: String,
        val body: String
    )
}

sealed class AffiliateServiceError(message: String) : Exception(message) {
    class InvalidResponse(platformName: String) : AffiliateServiceError(
        "Nao foi possivel buscar os dados do produto em $platformName."
    )
}
