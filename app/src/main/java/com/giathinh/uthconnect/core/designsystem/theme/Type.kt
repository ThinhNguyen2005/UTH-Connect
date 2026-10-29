package com.giathinh.uthconnect.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Typography UTH Connect — nguồn sự thật: `Design-system.md` mục 2.
 *
 * Font: Roboto (`FontFamily.Default`), không cần nhúng file.
 * Quy tắc: chỉ dùng 11 role ở dưới qua `MaterialTheme.typography.*`;
 * nghiêm cấm `.sp` tự do trong Composable.
 */
private val UthLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Proportional,
    trim = LineHeightStyle.Trim.None,
)

val UthTypography = Typography(
    // 32/40 · 600 — tiêu đề trang chính, số liệu thống kê to
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 28/36 · 600 — header tab khi có hero
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 24/32 · 600 — số đếm lớn trên màn hình kết quả
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 20/28 · 600 — TÊN SỰ KIỆN, tiêu đề bottom sheet
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 16/22 · 600 — tên người đăng Lost & Found, TÊN MÔN HỌC
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 14/20 · 600 — label nhóm mục
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 16/24 · 400 — nội dung bài đăng, tin nhắn
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 14/20 · 400 — mô tả phòng trọ, ghi chú môn học
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 12/16 · 400 — tooltip AI, vị trí, timestamp
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 14/20 · 500 — chữ trên nút, tag phân loại
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 12/16 · 500 — chip lọc, số chỗ còn lại
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
    // 11/16 · 500 — nhãn trong badge nhỏ, không dùng cho nội dung đọc dài
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = UthLineHeightStyle,
    ),
)

/**
 * Chữ số có bề rộng bằng nhau cho MSSV, giờ, số tiền — giữ các cột thẳng hàng
 * ở `EventStats` và danh sách check-in.
 */
val Typography.tabularFigures: TextStyle
    get() = bodyMedium.copy(fontFeatureSettings = "tnum")
