package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.forLanguageTag("vi-VN"))

@Composable
fun ScheduleScreen(

    modifier: Modifier = Modifier,
) {
    // Chỉ cần 3 biến để điều khiển màn hình. Đổi biến thì Compose tự cập nhật giao diện.
    // Lưu ngày bằng chuỗi để rememberSaveable giữ được khi xoay điện thoại.
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showWeek by rememberSaveable { mutableStateOf(false) }
    var showExams by rememberSaveable { mutableStateOf(false) }
    val selectedDate = LocalDate.parse(selectedDateText)
    // dayOfWeek.value: thứ Hai = 1, Chủ nhật = 7. Lùi về đầu tuần rồi tạo đủ 7 ngày.
    val monday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days = (0L..6L).map { monday.plusDays(it) }
    // Toàn bộ màn hình cuộn được, kể cả khi màn hình nhỏ hoặc tăng cỡ chữ.
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        item {
            Text(stringResource(R.string.tab_schedule), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = UthDimens.Spacing.Xl))
            Text(stringResource(R.string.schedule_sample_note), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                FilterChip(selected = !showExams, onClick = { showExams = false },
                    label = { Text(stringResource(R.string.schedule_classes)) })
                FilterChip(selected = showExams, onClick = { showExams = true },
                    label = { Text(stringResource(R.string.schedule_exams)) })
            }
        }
        if (showExams) {
            // Lịch thi hiển thị tất cả ngày thi, không bị lọc theo tuần học đang chọn.
            items(sampleExams.sortedBy { it.date }, key = { it.id }) { exam ->
                UthCard {
                    Text(exam.course.courseName, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.schedule_class_code_format, exam.course.classCode, exam.course.credits))
                    Text(stringResource(R.string.schedule_exam_date, exam.date.format(dateFormat)))
                    Text(exam.session)
                    Text(stringResource(R.string.schedule_room_format, exam.room))
                    Text(exam.campus)
                    Text(stringResource(R.string.schedule_exam_method, exam.method))
                }
            }
        } else {
            item {
                Text(
                    stringResource(R.string.schedule_week_range,
                        monday.format(dateFormat), monday.plusDays(6).format(dateFormat)),
                    style = MaterialTheme.typography.titleSmall,
                )
                // Bấm nút là đổi ngày trực tiếp, không cần gửi Action qua lớp khác.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { selectedDateText = selectedDate.minusWeeks(1).toString() }) {
                        Text(stringResource(R.string.schedule_previous))
                    }
                    TextButton(onClick = { selectedDateText = LocalDate.now().toString() }) {
                        Text(stringResource(R.string.schedule_today))
                    }
                    TextButton(onClick = { selectedDateText = selectedDate.plusWeeks(1).toString() }) {
                        Text(stringResource(R.string.schedule_next))
                    }
                }
                // LazyRow giữ vùng chạm đủ lớn, cho phép cuộn ngang trên điện thoại hẹp.
                LazyRow(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    items(days, key = { it.toEpochDay() }) { date ->
                        FilterChip(
                            selected = date == selectedDate,
                            onClick = { selectedDateText = date.toString() },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(date.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("vi-VN"))))
                                    Text(date.format(DateTimeFormatter.ofPattern("dd/MM")))
                                }
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    FilterChip(selected = !showWeek, onClick = { showWeek = false },
                        label = { Text(stringResource(R.string.schedule_day)) })
                    FilterChip(selected = showWeek, onClick = { showWeek = true },
                        label = { Text(stringResource(R.string.schedule_week)) })
                }
            }
            val visibleDays = if (showWeek) days else listOf(selectedDate)
            items(visibleDays, key = { if (showWeek) it.toEpochDay() else 0L }) { date ->
                // Crossfade nhẹ khi đổi ngày. Danh sách chỉ lọc các buổi đúng ngày được chọn.
                Crossfade(targetState = date, label = "schedule-day") { shownDate ->
                    DayClasses(shownDate, sampleClasses)
                }
            }
        }
        item { Spacer(Modifier.height(UthDimens.Spacing.Xl)) }
    }
}

@Composable
private fun DayClasses(date: LocalDate, classes: List<CourseClassItem>) {
    val dailyClasses = classes.filter { it.date == date }.sortedBy { it.startPeriod }
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        Text(date.format(dayFormat), style = MaterialTheme.typography.titleMedium)
        if (dailyClasses.isEmpty()) {
            UthEmptyState(stringResource(R.string.schedule_empty_title), stringResource(R.string.schedule_empty_body))
        }
        dailyClasses.forEach { CourseClassCard(it) }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun ScheduleScreenPreview() = UthPreview {
    ScheduleScreen()
}
@Composable
fun CourseClassCard(item: CourseClassItem, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        // Xếp dọc để mã lớp dài, tên môn và cơ sở đều có thể tự xuống dòng.
        Text(
            text = stringResource(R.string.schedule_period_format, item.startPeriod, item.endPeriod),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(item.timeRange, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(item.courseName, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.schedule_class_code_format, item.classCode, item.credits),
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(stringResource(R.string.schedule_room_format, item.room),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(item.campus, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.schedule_lecturer, item.lecturer),
            style = MaterialTheme.typography.bodySmall, color = UthTheme.extendedColors.textSecondary)
    }
}
