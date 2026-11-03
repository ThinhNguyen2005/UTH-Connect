package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.Timetable
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.presentation.tickets.TicketQrScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedTicket by remember { mutableStateOf<Ticket?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFD))) {
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

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
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
                        Spacer(Modifier.height(16.dp))
                        UthCard {
                            UthEmptyState(
                                title = stringResource(R.string.no_events_for_date),
                                body = stringResource(R.string.timetable_memory),
                                icon = Icons.Filled.DateRange,
                            )
                        }
                    } else {
                        // Nhóm sự kiện theo ca (Sáng, Chiều, Tối)
                        val grouped = filteredEvents.groupBy { it.session.ifBlank { "Event" } }
                        grouped.forEach { (sessionName, sessionEvents) ->
                            SessionHeader(sessionName)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                sessionEvents.forEach { event ->
                                    CampusEventCard(
                                        event = event,
                                        now = uiState.now,
                                        onAction = onAction,
                                        onViewQr = { selectedTicket = it.toTicket() },
                                        onViewDetails = { selectedTicket = it.toTicket() },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    selectedTicket?.let { ticket ->
        Dialog(
            onDismissRequest = { selectedTicket = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            TicketQrScreen(
                ticket = ticket,
                studentName = "Nguyễn Minh Anh",
                secondsRemaining = 60,
                onBack = { selectedTicket = null },
            )
        }
    }
}

@Composable
private fun ScheduleHeader(
    onAction: (ScheduleAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF002D54),
                        Color(0xFF094074),
                        Color(0xFF0B4F8A),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SV",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.timetable_memory),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color(0xFFBFDBFE),
                    )
                    Text(
                        text = stringResource(R.string.timetable_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Nút HIỆN TẠI để nhảy về ngày hôm nay
                Surface(
                    onClick = { onAction(ScheduleAction.JumpToToday) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.height(36.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = Color(0xFFBFDBFE),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.timetable_jump_today),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }

                // Notification Bell
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .padding(1.dp)
                                .background(Color(0xFFF59E0B), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekPicker(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
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

    // Khi state.selectedDate thay đổi (ví dụ bấm HIỆN TẠI), cuộn pager đến đúng tuần
    LaunchedEffect(state.selectedDate) {
        val curMonday = state.selectedDate.with(DayOfWeek.MONDAY)
        val expectedOffset = ChronoUnit.WEEKS.between(baseMonday, curMonday).toInt()
        val targetPage = basePage + expectedOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDBE5F1)),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val weekMonday = baseMonday.plusWeeks((page - basePage).toLong())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(7) { offset ->
                    val day = weekMonday.plusDays(offset.toLong())
                    val selected = day == state.selectedDate
                    val hasClassOrEvent = state.timetable.sessions.any { it.startsAt.toLocalDate() == day } ||
                        state.events.any { it.startAt.toLocalDate() == day }

                    Surface(
                        onClick = { onAction(ScheduleAction.SelectDate(day)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) Color(0xFF003866) else Color(0xFFF9FAFB),
                        border = if (selected) {
                            BorderStroke(1.5.dp, Color(0xFF0B4F8A))
                        } else {
                            BorderStroke(1.dp, Color(0xFFE5E7EB))
                        },
                        shadowElevation = if (selected) 2.dp else 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = formatDayOfWeek(day.dayOfWeek),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (selected) Color(0xFFBFDBFE) else Color(0xFF6B7280),
                                maxLines = 1,
                            )
                            Text(
                                text = day.format(DateTimeFormatter.ofPattern("dd/MM")),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (selected) Color.White else Color(0xFF1F2937),
                                maxLines = 1,
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(
                                        when {
                                            selected -> Color.White
                                            hasClassOrEvent -> Color(0xFF0B4F8A)
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

@Composable
private fun CategoryFilterRow(
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit,
    eventsCount: Int,
) {
    val categories = listOf(
        "All Events ($eventsCount)",
        "Workshops & Seminars",
        "Culture & Sports",
        "Career & Networking",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEachIndexed { index, cat ->
            val isSelected = index == selectedCategory
            Surface(
                onClick = { onSelectCategory(index) },
                shape = RoundedCornerShape(9999.dp),
                color = if (isSelected) Color(0xFF003366) else Color.White,
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shadowElevation = if (isSelected) 1.dp else 0.dp,
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    color = if (isSelected) Color.White else Color(0xFF374151),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionHeader(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = Color(0xFF003366),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFE2E8F0),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun CampusEventCard(
    event: ScheduleEvent,
    now: LocalDateTime,
    onAction: (ScheduleAction) -> Unit,
    onViewQr: (ScheduleEvent) -> Unit,
    onViewDetails: (ScheduleEvent) -> Unit,
) {
    val isOngoing = now >= event.startAt && now < event.endAt
    val isRegistered = event.registration != EventRegistration.NOT_REGISTERED
    val isCheckedIn = event.registration == EventRegistration.CHECKED_IN

    // Viền xanh lá khi đang diễn ra, viền xanh dương khi đã đăng ký thành công
    val cardBorder = when {
        isOngoing -> BorderStroke(1.5.dp, Color(0xFF10B981))
        isRegistered || event.accent == EventAccent.PRIMARY -> BorderStroke(1.5.dp, Color(0xFF0B4F8A))
        event.accent == EventAccent.WARNING -> BorderStroke(1.dp, Color(0xFFFDE68A))
        else -> BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    val stripeColor = when {
        isOngoing -> Color(0xFF10B981)
        isRegistered || event.accent == EventAccent.PRIMARY -> Color(0xFF0B4F8A)
        event.accent == EventAccent.WARNING -> Color(0xFFF5A623)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = cardBorder,
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Thanh màu bên trái
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stripeColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Top Row: Khung giờ + Nhãn trạng thái
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val timeBg = when {
                        isOngoing -> Color(0xFF003366)
                        isRegistered || event.accent == EventAccent.PRIMARY -> Color(0xFF0B4F8A)
                        event.accent == EventAccent.WARNING -> Color(0xFFF5A623)
                        else -> Color(0xFF003366)
                    }
                    val timeTextColor = if (event.accent == EventAccent.WARNING && !isOngoing && !isRegistered) Color(0xFF1A1C1F) else Color.White

                    Surface(
                        color = timeBg,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "${event.startAt.format(timeFormatter())} - ${event.endAt.format(timeFormatter())}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = timeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }

                    // Nhãn Đang diễn ra viền xanh lá hoặc Sắp diễn ra
                    if (isOngoing) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.event_ongoing),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color(0xFF047857),
                                )
                            }
                        }
                    } else if (isRegistered || event.accent == EventAccent.PRIMARY) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        ) {
                            Text(
                                text = stringResource(R.string.event_upcoming),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        ) {
                            Text(
                                text = stringResource(R.string.event_upcoming),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Tiêu đề & Đơn vị tổ chức
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                        ),
                        color = Color(0xFF111827),
                    )
                    Text(
                        text = stringResource(R.string.event_organizer, event.organizer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Địa điểm & Điểm rèn luyện (+5 ĐRL)
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
                            tint = Color(0xFF003366),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF374151),
                        )
                    }

                    val displayPoints = event.trainingPoints.ifBlank { if (!isRegistered) "+5 ĐRL" else "" }
                    if (displayPoints.isNotBlank()) {
                        val (pointBg, pointBorder, pointText) = when {
                            isOngoing -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF003366))
                            isRegistered || event.accent == EventAccent.PRIMARY -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF003366))
                            event.accent == EventAccent.WARNING -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFB45309))
                            else -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF003366))
                        }
                        Surface(
                            color = pointBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, pointBorder),
                        ) {
                            Text(
                                text = displayPoints,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = pointText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Dòng hành động phía dưới
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        isCheckedIn -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.event_checked_in),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857),
                                )
                            }
                            OutlinedButton(
                                onClick = { onViewDetails(event) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF003366),
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.event_details),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Event details",
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                        isRegistered -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.event_registered),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1D4ED8),
                                )
                            }
                            OutlinedButton(
                                onClick = { onViewDetails(event) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF003366),
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.event_details),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.event_not_registered),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF6B7280),
                                )
                            }
                            Button(
                                onClick = { onAction(ScheduleAction.RegisterEvent(event.id)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF5A623),
                                    contentColor = Color(0xFF1A1C1F),
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.event_register),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleQrPreview(seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val count = 17
        val cell = size.minDimension / count
        var rnd = (seed.toLong() and 0x7FFFFFFFL) or 1L
        drawRect(Color.White)
        for (r in 0 until count) {
            for (c in 0 until count) {
                rnd = (rnd * 1103515245L + 12345L) and 0x7FFFFFFFL
                val isFinder = (r < 5 && c < 5) || (r < 5 && c >= count - 5) || (r >= count - 5 && c < 5)
                val filled = if (isFinder) {
                    val inOuter = (r == 0 || r == 4 || c == 0 || c == 4 ||
                        (r < 5 && (c == count - 1 || c == count - 5)) ||
                        (r >= count - 5 && (c == 0 || c == 4)))
                    val inCenter = (r in 1..3 && c in 1..3) ||
                        (r in 1..3 && c in (count - 4)..(count - 2)) ||
                        (r in (count - 4)..(count - 2) && c in 1..3)
                    inOuter || inCenter
                } else {
                    ((rnd ushr 16) and 1L) == 1L
                }
                if (filled) {
                    drawRect(
                        color = Color(0xFF003866),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

@Composable
private fun QrTicketDialog(
    event: ScheduleEvent,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF003366),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.event_qr_close))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "CHECKED IN SUCCESSFULLY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF047857),
                        )
                    }
                }

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF111827),
                )

                Text(
                    text = "${event.location}\n${event.startAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4B5563),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SimpleQrPreview(seed = event.id.hashCode(), modifier = Modifier.size(160.dp))
                    }
                }

                Text(
                    text = stringResource(R.string.event_qr_code, event.id.takeLast(4).uppercase()),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF003866),
                )
            }
        },
    )
}

@Composable
private fun EventDetailsDialog(
    event: ScheduleEvent,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.event_details_body,
                        event.organizer,
                        event.location,
                        "${event.startAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))} - ${event.endAt.format(timeFormatter())}",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (event.trainingPoints.isNotBlank()) {
                    Text(
                        text = "Training points: ${event.trainingPoints}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF003366),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.timetable_cancel))
            }
        },
    )
}

private fun formatDayOfWeek(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun timeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview(showBackground = true)
@Composable
private fun LightPreview() = UthPreview { ScheduleScreen(previewState(), {}) }

@Preview(showBackground = true)
@Composable
private fun DarkPreview() = UthPreview(darkTheme = true) { ScheduleScreen(previewState(), {}) }

private fun previewState(): ScheduleUiState {
    val now = LocalDateTime.of(2026, 11, 3, 9, 41)
    val date = now.toLocalDate()
    return ScheduleUiState(
        now = now,
        selectedDate = date,
        isLoading = false,
        events = listOf(
            ScheduleEvent(
                id = "preview-ai",
                title = "Workshop: AI & Tương lai ngành Giao thông thông minh",
                organizer = "Đoàn Thanh niên & Khoa CNTT",
                location = "Hội trường: A.101 (Cơ sở chính)",
                startAt = date.atTime(8, 0),
                endAt = date.atTime(11, 30),
                registration = EventRegistration.CHECKED_IN,
                accent = EventAccent.SUCCESS,
                trainingPoints = "+5 ĐRL",
                session = "Morning",
            ),
            ScheduleEvent(
                id = "preview-career",
                title = "Ngày hội Việc làm & Kết nối Doanh nghiệp Logistics 2026",
                organizer = "Trung tâm Hướng nghiệp & Tuyển dụng UTH",
                location = "Địa điểm: Sân trung tâm & Hội trường B",
                startAt = date.atTime(14, 0),
                endAt = date.atTime(17, 0),
                registration = EventRegistration.REGISTERED,
                accent = EventAccent.PRIMARY,
                trainingPoints = "+10 ĐRL",
                session = "Afternoon",
            ),
            ScheduleEvent(
                id = "preview-sports",
                title = "Hội thao Sinh viên UTH 2026",
                organizer = "CLB Thể Thao UTH",
                location = "Địa điểm: Sân vận động Cơ sở 2",
                startAt = date.atTime(18, 30),
                endAt = date.atTime(21, 0),
                registration = EventRegistration.NOT_REGISTERED,
                accent = EventAccent.WARNING,
                trainingPoints = "+5 ĐRL",
                session = "Evening",
            ),
        ),
        timetable = Timetable(),
    )
}

private fun ScheduleEvent.toTicket(): Ticket {
    val isSports = id.contains("sports") || title.contains("Hội thao")
    val isAi = id.contains("ai") || title.contains("AI")
    return Ticket(
        id = if (isSports) "ticket-sports" else if (isAi) "ticket-ai" else "ticket-$id",
        studentId = "2023040123",
        eventId = id,
        eventTitle = title,
        eventLocation = location.removePrefix("Địa điểm: ").removePrefix("Hội trường: "),
        eventStartsAt = "${startAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${startAt.format(DateTimeFormatter.ofPattern("HH:mm"))}",
        status = if (registration == EventRegistration.CHECKED_IN) TicketStatus.CHECKED_IN else TicketStatus.VALID,
        fallbackCode = if (isSports) "482 917" else if (isAi) "998 102" else "356 891",
        organizerName = organizer,
        seatLabel = if (isSports) "Ghế A-14" else "Khu vực tự do",
    )
}
