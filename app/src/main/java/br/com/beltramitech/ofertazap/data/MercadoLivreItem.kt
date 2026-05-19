package br.com.beltramitech.ofertazap.data

import java.math.BigDecimal

data class MercadoLivreItem(
    val id: String,
    val title: String,
    val price: BigDecimal?,
    val originalPrice: BigDecimal?,
    val permalink: String
)
