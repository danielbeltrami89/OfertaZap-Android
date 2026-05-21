package br.com.beltramitech.ofertazap.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.beltramitech.ofertazap.data.AffiliateService
import br.com.beltramitech.ofertazap.data.MercadoLivreService
import br.com.beltramitech.ofertazap.data.OfertaZapSettings
import br.com.beltramitech.ofertazap.data.ShopeeService
import br.com.beltramitech.ofertazap.domain.OfferMessageBuilder
import br.com.beltramitech.ofertazap.domain.ProductPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: OfertaZapSettings,
    private val mercadoLivreService: MercadoLivreService = MercadoLivreService(),
    private val shopeeService: ShopeeService = ShopeeService(),
    private val affiliateService: AffiliateService = AffiliateService(),
    private val messageBuilder: OfferMessageBuilder = OfferMessageBuilder()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            headline = settings.messageHeadline,
            footer = settings.messageFooter
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun updateHeadline(value: String) {
        settings.messageHeadline = value
        _uiState.update { it.copy(headline = value) }
    }

    fun clearHeadline() {
        updateHeadline("")
    }

    fun updateFooter(value: String) {
        settings.messageFooter = value
        _uiState.update { it.copy(footer = value) }
    }

    fun clearFooter() {
        updateFooter("")
    }

    fun updateAffiliateLink(value: String) {
        _uiState.update { it.copy(affiliateLink = value, affiliateError = null) }
    }

    fun clearAffiliateLink() {
        _uiState.update {
            it.copy(
                affiliateLink = "",
                affiliateMessage = "",
                affiliateError = null
            )
        }
    }

    fun prepareAffiliateMessage() {
        val url = _uiState.value.affiliateLink.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(affiliateMessage = "", affiliateError = "Cole um link da oferta.") }
            return
        }

        _uiState.update { it.copy(isAffiliateLoading = true, affiliateError = null) }

        viewModelScope.launch {
            val platform = ProductPlatform.fromUrl(url)
            val message = when (platform) {
                ProductPlatform.MercadoLivre -> makeMercadoLivreMessage(url)
                ProductPlatform.Shopee -> makeShopeeMessage(url)
                ProductPlatform.Amazon,
                ProductPlatform.MagazineLuiza -> makeAffiliateMessage(url, platform)
                ProductPlatform.Unsupported -> messageBuilder.buildFallbackMessage(
                    url = url,
                    platformName = platform.displayName,
                    headline = settings.normalizedHeadline,
                    footer = settings.normalizedFooter
                )
            }

            _uiState.update {
                it.copy(
                    isAffiliateLoading = false,
                    affiliateMessage = message,
                    affiliateError = if (platform == ProductPlatform.Unsupported) {
                        "Não reconheci a loja, mas deixei uma mensagem pronta para revisar."
                    } else {
                        null
                    }
                )
            }
        }
    }

    private suspend fun makeMercadoLivreMessage(url: String): String {
        return try {
            val item = mercadoLivreService.fetchItem(url)
            messageBuilder.buildMessage(
                item = item,
                shareUrl = url,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        } catch (_: Exception) {
            messageBuilder.buildFallbackMessage(
                url = url,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        }
    }

    private suspend fun makeShopeeMessage(url: String): String {
        return try {
            val item = shopeeService.fetchItem(sharedUrl = url)
            messageBuilder.buildMessage(
                item = item,
                shareUrl = url,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        } catch (_: Exception) {
            messageBuilder.buildFallbackMessage(
                url = url,
                platformName = ProductPlatform.Shopee.displayName,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        }
    }

    private suspend fun makeAffiliateMessage(url: String, platform: ProductPlatform): String {
        return try {
            val item = affiliateService.fetchItem(
                sharedUrl = url,
                platform = platform
            )
            messageBuilder.buildMessage(
                item = item,
                platform = platform,
                shareUrl = url,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        } catch (_: Exception) {
            messageBuilder.buildFallbackMessage(
                url = url,
                platformName = platform.displayName,
                headline = settings.normalizedHeadline,
                footer = settings.normalizedFooter
            )
        }
    }
}

data class SettingsUiState(
    val headline: String,
    val footer: String,
    val affiliateLink: String = "",
    val affiliateMessage: String = "",
    val affiliateError: String? = null,
    val isAffiliateLoading: Boolean = false
) {
    val previewLines: List<String>
        get() {
            val trimmedHeadline = headline.trim()
            val trimmedFooter = footer.trim()
            return buildList {
                if (trimmedHeadline.isNotEmpty()) {
                    add("🔥 $trimmedHeadline 🔥")
                    add("")
                }
                add("✨ NOME DO PRODUTO")
                add("✅ por: R$ 48,00")
                add("🚚 Confira na [plataforma]")
                add("🛒 https://link-da-oferta")
                if (trimmedFooter.isNotEmpty()) {
                    add("")
                    add("_${trimmedFooter}_")
                }
            }
        }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(OfertaZapSettings(context)) as T
    }
}
