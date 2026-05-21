package br.com.beltramitech.ofertazap.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.beltramitech.ofertazap.data.MercadoLivreService
import br.com.beltramitech.ofertazap.data.OfertaZapSettings
import br.com.beltramitech.ofertazap.data.ShopeeService
import br.com.beltramitech.ofertazap.domain.OfferMessageBuilder
import br.com.beltramitech.ofertazap.domain.ProductPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareViewModel(
    private val settings: OfertaZapSettings,
    private val mercadoLivreService: MercadoLivreService = MercadoLivreService(),
    private val shopeeService: ShopeeService = ShopeeService(),
    private val messageBuilder: OfferMessageBuilder = OfferMessageBuilder()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState

    fun loadOffer(url: String, sharedText: String? = null) {
        _uiState.value = ShareUiState(isLoading = true, sharedUrl = url)

        viewModelScope.launch {
            val message = when (ProductPlatform.fromUrl(url)) {
                ProductPlatform.MercadoLivre -> makeMercadoLivreMessage(url)
                ProductPlatform.Shopee -> makeShopeeMessage(url, sharedText)
                ProductPlatform.Unsupported -> messageBuilder.buildFallbackMessage(
                    url = url,
                    platformName = ProductPlatform.Unsupported.displayName,
                    headline = settings.normalizedHeadline,
                    footer = settings.normalizedFooter
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    generatedMessage = message,
                    editableMessage = message
                )
            }
        }
    }

    fun updateEditableMessage(value: String) {
        _uiState.update { it.copy(editableMessage = value) }
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

    private suspend fun makeShopeeMessage(url: String, sharedText: String?): String {
        return try {
            val item = shopeeService.fetchItem(
                sharedUrl = url,
                sharedText = sharedText
            )
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
}

data class ShareUiState(
    val isLoading: Boolean = false,
    val sharedUrl: String? = null,
    val generatedMessage: String = "",
    val editableMessage: String = ""
)

class ShareViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShareViewModel(OfertaZapSettings(context)) as T
    }
}
