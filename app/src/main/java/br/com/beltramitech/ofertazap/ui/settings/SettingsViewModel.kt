package br.com.beltramitech.ofertazap.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.beltramitech.ofertazap.data.OfertaZapSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val settings: OfertaZapSettings
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
}

data class SettingsUiState(
    val headline: String,
    val footer: String
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
