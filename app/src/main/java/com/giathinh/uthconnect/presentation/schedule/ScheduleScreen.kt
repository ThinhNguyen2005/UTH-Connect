package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

// Mô hình dữ liệu tạm cho một tiết học theo chuẩn cổng UTH
data class CourseClassItem(
    val id: String,
    val courseName: String,     // Lập trình di động
    val classCode: String,      // Mã lớp học phần: 010100085402
    val credits: Int,           // Số tín chỉ: 3
    val startPeriod: Int,       // Tiết bắt đầu: 1
    val endPeriod: Int,         // Tiết kết thúc: 3
    val timeRange: String,      // 07:00 - 09:15
    val room: String,           // B102
    val campus: String,         // Cơ sở 1 (Võ Oanh)
    val lecturer: String,       // TS. Nguyễn Văn A
)

/**
 * Tab Thời khóa biểu — khung chờ cho module Phase 2 (`docs/Design.md` mục 3).
 * Người nhận branch `feature/timetable-<tên>` thay màn hình này bằng màn hình thật.
 */
@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        UthEmptyState(
            title = stringResource(R.string.schedule_wip_title),
            body = stringResource(R.string.schedule_wip_body),
            icon = Icons.Filled.DateRange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleScreenPreview() = UthPreview { ScheduleScreen() }

@Composable
fun CourseClassCard(
    item: CourseClassItem,
    modifier: Modifier = Modifier,
) {
    UthCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. CỘT TRÁI: TIẾT HỌC & GIỜ HỌC
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
            ) {
                Text(
                    text = stringResource(
                        R.string.schedule_period_format,
                        item.startPeriod,
                        item.endPeriod,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                // Khoảng cách nhỏ giữa 2 dòng
                Spacer(Modifier.height(UthDimens.Spacing.Xxs))

                Text(
                    text = item.timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            // Khoảng cách ngang 16dp giữa cột trái và cột phải
            Spacer(Modifier.width(UthDimens.Spacing.Lg))

            // 2. CỘT PHẢI: THÔNG TIN MÔN HỌC (nằm trọn trong Row)
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Tên môn học
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(UthDimens.Spacing.Xs))

                // Mã lớp học phần & Số tín chỉ
                Text(
                    text = stringResource(
                        R.string.schedule_class_code_format,
                        item.classCode,
                        item.credits,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )

                Spacer(Modifier.height(UthDimens.Spacing.Sm))

                // Phòng học & Cơ sở
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.schedule_room_format, item.room),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall,
                            )
                            .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
                    )

                    Spacer(Modifier.width(UthDimens.Spacing.Sm))

                    Text(
                        text = item.campus,
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseClassCardPreview() {
    UthPreview {
        CourseClassCard(
            item = CourseClassItem(
                id = "1",
                courseName = "Lập trình thiết bị di động",
                classCode = "242_1COMP10203_01",
                credits = 3,
                startPeriod = 1,
                endPeriod = 3,
                timeRange = "07:00 - 09:15",
                room = "B102",
                campus = "CS1 (Võ Oanh)",
                lecturer = "TS. Nguyễn Văn A",
            ),
            modifier = Modifier.padding(UthDimens.Spacing.Lg),
        )
    }
}

