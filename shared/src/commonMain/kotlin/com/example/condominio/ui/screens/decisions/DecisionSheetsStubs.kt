package com.example.condominio.ui.screens.decisions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.QuoteDto
import com.example.condominio.data.utils.PlatformFileReader
import com.example.condominio.data.utils.rememberDocumentPickerLauncher
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// ---------------------------------------------------------------------------
// UploadQuoteSheet — Phase 4c real implementation.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadQuoteSheet(
    isSubmitting: Boolean,
    error: UiText?,
    onDismiss: () -> Unit,
    onSubmit: (providerName: String, amount: Double, notes: String?, fileUri: String, mimeType: String) -> Unit
) {
    val fileReader = koinInject<PlatformFileReader>()

    var provider by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<String?>(null) }
    var fileMime by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileSizeBytes by remember { mutableStateOf(0) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val errReadFailed = stringResource(Res.string.decisions_upload_file_read_failed)
    val errFileTooLarge = stringResource(Res.string.decisions_upload_file_too_large)

    val pickFile = rememberDocumentPickerLauncher { uri, mime ->
        if (uri == null) return@rememberDocumentPickerLauncher
        fileError = null
        val bytes = fileReader.readBytes(uri)
        if (bytes == null) {
            fileError = errReadFailed
            return@rememberDocumentPickerLauncher
        }
        if (bytes.size > 5 * 1024 * 1024) {
            fileError = errFileTooLarge
            fileUri = null
            fileMime = null
            fileName = null
            fileSizeBytes = 0
            return@rememberDocumentPickerLauncher
        }
        fileUri = uri
        fileMime = mime ?: "application/octet-stream"
        fileName = fileReader.getFileName(uri)
        fileSizeBytes = bytes.size
    }

    val amount = amountText.toDoubleOrNull()
    val canSubmit =
        provider.isNotBlank() &&
        provider.length >= 2 &&
        amount != null &&
        amount > 0 &&
        fileUri != null &&
        fileMime != null &&
        !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.decisions_upload_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text(stringResource(Res.string.decisions_upload_provider_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(Res.string.decisions_upload_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(Res.string.decisions_upload_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // File picker section
            if (fileUri == null) {
                Button(
                    onClick = pickFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.decisions_upload_select_file_btn))
                }
            } else {
                val sizeMb = fileSizeBytes / (1024.0 * 1024.0)
                val sizeLabel = "%.2f MB".format(sizeMb)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${fileName ?: "archivo"} · $sizeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = pickFile) {
                        Text(stringResource(Res.string.decisions_upload_change_file_btn))
                    }
                }
            }

            if (fileError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fileError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = {
                        onSubmit(
                            provider.trim(),
                            amount!!,
                            notes.takeIf { it.isNotBlank() },
                            fileUri!!,
                            fileMime!!
                        )
                    },
                    enabled = canSubmit
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(Res.string.decisions_upload_submit_btn))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// VoteSheet — Phase 4c real implementation.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteSheet(
    quotes: List<QuoteDto>,
    isSubmitting: Boolean,
    error: UiText?,
    onDismiss: () -> Unit,
    onConfirm: (quoteId: String) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    val activeQuotes = quotes.filter { it.deletedAt == null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.decisions_vote_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Warning card
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.decisions_vote_final_warning),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                activeQuotes.forEach { quote ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedId == quote.id,
                            onClick = { selectedId = quote.id }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quote.providerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$%.2f".format(quote.amount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = { onConfirm(selectedId!!) },
                    enabled = selectedId != null && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(Res.string.decisions_vote_confirm_btn))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
