package br.com.beltramitech.ofertazap.data

import java.math.BigDecimal

data class AffiliateItem(
    val title: String?,
    val price: BigDecimal?,
    val originalPrice: BigDecimal?,
    val permalink: String
)
