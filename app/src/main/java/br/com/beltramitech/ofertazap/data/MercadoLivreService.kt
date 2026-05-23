package dev.beltramitech.ofertazap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal

class MercadoLivreService(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchItem(sharedUrl: String): MercadoLivreItem = withContext(Dispatchers.IO) {
        val resolvedPage = resolveFinalUrl(sharedUrl)
        val htmlFallbackItem = extractItemFromHtml(resolvedPage.body, resolvedPage.finalUrl)
        val itemId = extractItemId(resolvedPage.finalUrl) ?: htmlFallbackItem?.id

        if (itemId == null) {
            return@withContext htmlFallbackItem ?: throw MercadoLivreServiceError.InvalidItemUrl
        }

        val request = Request.Builder()
            .url("https://api.mercadolibre.com/items/$itemId")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext htmlFallbackItem ?: throw MercadoLivreServiceError.InvalidApiResponse
            }

            val body = response.body?.string()
                ?: return@withContext htmlFallbackItem
                ?: throw MercadoLivreServiceError.InvalidApiResponse

            try {
                return@withContext parseApiItem(body)
            } catch (_: Exception) {
                return@withContext htmlFallbackItem ?: throw MercadoLivreServiceError.InvalidApiResponse
            }
        }
    }

    private fun resolveFinalUrl(url: String): ResolvedPage {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            return ResolvedPage(
                finalUrl = response.request.url.toString(),
                body = response.body?.string().orEmpty()
            )
        }
    }

    private fun parseApiItem(json: String): MercadoLivreItem {
        val objectValue = JSONObject(json)
        return MercadoLivreItem(
            id = objectValue.getString("id"),
            title = objectValue.getString("title"),
            price = objectValue.optBigDecimal("price"),
            originalPrice = objectValue.optBigDecimal("original_price"),
            permalink = objectValue.getString("permalink")
        )
    }

    private fun extractItemFromHtml(rawHtml: String, finalUrl: String): MercadoLivreItem? {
        val html = normalizeHtml(rawHtml)
        val id = extractProductIdFromHtml(html) ?: extractImageIdFromHtml(html) ?: "MERCADOLIVRE_SOCIAL"
        val hasProductId = id.startsWith("MLB")
        val productHtml = if (hasProductId) htmlSegment(id, html) ?: html else html
        val title = extractTitle(productHtml) ?: extractTitle(html) ?: return null
        val price = extractCurrentPrice(productHtml) ?: extractCurrentPrice(html)
        val originalPrice = extractOriginalPrice(productHtml) ?: extractOriginalPrice(html)
        val permalink = extractProductUrlFromHtml(html, id)
            ?: extractMetaUrl(html)
            ?: extractCanonicalUrl(html)
            ?: finalUrl

        return MercadoLivreItem(
            id = id,
            title = decodeHtmlText(title),
            price = price,
            originalPrice = originalPrice,
            permalink = permalink
        )
    }

    private fun extractItemId(url: String): String? {
        return Regex("MLB-?\\d+", RegexOption.IGNORE_CASE)
            .find(url)
            ?.value
            ?.uppercase()
            ?.replace("-", "")
    }

    private fun htmlSegment(id: String, html: String): String? {
        val dashedId = id.replace("MLB", "MLB-")
        val index = listOf(dashedId, id)
            .map { html.indexOf(it) }
            .firstOrNull { it >= 0 } ?: return null
        val start = (index - 1_500).coerceAtLeast(0)
        val end = (index + 8_000).coerceAtMost(html.length)
        return html.substring(start, end)
    }

    private fun extractTitle(text: String): String? {
        val patterns = listOf(
            """\"title\"\s*:\s*\{\"text\"\s*:\s*\"([^\"]+)\"""",
            """<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']og:title["']""",
            """\"name\"\s*:\s*\"([^\"]+)\"""",
            """\"headline\"\s*:\s*\"([^\"]+)\""""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(text, pattern)?.let(::cleanTitle)
        }
    }

    private fun extractCurrentPrice(text: String): BigDecimal? {
        val patterns = listOf(
            """\"current_price\"\s*:\s*\{[^}]*\"value\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"price\"\s*:\s*\{[^}]*\"amount\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """<meta\s+property=["']product:price:amount["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']product:price:amount["']""",
            """<meta\s+itemprop=["']price["']\s+content=["']([^"']+)["']""",
            """<span[^>]+class=["'][^"']*andes-money-amount__fraction[^"']*["'][^>]*>\s*([^<]+)""",
            """\"aria-label\"\s*:\s*\"[^\"]*?([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)\s*reais"""
        )

        return extractDecimal(text, patterns)
    }

    private fun extractOriginalPrice(text: String): BigDecimal? {
        val patterns = listOf(
            """\"previous_price\"\s*:\s*\{[^}]*\"value\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"original_price\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """\"regular_amount\"\s*:\s*\{[^}]*\"value\"\s*:\s*([0-9]+(?:\.[0-9]+)?)""",
            """<span[^>]+class=["'][^"']*andes-money-amount--previous[^"']*["'][^>]*>.*?<span[^>]+class=["'][^"']*andes-money-amount__fraction[^"']*["'][^>]*>\s*([^<]+)"""
        )

        return extractDecimal(text, patterns)
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

    private fun extractProductIdFromHtml(html: String): String? {
        val patterns = listOf(
            """produto\.mercadolivre\.com\.br/(MLB-\d+)""",
            """\"url\":\"(?:https?:)?//?produto\.mercadolivre\.com\.br/(MLB-\d+)""",
            """<link\s+rel=["']canonical["']\s+href=["'][^"']*/(MLB-\d+)[^"']*["']""",
            """<meta\s+property=["']og:url["']\s+content=["'][^"']*/(MLB-\d+)[^"']*["']""",
            """\"product_id\"\s*:\s*\"(MLB\d{8,})\"""",
            """\"metadata\"\s*:\s*\{\"id\"\s*:\s*\"(MLB\d{8,})\"""",
            """\"item_id\"\s*:\s*\"(MLB\d{8,})\"""",
            """\"id\"\s*:\s*\"(MLB\d{8,})\""""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.uppercase()?.replace("-", "")
        }
    }

    private fun extractImageIdFromHtml(html: String): String? {
        val patterns = listOf(
            """mlstatic\.com/[^"']*-(MLB\d{8,})[-_]""",
            """/(MLB\d{8,})[-_][^"']*\.(?:jpg|jpeg|png|webp)"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.uppercase()
        }
    }

    private fun extractProductUrlFromHtml(html: String, id: String): String? {
        val dashedId = id.replace("MLB", "MLB-")
        val patterns = listOf(
            """((?:https?:)?//?produto\.mercadolivre\.com\.br/$dashedId[^"]*)""",
            """((?:https?:)?//?produto\.mercadolivre\.com\.br/MLB-\d+[^"]*)"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.let(::makeMercadoLivreUrl)
        }
    }

    private fun extractMetaUrl(html: String): String? {
        val patterns = listOf(
            """<meta\s+property=["']og:url["']\s+content=["']([^"']+)["']""",
            """<meta\s+content=["']([^"']+)["']\s+property=["']og:url["']"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.let(::makeMercadoLivreUrl)
        }
    }

    private fun extractCanonicalUrl(html: String): String? {
        val patterns = listOf(
            """<link\s+rel=["']canonical["']\s+href=["']([^"']+)["']""",
            """<link\s+href=["']([^"']+)["']\s+rel=["']canonical["']"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            firstMatch(html, pattern)?.let(::makeMercadoLivreUrl)
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

    private fun firstMatch(text: String, pattern: String): String? {
        return Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun makeMercadoLivreUrl(rawValue: String): String {
        val cleanedValue = rawValue
            .replace("\\u002F", "/")
            .replace("\\/", "/")
            .replace("&amp;", "&")

        return if (cleanedValue.startsWith("http")) cleanedValue else "https://$cleanedValue"
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
        val separators = listOf(" | MercadoLivre", " | Mercado Livre", " - MercadoLivre", " - Mercado Livre")
        val separatorIndex = separators
            .map { decodedTitle.lastIndexOf(it, ignoreCase = true) }
            .filter { it >= 0 }
            .minOrNull()

        return if (separatorIndex == null) decodedTitle.trim() else decodedTitle.substring(0, separatorIndex).trim()
    }

    private data class ResolvedPage(
        val finalUrl: String,
        val body: String
    )
}

sealed class MercadoLivreServiceError(message: String) : Exception(message) {
    data object InvalidItemUrl : MercadoLivreServiceError("Nao foi possivel encontrar o codigo MLB no link informado.")
    data object InvalidApiResponse : MercadoLivreServiceError("A resposta do Mercado Livre nao foi valida.")
}

private fun JSONObject.optBigDecimal(name: String): BigDecimal? {
    if (isNull(name)) return null
    return optString(name).toBigDecimalOrNull()
}
