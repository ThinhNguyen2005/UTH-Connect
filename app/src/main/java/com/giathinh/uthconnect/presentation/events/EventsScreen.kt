package com.giathinh.uthconnect.presentation.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.UserRole
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val eventDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))

@Composable
fun EventsScreen(
    state: EventsUiState,
    onRefresh: () -> Unit,
    onRegister: (String) -> Unit,
    onCancel: (String) -> Unit,
    onSave: (String?, EventDraft) -> Unit,
    onSetStatus: (String, EventStatus) -> Unit,
    onScan: () -> Unit,
) {
    var editor by remember { mutableStateOf<EventEditorTarget?>(null) }
    var cancelEvent by remember { mutableStateOf<Event?>(null) }
    LaunchedEffect(state.message) {
        if (state.message == "Đã lưu sự kiện") editor = null
    }
    val isOrganizer = state.role == UserRole.ADMIN || state.role == UserRole.ORGANIZER
    editor?.let { target ->
        EventEditorDialog(
            event = target.event,
            busy = state.busyEvent != null,
            error = state.error,
            onDismiss = { editor = null },
            onSave = { onSave(target.event?.id, it) },
        )
    }
    cancelEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { cancelEvent = null },
            title = { Text("Hủy sự kiện?") },
            text = { Text("Người tham gia sẽ không thể đăng ký hoặc điểm danh sự kiện ${event.title}.") },
            confirmButton = {
                Button(onClick = { onSetStatus(event.id, EventStatus.CANCELLED); cancelEvent = null }) {
                    Text("Xác nhận hủy")
                }
            },
            dismissButton = { OutlinedButton(onClick = { cancelEvent = null }) { Text("Quay lại") } },
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        item {
            Text("Sự kiện UTH", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Đăng ký sự kiện và quản lý vé tham gia", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                OutlinedButton(onClick = onRefresh) { Text("Làm mới") }
                if (isOrganizer) {
                    OutlinedButton(onClick = { editor = EventEditorTarget(null) }) { Text("Tạo sự kiện") }
                    Button(onClick = onScan, enabled = !state.checkingIn) {
                        Text(if (state.checkingIn) "Đang điểm danh…" else "Quét QR điểm danh")
                    }
                }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (!state.loading && state.events.isEmpty()) {
            item { Text("Chưa có sự kiện được đăng tải.") }
        }
        items(state.events, key = { it.id }) { event ->
            val registered = event.id in state.registeredIds
            EventCard(
                event = event, registered = registered,
                busy = state.busyEvent != null,
                canManage = isOrganizer && (state.role == UserRole.ADMIN || event.orgId == state.currentUserId),
                onRegister = { onRegister(event.id) }, onCancel = { onCancel(event.id) },
                onEdit = { editor = EventEditorTarget(event) },
                onSetStatus = { status ->
                    if (status == EventStatus.CANCELLED) cancelEvent = event
                    else onSetStatus(event.id, status)
                },
            )
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    registered: Boolean,
    busy: Boolean,
    canManage: Boolean,
    onRegister: () -> Unit,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    onSetStatus: (EventStatus) -> Unit,
) {
    val canRegister = event.status == EventStatus.PUBLISHED && event.startAt > System.currentTimeMillis() &&
        (event.capacity == 0 || event.registeredCount < event.capacity)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Text(event.title, style = MaterialTheme.typography.titleLarge)
            Text(event.orgName, style = MaterialTheme.typography.labelMedium)
            Text(eventDateFormatter.format(Instant.ofEpochMilli(event.startAt)))
            Text("${event.location} · ${event.campus}", style = MaterialTheme.typography.bodyMedium)
            Text("Trạng thái: ${event.status.name} · Đã đăng ký: ${event.registeredCount}${if (event.capacity > 0) "/${event.capacity}" else ""}")
            if (registered) {
                Text("Đã đăng ký · Vé có trong tab Vé của tôi", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = onCancel, enabled = !busy) { Text("Hủy đăng ký") }
            } else if (canRegister) {
                Button(onClick = onRegister, enabled = !busy) { Text("Đăng ký tham gia") }
            } else {
                Text("Sự kiện chưa mở hoặc đã đóng đăng ký", style = MaterialTheme.typography.bodySmall)
            }
            if (canManage && event.status !in setOf(EventStatus.CANCELLED, EventStatus.COMPLETED)) {
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    OutlinedButton(onClick = onEdit, enabled = !busy) { Text("Sửa") }
                    when (event.status) {
                        EventStatus.DRAFT -> Button(onClick = { onSetStatus(EventStatus.PUBLISHED) }, enabled = !busy) {
                            Text("Xuất bản")
                        }
                        EventStatus.PUBLISHED -> Button(onClick = { onSetStatus(EventStatus.HAPPENING) }, enabled = !busy) {
                            Text("Bắt đầu")
                        }
                        EventStatus.HAPPENING -> Button(onClick = { onSetStatus(EventStatus.COMPLETED) }, enabled = !busy) {
                            Text("Kết thúc")
                        }
                        else -> Unit
                    }
                }
                OutlinedButton(onClick = { onSetStatus(EventStatus.CANCELLED) }, enabled = !busy) {
                    Text("Hủy sự kiện")
                }
            }
        }
    }
}

private data class EventEditorTarget(val event: Event?)

private val editableDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
private fun EventEditorDialog(
    event: Event?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (EventDraft) -> Unit,
) {
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var description by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var campus by remember(event?.id) { mutableStateOf(event?.campus.orEmpty()) }
    val localZone = ZoneId.of("Asia/Ho_Chi_Minh")
    var startsAt by remember(event?.id) {
        mutableStateOf(editableDateFormatter.format(Instant.ofEpochMilli(event?.startAt ?: System.currentTimeMillis() + 86_400_000L).atZone(localZone)))
    }
    var endsAt by remember(event?.id) {
        mutableStateOf(editableDateFormatter.format(Instant.ofEpochMilli(event?.endAt ?: System.currentTimeMillis() + 90_000_000L).atZone(localZone)))
    }
    var capacity by remember(event?.id) { mutableStateOf((event?.capacity ?: 0).toString()) }
    var validationError by remember(event?.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (event == null) "Tạo bản nháp sự kiện" else "Chỉnh sửa sự kiện") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = UthDimens.Spacing.Xxl * 15).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên sự kiện") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Địa điểm") }, singleLine = true)
                OutlinedTextField(value = campus, onValueChange = { campus = it }, label = { Text("Cơ sở") }, singleLine = true)
                OutlinedTextField(value = startsAt, onValueChange = { startsAt = it }, label = { Text("Bắt đầu (dd/MM/yyyy HH:mm)") }, singleLine = true)
                OutlinedTextField(value = endsAt, onValueChange = { endsAt = it }, label = { Text("Kết thúc (dd/MM/yyyy HH:mm)") }, singleLine = true)
                OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Sức chứa (0 = không giới hạn)") }, singleLine = true)
                (validationError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val count = requireNotNull(capacity.toIntOrNull())
                    val start = LocalDateTime.parse(startsAt.trim(), editableDateFormatter).atZone(localZone).toInstant().toEpochMilli()
                    val end = LocalDateTime.parse(endsAt.trim(), editableDateFormatter).atZone(localZone).toInstant().toEpochMilli()
                    require(title.trim().length >= 3 && location.isNotBlank() && campus.isNotBlank() && end > start && count >= 0)
                    validationError = null
                    onSave(EventDraft(title.trim(), description.trim(), location.trim(), campus.trim(), start, end, count))
                } catch (_: Exception) {
                    validationError = "Kiểm tra tên, thời gian, địa điểm và sức chứa trước khi lưu."
                }
            }, enabled = !busy) { Text("Lưu") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Đóng") } },
    )
}
