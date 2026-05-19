package br.com.beltramitech.ofertazap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.com.beltramitech.ofertazap.ui.settings.ContentView
import br.com.beltramitech.ofertazap.ui.settings.SettingsViewModel
import br.com.beltramitech.ofertazap.ui.settings.SettingsViewModelFactory
import br.com.beltramitech.ofertazap.ui.theme.OfertaZapTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfertaZapTheme {
                ContentView(viewModel = viewModel)
            }
        }
    }
}
