package br.com.beltramitech.ofertazap.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.beltramitech.ofertazap.ui.components.AdFooterView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentView(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OfertaZap") })
        },
        bottomBar = {
            AdFooterView()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsSection(
                uiState = uiState,
                onHeadlineChange = viewModel::updateHeadline,
                onClear = viewModel::clearHeadline
            )

            HorizontalDivider()

            PreviewSection(lines = uiState.previewLines)
        }
    }
}

@Composable
private fun SettingsSection(
    uiState: SettingsUiState,
    onHeadlineChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Topo da mensagem",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.headline,
            onValueChange = onHeadlineChange,
            label = { Text("Ex: Oferta encontrada") },
            minLines = 1,
            maxLines = 2,
            singleLine = false
        )

        TextButton(
            onClick = onClear,
            enabled = uiState.isClearButtonEnabled,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text("Limpar texto")
        }

        Text(
            text = "Se ficar vazio, a primeira linha da mensagem não será exibida ao compartilhar.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PreviewSection(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Prévia",
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEach { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("🛒")) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = if (line.startsWith("🛒")) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
