package br.com.beltramitech.ofertazap.domain

import android.util.Patterns

class SharedUrlExtractor {
    fun firstUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        if (matcher.find()) {
            val match = matcher.group()
            return if (match.startsWith("http")) match else "https://$match"
        }

        return text.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
