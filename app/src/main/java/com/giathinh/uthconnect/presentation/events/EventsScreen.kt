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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.presentation.common.messageText
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    onOpenAttendance: (String) -> Unit,
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
            // Ưu tiên câu dịch theo mã server; chuỗi thô chỉ còn dùng khi mã không nhận diện được.
            (state.errorCode?.messageText(state.error) ?: state.error)
                ?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                onOpenAttendance = { onOpenAttendance(event.id) },
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
    onOpenAttendance: () -> Unit,
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
            if (canManage) {
                OutlinedButton(onClick = onOpenAttendance) {
                    Text(stringResource(R.string.attendance_open))
                }
            }
        }
    }
}

private data class EventEditorTarget(val event: Event?)

// Các hàm quy đổi thời gian của editor để `internal` cho unit test chống tái diễn lỗi mặc định sai ngày.
internal val editableDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Độ rộng cửa sổ điểm danh quanh sự kiện, khớp các khoảng trừ trong `check_in_ticket_qr`:
 * quét sớm nhất `start_at - 30 phút`, muộn nhất `end_at + 30 phút`.
 */
private const val CHECKIN_EARLY_MINUTES = 30L
private const val CHECKIN_LATE_MINUTES = 30L

/** Sự kiện mới phải bắt đầu tối thiểu chừng này giây nữa, khớp điều kiện `p_start_at > now()`. */
private const val MIN_LEAD_SECONDS = 60L

/**
 * Mặc định của sự kiện mới: bắt đầu sau 60 phút (làm tròn lên bội số 5 phút), kéo dài 120 phút.
 * Mặc định cũ là "bây giờ + 24 giờ" nên người tạo chỉ sửa giờ mà quên sửa ngày: sự kiện rơi
 * sang hôm sau, vé vẫn hợp lệ nhưng mọi lần quét đều nhận CHECKIN_NOT_OPEN không lời giải thích.
 */
internal fun defaultEventWindow(zone: ZoneId): Pair<LocalDateTime, LocalDateTime> {
    val base = LocalDateTime.now(zone).plusMinutes(60).truncatedTo(ChronoUnit.MINUTES)
    val start = base.plusMinutes((5L - base.minute % 5L) % 5L)
    return start to start.plusMinutes(120)
}

internal fun parseEditableDateTime(text: String): LocalDateTime? =
    runCatching { LocalDateTime.parse(text, editableDateFormatter) }.getOrNull()

internal fun editableDateTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime().format(editableDateFormatter)

@Composable
private fun EventEditorDialog(
    event: Event?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (EventDraft) -> Unit,
) {
    val localZone = ZoneId.of("Asia/Ho_Chi_Minh")
    val errTitle = stringResource(R.string.events_editor_err_title)
    val errPlace = stringResource(R.string.events_editor_err_place)
    val errFormat = stringResource(R.string.events_editor_err_format)
    val errOrder = stringResource(R.string.events_editor_err_order)
    val errFuture = stringResource(R.string.events_editor_err_future)
    val errCapacity = stringResource(R.string.events_editor_err_capacity)
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var description by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var campus by remember(event?.id) { mutableStateOf(event?.campus.orEmpty()) }
    val defaults = remember(event?.id) { defaultEventWindow(localZone) }
    var startsAt by remember(event?.id) {
        val fromEvent = event?.let { editableDateTime(it.startAt, localZone) }
        mutableStateOf(fromEvent ?: defaults.first.format(editableDateFormatter))
    }
    var endsAt by remember(event?.id) {
        val fromEvent = event?.let { editableDateTime(it.endAt, localZone) }
        mutableStateOf(fromEvent ?: defaults.second.format(editableDateFormatter))
    }
    var capacity by remember(event?.id) { mutableStateOf((event?.capacity ?: 0).toString()) }
    var validationError by remember(event?.id) { mutableStateOf<String?>(null) }
    val parsedStart = parseEditableDateTime(startsAt.trim())
    val parsedEnd = parseEditableDateTime(endsAt.trim())
    val openFrom = parsedStart?.minusMinutes(CHECKIN_EARLY_MINUTES)
    val closeAfter = parsedEnd?.plusMinutes(CHECKIN_LATE_MINUTES)
    val showWindow = openFrom != null && closeAfter != null && closeAfter.isAfter(openFrom)
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
                if (showWindow) {
                    val now = LocalDateTime.now(localZone)
                    val minutesToOpen = ChronoUnit.MINUTES.between(now, openFrom)
                    val minutesFromClose = ChronoUnit.MINUTES.between(closeAfter, now)
                    val windowHint = when {
                        minutesToOpen > 0 -> stringResource(
                            R.string.events_editor_window_future,
                            stringResource(R.string.events_editor_left, minutesToOpen / 60, minutesToOpen % 60),
                        )
                        minutesFromClose > 0 -> stringResource(R.string.events_editor_window_past)
                        else -> stringResource(R.string.events_editor_window_open)
                    }
                    HorizontalDivider()
                    Text(
                        text = stringResource(
                            R.string.events_editor_window,
                            openFrom.format(editableDateFormatter),
                            closeAfter.format(editableDateFormatter),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = windowHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (minutesToOpen > 0 || minutesFromClose > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                if (event == null) {
                    Text(
                        stringResource(R.string.events_editor_draft_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                (validationError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val count = capacity.trim().toIntOrNull()
                val start = parseEditableDateTime(startsAt.trim())
                val end = parseEditableDateTime(endsAt.trim())
                val startsTooSoon = start?.atZone(localZone)?.toInstant()
                    ?.isBefore(Instant.now().plusSeconds(MIN_LEAD_SECONDS)) == true
                validationError = when {
                    title.trim().length < 3 -> errTitle
                    location.isBlank() || campus.isBlank() -> errPlace
                    start == null || end == null -> errFormat
                    !end.isAfter(start) -> errOrder
                    count == null || count < 0 -> errCapacity
                    event == null && startsTooSoon -> errFuture
                    else -> null
                }
                if (validationError == null && start != null && end != null && count != null) {
                    onSave(
                        EventDraft(
                            title.trim(), description.trim(), location.trim(), campus.trim(),
                            start.atZone(localZone).toInstant().toEpochMilli(),
                            end.atZone(localZone).toInstant().toEpochMilli(),
                            count,
                        ),
                    )
                }
            }, enabled = !busy) { Text("Lưu") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Đóng") } },
    )
}
