package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.presentation.common.messageText
import com.giathinh.uthconnect.presentation.tickets.TicketQrScreen
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Tab "Thời khoá biểu": header thương hiệu gradient + lịch tuần vuốt ngang + danh sách
 * sự kiện trong ngày, nhóm theo ca.
 *
 * Toàn bộ màu/kích thước/bo góc lấy từ design token (`Color.kt`, `Dimens.kt`, `Theme.kt`),
 * trạng thái diễn đạt bằng [UthStatusBadge] nên màu không phải tín hiệu duy nhất
 * (`docs/UI-UX.md`). Gradient header dùng `headerGradientStart → headerGradientEnd` với
 * chữ `headerOnGradient` — cặp đã đo AAA ở cả hai scheme, xem [UthTheme.extendedColors].
 */
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val selectedTicket = uiState.selectedEvent?.toTicket(uiState.studentId.orEmpty())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = UthDimens.Spacing.Xxl),
        ) {
            ScheduleHeader(
                onAction = onAction,
            )
            WeekPicker(uiState, onAction)
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                eventsCount = uiState.events.count { it.startAt.toLocalDate() == uiState.selectedDate },
            )

            Column(modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg)) {
                uiState.registrationErrorCode?.let { errorCode ->
                    UthCard {
                        Text(
                            text = errorCode.messageText(uiState.registrationError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(UthDimens.Spacing.Lg))
                }
                if (uiState.registrationsLoadFailed) {
                    UthCard {
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                            Text(
                                text = stringResource(R.string.schedule_registrations_load_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.schedule_registrations_load_failed_body),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(UthDimens.Spacing.Lg))
                }
                if (uiState.isEventsLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = UthDimens.Spacing.Xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(UthDimens.Size.Icon))
                    }
                } else {
                    val rawEvents = uiState.events.filter { it.startAt.toLocalDate() == uiState.selectedDate }
                    val filteredEvents = when (selectedCategory) {
                        1 -> rawEvents.filter { it.title.contains("Workshop", ignoreCase = true) || it.title.contains("Hội thảo", ignoreCase = true) || it.title.contains("Seminar", ignoreCase = true) || it.title.contains("AI", ignoreCase = true) }
                        2 -> rawEvents.filter { it.title.contains("Hội thao", ignoreCase = true) || it.title.contains("Thể thao", ignoreCase = true) || it.title.contains("Sports", ignoreCase = true) || it.title.contains("Festival", ignoreCase = true) }
                        3 -> rawEvents.filter { it.title.contains("Doanh nghiệp", ignoreCase = true) || it.title.contains("Logistics", ignoreCase = true) || it.title.contains("Career", ignoreCase = true) || it.title.contains("Đoàn", ignoreCase = true) }
                        else -> rawEvents
                    }

                    if (filteredEvents.isEmpty()) {
                        Spacer(Modifier.height(UthDimens.Spacing.Lg))
                        UthCard {
                            UthEmptyState(
                                title = stringResource(
                                    if (uiState.eventsLoadFailed) R.string.schedule_events_load_failed
                                    else R.string.no_events_for_date,
                                ),
                                body = stringResource(
                                    if (uiState.eventsLoadFailed) R.string.schedule_events_load_failed_body
                                    else R.string.timetable_memory,
                                ),
                                icon = Icons.Filled.DateRange,
                            )
                        }
                    } else {
                        if (uiState.eventsLoadFailed) {
                            UthCard {
                                UthEmptyState(
                                    title = stringResource(R.string.schedule_events_load_failed),
                                    body = stringResource(R.string.schedule_events_load_failed_body),
                                    icon = Icons.Filled.DateRange,
                                )
                            }
                            Spacer(Modifier.height(UthDimens.Spacing.Lg))
                        }
                        // Nhóm sự kiện theo ca (Sáng, Chiều, Tối).
                        val grouped = filteredEvents.groupBy { it.session }
                        grouped.forEach { (session, sessionEvents) ->
                            val sessionName = when (session) {
                                SchedulePeriod.MORNING -> stringResource(R.string.event_session_morning)
                                SchedulePeriod.AFTERNOON -> stringResource(R.string.event_session_afternoon)
                                SchedulePeriod.EVENING -> stringResource(R.string.event_session_evening)
                                SchedulePeriod.ALL -> stringResource(R.string.schedule_session_default)
                            }
                            SessionHeader(sessionName)
                            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
                                sessionEvents.forEach { event ->
                                    CampusEventCard(
                                        event = event,
                                        now = uiState.now,
                                        registrationStatusAvailable =
                                            !uiState.registrationsLoadFailed && !uiState.isRegistrationsLoading,
                                        onAction = onAction,
                                        onViewDetails = { onAction(ScheduleAction.OpenEventDetails(it.id)) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(UthDimens.Spacing.Sm))
                        }
                    }
                }
            }
        }
    }

    selectedTicket?.let { ticket ->
        Dialog(
            onDismissRequest = { onAction(ScheduleAction.CloseEventDetails) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            TicketQrScreen(
                ticket = ticket,
                studentName = uiState.studentName,
                secondsRemaining = 60,
                qrToken = uiState.ticketQrToken,
                qrError = if (uiState.ticketQrFailed) stringResource(R.string.tickets_qr_issue_failed) else null,
                onBack = { onAction(ScheduleAction.CloseEventDetails) },
                onCopy = {},
            )
        }
    }
}

/**
 * Header thương hiệu: gradient Navy hai điểm dừng (token `headerGradient*`), monogram "SV",
 * nhãn eyebrow và hai nút hành động mờ như kính — cùng công thức nền/viền với header
 * trang Chủ (`HomeScreen`), chỉ khác nền ở đây là gradient.
 */
@Composable
private fun ScheduleHeader(
    onAction: (ScheduleAction) -> Unit,
) {
    val ext = UthTheme.extendedColors
    val colors = MaterialTheme.colorScheme
    val onHeader = ext.headerOnGradient

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ext.headerGradientStart,
                        ext.headerGradientEnd,
                    ),
                ),
            )
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                Surface(
                    modifier = Modifier.size(UthDimens.Size.Avatar),
                    shape = MaterialTheme.shapes.small,
                    color = onHeader.copy(alpha = UthDimens.Opacity.HeaderActionFill),
                    border = BorderStroke(
                        UthDimens.Size.Stroke,
                        onHeader.copy(alpha = UthDimens.Opacity.HeaderActionBorder),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.schedule_avatar_initials),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onHeader,
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.timetable_memory),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onHeader.copy(alpha = UthDimens.Opacity.HeaderMetadata),
                    )
                    Text(
                        text = stringResource(R.string.timetable_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onHeader,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                // Nút HÔM NAY để nhảy về ngày hiện tại.
                Surface(
                    onClick = { onAction(ScheduleAction.JumpToToday) },
                    modifier = Modifier.height(UthDimens.Size.HeaderAction),
                    shape = MaterialTheme.shapes.small,
                    color = onHeader.copy(alpha = UthDimens.Opacity.HeaderActionFill),
                    border = BorderStroke(
                        UthDimens.Size.Stroke,
                        onHeader.copy(alpha = UthDimens.Opacity.HeaderActionBorder),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = onHeader.copy(alpha = UthDimens.Opacity.HeaderMetadata),
                            modifier = Modifier.size(UthDimens.Size.IconBadge),
                        )
                        Text(
                            text = stringResource(R.string.timetable_jump_today),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = onHeader,
                        )
                    }
                }

                // Chuông thông báo: chấm vàng chỉ là nền, tình trạng đọc được qua
                Surface(
                    modifier = Modifier.size(UthDimens.Size.HeaderAction),
                    shape = MaterialTheme.shapes.small,
                    color = onHeader.copy(alpha = UthDimens.Opacity.HeaderActionFill),
                    border = BorderStroke(
                        UthDimens.Size.Stroke,
                        onHeader.copy(alpha = UthDimens.Opacity.HeaderActionBorder),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.schedule_notifications),
                            tint = onHeader,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(UthDimens.Spacing.Xxs)
                                .size(UthDimens.Size.IndicatorDot)
                                .background(colors.secondary, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lịch tuần: ô đang chọn = `primary`/`onPrimary`, ô thường = `surfaceSubtle` + viền
 * `outlineVariant`; mỗi ô cao tối thiểu [UthDimens.Size.MinTouchTarget] vì vuốt/tap lịch
 * là thao tác chính của màn này.
 */
@Composable
private fun WeekPicker(
    state: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = UthTheme.extendedColors
    val baseMonday = remember(state.now) { state.now.toLocalDate().with(DayOfWeek.MONDAY) }
    val basePage = 1000
    val pageCount = 2000

    val selectedMonday = state.selectedDate.with(DayOfWeek.MONDAY)
    val initialWeekOffset = remember(baseMonday) {
        ChronoUnit.WEEKS.between(baseMonday, selectedMonday).toInt()
    }
    val pagerState = rememberPagerState(
        initialPage = basePage + initialWeekOffset,
        pageCount = { pageCount },
    )

    // Khi người dùng vuốt sang trái/phải, đồng bộ ngày chọn sang tuần mới
    LaunchedEffect(pagerState.currentPage) {
        val pageMonday = baseMonday.plusWeeks((pagerState.currentPage - basePage).toLong())
        val pageSunday = pageMonday.plusDays(6)
        if (state.selectedDate.isBefore(pageMonday) || state.selectedDate.isAfter(pageSunday)) {
            val currentDayOfWeek = state.selectedDate.dayOfWeek
            val targetDate = pageMonday.with(currentDayOfWeek)
            onAction(ScheduleAction.SelectDate(targetDate))
        }
    }

    // Khi state.selectedDate thay đổi (ví dụ bấm HÔM NAY), cuộn pager đến đúng tuần
    LaunchedEffect(state.selectedDate) {
        val curMonday = state.selectedDate.with(DayOfWeek.MONDAY)
        val expectedOffset = ChronoUnit.WEEKS.between(baseMonday, curMonday).toInt()
        val targetPage = basePage + expectedOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Surface(
        color = colors.surface,
        border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val weekMonday = baseMonday.plusWeeks((page - basePage).toLong())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Md),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                repeat(7) { offset ->
                    val day = weekMonday.plusDays(offset.toLong())
                    val selected = day == state.selectedDate
                    val hasClassOrEvent = state.timetable.sessions.any { it.startsAt.toLocalDate() == day } ||
                        state.events.any { it.startAt.toLocalDate() == day }

                    Surface(
                        onClick = { onAction(ScheduleAction.SelectDate(day)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = UthDimens.Size.MinTouchTarget),
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) colors.primary else ext.surfaceSubtle,
                        border = if (selected) {
                            BorderStroke(UthDimens.Size.StrokeFocused, colors.primary)
                        } else {
                            BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant)
                        },
                        shadowElevation = if (selected) UthDimens.Elevation.Card else UthDimens.Elevation.None,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = UthDimens.Spacing.Sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
                        ) {
                            Text(
                                text = dayOfWeekLabel(day.dayOfWeek),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) {
                                    colors.onPrimary.copy(alpha = UthDimens.Opacity.HeaderMetadata)
                                } else {
                                    ext.textSecondary
                                },
                                maxLines = 1,
                            )
                            Text(
                                text = day.format(DateTimeFormatter.ofPattern("dd/MM")),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) colors.onPrimary else colors.onSurface,
                                maxLines = 1,
                            )
                            // Chấm "ngày này có lịch": chỉ là gợi ý, ngày đã là nhãn đọc được.
                            Box(
                                modifier = Modifier
                                    .size(UthDimens.Size.IndicatorDot)
                                    .background(
                                        when {
                                            selected -> colors.onPrimary
                                            hasClassOrEvent -> colors.primary
                                            else -> Color.Transparent
                                        },
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Chips lọc nhóm sự kiện: chip đang chọn = `primary`/`onPrimary`, chip thường = `surface` + viền `outlineVariant`. */
@Composable
private fun CategoryFilterRow(
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit,
    eventsCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    val categories = listOf(
        stringResource(R.string.schedule_category_all, eventsCount),
        stringResource(R.string.schedule_category_workshops),
        stringResource(R.string.schedule_category_culture),
        stringResource(R.string.schedule_category_career),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        categories.forEachIndexed { index, category ->
            val isSelected = index == selectedCategory
            Surface(
                onClick = { onSelectCategory(index) },
                shape = UthDimens.Radius.PillShape,
                color = if (isSelected) colors.primary else colors.surface,
                border = if (isSelected) null else BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant),
                shadowElevation = if (isSelected) UthDimens.Elevation.Card else UthDimens.Elevation.None,
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Md,
                        vertical = UthDimens.Spacing.Xs,
                    ),
                )
            }
        }
    }
}

/** Nhãn ca (Sáng/Chiều/Tối): chữ `onPrimary` trên nền `primary` + kẻ ngang `outlineVariant`. */
@Composable
private fun SessionHeader(name: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UthDimens.Spacing.Md, bottom = UthDimens.Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        Surface(
            color = colors.primary,
            contentColor = colors.onPrimary,
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.outlineVariant,
            thickness = UthDimens.Size.Stroke,
        )
    }
}

/**
 * Thẻ sự kiện: dải màu trái + khối nội dung cân bằng chiều cao hàng qua
 * [IntrinsicSize.Min] nên hai nhãn dài không làm thẻ lệch đáy (idiom giống thẻ vé ở
 * `TicketsScreen`). Trạng thái dùng [UthStatusBadge] — nền/chữ đã đo tương phản theo tone,
 * không tự phối tay.
 */
@Composable
private fun CampusEventCard(
    event: ScheduleEvent,
    now: LocalDateTime,
    registrationStatusAvailable: Boolean,
    onAction: (ScheduleAction) -> Unit,
    onViewDetails: (ScheduleEvent) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = UthTheme.extendedColors

    val isCancelled = event.status == EventStatus.CANCELLED
    val isEnded = now >= event.endAt
    val isInactive = isCancelled || isEnded
    val canOpenTicket = event.canOpenTicketAt(now)
    val isOngoing = !isCancelled && now >= event.startAt && now < event.endAt
    val isRegistered = event.registration != EventRegistration.NOT_REGISTERED
    val isCheckedIn = event.registration == EventRegistration.CHECKED_IN
    val isPrimaryAccent = isRegistered || event.accent == EventAccent.PRIMARY
    val isWarningAccent = event.accent == EventAccent.WARNING
    val isNeutral = !isOngoing && !isPrimaryAccent && !isWarningAccent

    val borderColor = when {
        isInactive -> colors.outlineVariant
        isOngoing -> ext.success
        isPrimaryAccent -> colors.primary
        isWarningAccent -> ext.warning
        else -> colors.outlineVariant
    }
    // Dải màu: Gold chỉ làm NỀN (chữ Navy trên Gold 6.2:1); trạng thái trung tính giữ Navy.
    val stripeColor = when {
        isInactive -> colors.outlineVariant
        isOngoing -> ext.success
        isWarningAccent -> colors.secondary
        else -> colors.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isInactive) colors.surfaceVariant else colors.surface,
        border = BorderStroke(
            width = if (isNeutral) UthDimens.Size.Stroke else UthDimens.Size.StrokeFocused,
            color = borderColor,
        ),
        shadowElevation = UthDimens.Elevation.Card,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(UthDimens.Size.AccentStripe)
                    .fillMaxHeight()
                    .background(stripeColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(UthDimens.Spacing.Lg),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                // Hàng trên: khung giờ + nhãn trạng thái
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Nền vàng Gold luôn đi chữ Navy (`onSecondary`); chữ trắng trên Gold chỉ 2.0:1.
                    val timeOnGold = isWarningAccent && !isOngoing && !isRegistered
                    Surface(
                        color = when {
                            isInactive -> colors.surface
                            timeOnGold -> colors.secondary
                            else -> colors.primary
                        },
                        contentColor = when {
                            isInactive -> colors.onSurfaceVariant
                            timeOnGold -> colors.onSecondary
                            else -> colors.onPrimary
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.timetable_time_range,
                                event.startAt.format(timeFormatter()),
                                event.endAt.format(timeFormatter()),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(
                                horizontal = UthDimens.Spacing.Sm,
                                vertical = UthDimens.Spacing.Xxs,
                            ),
                        )
                    }

                    when {
                        isCancelled -> UthStatusBadge(
                            text = stringResource(R.string.tickets_status_cancelled),
                            tone = BadgeTone.Neutral,
                        )

                        isEnded -> UthStatusBadge(
                            text = stringResource(R.string.event_ended),
                            tone = BadgeTone.Neutral,
                        )

                        isOngoing -> UthStatusBadge(
                            text = stringResource(R.string.event_ongoing),
                            tone = BadgeTone.Success,
                        )

                        isPrimaryAccent -> UthStatusBadge(
                            text = stringResource(R.string.event_upcoming),
                            tone = BadgeTone.Info,
                        )

                        else -> UthStatusBadge(
                            text = stringResource(R.string.event_upcoming),
                            tone = BadgeTone.Warning,
                        )
                    }
                }

                // Tiêu đề & đơn vị tổ chức
                Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isInactive) colors.onSurfaceVariant else colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.event_organizer, event.organizer),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInactive) colors.onSurfaceVariant else ext.textSecondary,
                    )
                }

                HorizontalDivider(color = colors.outlineVariant, thickness = UthDimens.Size.Stroke)

                // Địa điểm & điểm rèn luyện
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = if (isInactive) colors.onSurfaceVariant else colors.primary,
                            modifier = Modifier.size(UthDimens.Size.IconCompact),
                        )
                        Spacer(Modifier.width(UthDimens.Spacing.Xs))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    val displayPoints = event.trainingPoints
                    if (displayPoints.isNotBlank()) {
                        UthStatusBadge(
                            text = displayPoints,
                            tone = if (isInactive) BadgeTone.Neutral else if (isWarningAccent && !isRegistered) BadgeTone.Warning else BadgeTone.Info,
                            icon = null,
                        )
                    }
                }

                // Hàng trạng thái đăng ký + hành động
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        isCancelled -> RegistrationHint(
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.tickets_status_cancelled),
                            tint = colors.onSurfaceVariant,
                        )

                        isEnded -> {
                            RegistrationHint(
                                icon = Icons.Filled.DateRange,
                                label = stringResource(R.string.event_ended),
                                tint = colors.onSurfaceVariant,
                            )
                            if (canOpenTicket) EventDetailsButton(onClick = { onViewDetails(event) })
                        }

                        isOngoing && !isRegistered -> RegistrationHint(
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.event_registration_closed),
                            tint = colors.onSurfaceVariant,
                        )

                        isCheckedIn -> {
                            RegistrationHint(
                                icon = Icons.Filled.Check,
                                label = stringResource(R.string.event_checked_in),
                                tint = ext.onSuccessContainer,
                            )
                            if (canOpenTicket) EventDetailsButton(onClick = { onViewDetails(event) })
                        }

                        isRegistered -> {
                            RegistrationHint(
                                icon = Icons.Filled.CheckCircle,
                                label = stringResource(R.string.event_registered),
                                tint = ext.onInfoContainer,
                            )
                            if (canOpenTicket) EventDetailsButton(onClick = { onViewDetails(event) })
                        }

                        else -> {
                            RegistrationHint(
                                icon = Icons.Filled.DateRange,
                                label = stringResource(R.string.event_not_registered),
                                tint = ext.textSecondary,
                            )
                            Button(
                                onClick = { onAction(ScheduleAction.RegisterEvent(event.id)) },
                                enabled = event.status == EventStatus.PUBLISHED && registrationStatusAvailable,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.secondary,
                                    contentColor = colors.onSecondary,
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = UthDimens.Spacing.Lg,
                                    vertical = UthDimens.Spacing.Xs,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.event_register),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Trạng thái đăng ký ở chân thẻ: icon và chữ cùng màu `onContainer` đã đo AA. */
@Composable
private fun RegistrationHint(
    icon: ImageVector,
    label: String,
    tint: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(UthDimens.Size.IconBadge),
        )
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

/** Nút "Chi tiết sự kiện": viền `outline`, chữ `primary`; icon trang trí vì nhãn đã đủ nghĩa. */
@Composable
private fun EventDetailsButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(UthDimens.Size.Stroke, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        contentPadding = PaddingValues(
            horizontal = UthDimens.Spacing.Md,
            vertical = UthDimens.Spacing.Xs,
        ),
    ) {
        Text(
            text = stringResource(R.string.event_details),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(UthDimens.Size.IconBadge),
        )
    }
}

/** Tên viết tắt thứ trong tuần — lấy từ `strings.xml` để dịch sang tiếng Anh được. */
@Composable
private fun dayOfWeekLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.schedule_day_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.schedule_day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.schedule_day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.schedule_day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.schedule_day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.schedule_day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.schedule_day_sun)
}

/** Creates the 24-hour time formatter used for event cards. */
private fun timeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Renders an empty schedule in the light preview theme. */
@Preview(showBackground = true)
@Composable
private fun LightPreview() = UthPreview {
    val now = LocalDateTime.now()
    ScheduleScreen(
        uiState = ScheduleUiState(
            now = now,
            selectedDate = now.toLocalDate(),
            isLoading = false,
            isEventsLoading = false,
            isRegistrationsLoading = false,
        ),
        onAction = {},
    )
}

/** Renders an empty schedule in the dark preview theme. */
@Preview(showBackground = true)
@Composable
private fun DarkPreview() = UthPreview(darkTheme = true) {
    val now = LocalDateTime.now()
    ScheduleScreen(
        uiState = ScheduleUiState(
            now = now,
            selectedDate = now.toLocalDate(),
            isLoading = false,
            isEventsLoading = false,
            isRegistrationsLoading = false,
        ),
        onAction = {},
    )
}

/** Converts a registered schedule event into the ticket view model. */
private fun ScheduleEvent.toTicket(studentId: String): Ticket {
    return Ticket(
        id = "ticket-$id",
        studentId = studentId,
        eventId = id,
        eventTitle = title,
        eventLocation = location.removePrefix("Địa điểm: ").removePrefix("Hội trường: "),
        eventStartsAt = "${startAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${startAt.format(DateTimeFormatter.ofPattern("HH:mm"))}",
        status = if (registration == EventRegistration.CHECKED_IN) TicketStatus.CHECKED_IN else TicketStatus.VALID,
        fallbackCode = "",
        organizerName = organizer,
        seatLabel = null,
    )
}
