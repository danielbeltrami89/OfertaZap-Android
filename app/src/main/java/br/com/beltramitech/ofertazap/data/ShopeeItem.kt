package dev.beltramitech.ofertazap.data

import java.math.BigDecimal

data class ShopeeItem(
    val title: String?,
    val price: BigDecimal?,
    val originalPrice: BigDecimal?,
    val permalink: String
)
