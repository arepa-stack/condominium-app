package com.example.condominio.ui.screens.profile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** "You have unsaved changes" prompt shown before leaving a form that was edited (E-R4). */
@Composable
internal fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(Res.string.unsaved_changes_title)) },
        text = { Text(stringResource(Res.string.unsaved_changes_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(Res.string.discard), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(Res.string.keep_editing)) }
        }
    )
}
