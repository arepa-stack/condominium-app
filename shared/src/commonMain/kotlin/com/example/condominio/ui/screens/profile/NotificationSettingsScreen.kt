package com.example.condominio.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.formWidth
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit
) {
    var paymentReminders by remember { mutableStateOf(true) }
    var paymentConfirmations by remember { mutableStateOf(true) }
    var announcements by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.notification_settings),
                onBackClick = onBackClick
            )
        },
        containerColor = screenBackground(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        if (isWide()) {
            WideNotificationSettings(
                paddingValues = paddingValues,
                items = listOf(
                    NotificationPref(
                        title = stringResource(Res.string.payment_reminders_title),
                        description = stringResource(Res.string.payment_reminders_desc),
                        checked = paymentReminders,
                        onCheckedChange = { paymentReminders = it }
                    ),
                    NotificationPref(
                        title = stringResource(Res.string.payment_confirmations_title),
                        description = stringResource(Res.string.payment_confirmations_desc),
                        checked = paymentConfirmations,
                        onCheckedChange = { paymentConfirmations = it }
                    ),
                    NotificationPref(
                        title = stringResource(Res.string.announcements_title),
                        description = stringResource(Res.string.announcements_desc),
                        checked = announcements,
                        onCheckedChange = { announcements = it }
                    )
                )
            )
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.notification_settings_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Payment Reminders
            NotificationToggleCard(
                title = stringResource(Res.string.payment_reminders_title),
                description = stringResource(Res.string.payment_reminders_desc),
                checked = paymentReminders,
                onCheckedChange = {
                    paymentReminders = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Confirmations
            NotificationToggleCard(
                title = stringResource(Res.string.payment_confirmations_title),
                description = stringResource(Res.string.payment_confirmations_desc),
                checked = paymentConfirmations,
                onCheckedChange = {
                    paymentConfirmations = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Announcements
            NotificationToggleCard(
                title = stringResource(Res.string.announcements_title),
                description = stringResource(Res.string.announcements_desc),
                checked = announcements,
                onCheckedChange = {
                    announcements = it
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class NotificationPref(
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

/** Wide layouts: web settings page, one card with switch rows separated by dividers. */
@Composable
private fun WideNotificationSettings(
    paddingValues: PaddingValues,
    items: List<NotificationPref>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = pagePadding())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.notification_settings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.formWidth().padding(bottom = 24.dp)
        )
        WebCard(modifier = Modifier.formWidth(), contentPadding = 0.dp) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { item.onCheckedChange(!item.checked) }
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Switch(
                        checked = item.checked,
                        onCheckedChange = item.onCheckedChange,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NotificationToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
