package dev.beltramitech.ofertazap.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import dev.beltramitech.ofertazap.BuildConfig
import dev.beltramitech.ofertazap.data.AnalyticsTracker
import dev.beltramitech.ofertazap.ui.components.AdFooterView
import dev.beltramitech.ofertazap.ui.share.ValueCheckWarning

@Composable
fun ContentView(
    viewModel: SettingsViewModel,
    analyticsTracker: AnalyticsTracker,
    onShare: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            AdFooterView(
                screenName = AnalyticsTracker.SCREEN_HOME,
                analyticsTracker = analyticsTracker
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppTitle()

            MessageTemplateCard(
                uiState = uiState,
                onHeadlineChange = viewModel::updateHeadline,
                onHeadlineClear = {
                    analyticsTracker.logClick("home_clear_headline", AnalyticsTracker.SCREEN_HOME)
                    viewModel.clearHeadline()
                },
                onFooterChange = viewModel::updateFooter,
                onFooterClear = {
                    analyticsTracker.logClick("home_clear_footer", AnalyticsTracker.SCREEN_HOME)
                    viewModel.clearFooter()
                },
                onDone = { focusManager.clearFocus() }
            )

            AffiliateShareSection(
                uiState = uiState,
                onLinkChange = viewModel::updateAffiliateLink,
                onLinkClear = {
                    analyticsTracker.logClick("home_clear_affiliate_link", AnalyticsTracker.SCREEN_HOME)
                    viewModel.clearAffiliateLink()
                },
                onPaste = {
                    analyticsTracker.logClick("home_paste_affiliate_link", AnalyticsTracker.SCREEN_HOME)
                    viewModel.updateAffiliateLink(clipboardManager.getText()?.text.orEmpty())
                },
                onPrepare = {
                    analyticsTracker.logClick("home_prepare_affiliate_message", AnalyticsTracker.SCREEN_HOME)
                    focusManager.clearFocus()
                    viewModel.prepareAffiliateMessage()
                },
                onShare = onShare,
                onCopy = {
                    analyticsTracker.logClick("home_copy_affiliate_message", AnalyticsTracker.SCREEN_HOME)
                    clipboardManager.setText(AnnotatedString(it))
                },
                onDone = { focusManager.clearFocus() }
            )
        }
    }
}

@Composable
private fun AffiliateShareSection(
    uiState: SettingsUiState,
    onLinkChange: (String) -> Unit,
    onLinkClear: () -> Unit,
    onPaste: () -> Unit,
    onPrepare: () -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDone: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compartilhar por link",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.affiliateLink,
                onValueChange = onLinkChange,
                label = { Text("Cole o link da oferta") },
                minLines = 1,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (uiState.affiliateLink.trim().isNotEmpty()) {
                        IconButton(onClick = onLinkClear) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPaste,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Colar")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPrepare,
                    enabled = !uiState.isAffiliateLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isAffiliateLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Preparar")
                    }
                }
            }

            uiState.affiliateError?.let { SettingsTip(it) }

            if (uiState.affiliateMessage.isNotBlank()) {
                MessagePreview(lines = uiState.affiliateMessage.lines())
                ValueCheckWarning()

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onShare(uiState.affiliateMessage) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar mensagem")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onCopy(uiState.affiliateMessage) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Copiar texto")
                }
            }

            SettingsTip("Amazon e Magazine Luiza podem ser colados aqui.")
        }
    }
}

@Composable
private fun AppTitle() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "OfertaZap",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            modifier = Modifier.padding(bottom = 6.dp),
            text = "v${BuildConfig.VERSION_NAME}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun MessageTemplateCard(
    uiState: SettingsUiState,
    onHeadlineChange: (String) -> Unit,
    onHeadlineClear: () -> Unit,
    onFooterChange: (String) -> Unit,
    onFooterClear: () -> Unit,
    onDone: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Mensagem padrao",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Toque nas linhas para editar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InlineTemplateField(
                        value = uiState.headline,
                        placeholder = "Oferta encontrada",
                        prefix = "🔥 ",
                        suffix = " 🔥",
                        onValueChange = onHeadlineChange,
                        onClear = onHeadlineClear,
                        onDone = onDone
                    )

                    TemplatePreviewLine("✨ NOME DO PRODUTO")
                    TemplatePreviewLine("✅ por: R$ 48,00")
                    TemplatePreviewLine("🚚 Confira na [plataforma]")
                    TemplatePreviewLine(
                        text = "🛒 https://link-da-oferta",
                        subdued = true
                    )

                    InlineTemplateField(
                        value = uiState.footer,
                        placeholder = "Os valores podem se alterar",
                        prefix = "_",
                        suffix = "_",
                        onValueChange = onFooterChange,
                        onClear = onFooterClear,
                        onDone = onDone
                    )
                }
            }

            SettingsTip("Campos vazios nao aparecem na mensagem compartilhada.")
        }
    }
}

@Composable
private fun SettingsTip(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InlineTemplateField(
    value: String,
    placeholder: String,
    prefix: String,
    suffix: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = prefix + placeholder + suffix,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        minLines = 1,
        maxLines = 2,
        singleLine = false,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(14.dp),
        leadingIcon = {
            Text(
                text = prefix,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (value.trim().isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar campo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TemplatePreviewLine(
    text: String,
    subdued: Boolean = false
) {
    Text(
        text = text,
        color = if (subdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = if (subdued) 1 else Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun MessagePreview(lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
