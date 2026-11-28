package com.giathinh.uthconnect.presentation.notificationshub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthContentSkeleton
import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserNotificationsScreen(
    state: UserNotificationsUiState,
    onAction: (UserNotificationsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_notifications_title)) },
                actions = {
                    IconButton(
                        onClick = { onAction(UserNotificationsAction.Refresh) },
                        enabled = !state.isRefreshing,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.notification_refresh))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(UserNotificationsAction.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.isLoading && state.notifications.isEmpty() -> NotificationHubLoading()

                state.errorMessage != null && state.notifications.isEmpty() -> NotificationHubError(
                    message = state.errorMessage,
                    onRetry = { onAction(UserNotificationsAction.Refresh) },
                )

                state.notifications.isEmpty() -> NotificationHubEmpty()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.errorMessage != null) {
                        item {
                            NotificationHubInlineError(
                                message = state.errorMessage,
                                onRetry = { onAction(UserNotificationsAction.Refresh) },
                            )
                        }
                    }
                    items(state.notifications, key = NotificationItem::id) { item ->
                        NotificationHubCard(item = item, onClick = { onAction(UserNotificationsAction.Open(item)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationHubLoading() {
    UthContentSkeleton()
}

@Composable
private fun NotificationHubEmpty() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.user_notifications_empty), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.user_notifications_empty_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationHubError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        IconButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.notification_retry))
        }
    }
}

@Composable
private fun NotificationHubInlineError(message: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRetry),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun NotificationHubCard(item: NotificationItem, onClick: () -> Unit) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var contentOverflows by remember(item.id) { mutableStateOf(false) }
    val color = if (item.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = color,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (item.isRead) 0.dp else 1.dp,
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!item.isRead) {
                Box(
                    Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            } else {
                Spacer(Modifier.size(8.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { contentOverflows = it.hasVisualOverflow },
                )
                if (item.type == UserNotificationType.SYSTEM && (contentOverflows || expanded)) {
                    TextButton(
                        onClick = {
                            if (!expanded && !item.isRead) onClick()
                            expanded = !expanded
                        },
                    ) {
                        Text(
                            stringResource(
                                if (expanded) {
                                    R.string.user_notification_show_less
                                } else {
                                    R.string.user_notification_show_more
                                },
                            ),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        typeLabel(item.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        Instant.ofEpochMilli(item.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun typeLabel(type: UserNotificationType): String = when (type) {
    UserNotificationType.HOUSING_APPROVED -> stringResource(R.string.user_notification_housing_approved)

    UserNotificationType.HOUSING_REJECTED -> stringResource(R.string.user_notification_housing_rejected)

    UserNotificationType.EVENT_REMINDER -> stringResource(R.string.user_notification_event_reminder)

    UserNotificationType.INCIDENT_SUBMITTED,
    UserNotificationType.INCIDENT_STATUS_UPDATED,
    UserNotificationType.INCIDENT_RESOLVED,
    UserNotificationType.INCIDENT_REJECTED,
    -> stringResource(R.string.user_notification_incident_update)

    UserNotificationType.SYSTEM -> stringResource(R.string.user_notification_system)
}
