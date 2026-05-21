package br.com.beltramitech.ofertazap.domain

enum class ProductPlatform(val displayName: String) {
    MercadoLivre("Mercado Livre"),
    Shopee("Shopee"),
    Amazon("Amazon"),
    MagazineLuiza("Magazine Luiza"),
    Unsupported("loja");

    companion object {
        fun fromUrl(url: String): ProductPlatform {
            val normalizedUrl = url.lowercase()

            return when {
                "mercadolivre.com.br" in normalizedUrl ||
                    "mercadolibre.com" in normalizedUrl ||
                    "meli.la" in normalizedUrl -> MercadoLivre
                "shopee.com.br" in normalizedUrl ||
                    "s.shopee.com.br" in normalizedUrl ||
                    "shope.ee" in normalizedUrl -> Shopee
                "amzn.to" in normalizedUrl ||
                    "a.co" in normalizedUrl ||
                    "amazon.com" in normalizedUrl -> Amazon
                "magazineluiza.onelink.me" in normalizedUrl ||
                    "magazineluiza.com.br" in normalizedUrl ||
                    "magalu.com" in normalizedUrl -> MagazineLuiza
                else -> Unsupported
            }
        }
    }
}
