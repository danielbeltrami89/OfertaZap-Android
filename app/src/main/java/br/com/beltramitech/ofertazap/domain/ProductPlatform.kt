package br.com.beltramitech.ofertazap.domain

enum class ProductPlatform(val displayName: String) {
    MercadoLivre("Mercado Livre"),
    Shopee("Shopee"),
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
                else -> Unsupported
            }
        }
    }
}
