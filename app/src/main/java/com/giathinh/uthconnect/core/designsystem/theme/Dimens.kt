package com.giathinh.uthconnect.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token khoảng cách / bo góc / kích thước / chuyển động.
 * Nguồn sự thật: `docs/Design-system.md` mục 3 và mục 5.
 *
 * Quy tắc: Composable không được phép xuất hiện `.dp` tự do ngoài object này.
 */
object UthDimens {

    /** Lưới 8pt. */
    object Spacing {
        /** 2.dp — viền trong, gap biểu tượng nhỏ. */
        val Xxs: Dp = 2.dp

        /** 4.dp — icon ↔ text trong cùng một thẻ. */
        val Xs: Dp = 4.dp

        /** 8.dp — title ↔ subtitle. */
        val Sm: Dp = 8.dp

        /** 12.dp — padding trong card nhỏ, badge. */
        val Md: Dp = 12.dp

        /** 16.dp — padding chuẩn của component và nút. */
        val Lg: Dp = 16.dp

        /** 24.dp — lề ngang mọi màn hình. */
        val Xl: Dp = 24.dp

        /** 32.dp — khoảng giữa các section lớn. */
        val Xxl: Dp = 32.dp

        /** 40.dp — khoảng trước header của một nhóm nội dung mới. */
        val Section: Dp = 40.dp
    }

    object Radius {
        /** 4.dp — tag, checkbox. */
        val Sm: Dp = 4.dp

        /** 8.dp — nút bấm, text field. */
        val Md: Dp = 8.dp

        /** 16.dp — card sự kiện, bài đăng. */
        val Lg: Dp = 16.dp

        /** 24.dp — bottom sheet, dialog. */
        val Xl: Dp = 24.dp

        val SmShape = RoundedCornerShape(Sm)
        val MdShape = RoundedCornerShape(Md)
        val LgShape = RoundedCornerShape(Lg)
        val XlShape = RoundedCornerShape(topStart = Xl, topEnd = Xl, bottomStart = 0.dp, bottomEnd = 0.dp)
    }

    object Size {
        /** Chiều cao nút primary. */
        val ButtonHeight: Dp = 48.dp

        /** Vùng chạm tối thiểu cho mọi phần tử bấm được (WCAG / Material). */
        val MinTouchTarget: Dp = 48.dp

        /** Top app bar. */
        val TopBar: Dp = 64.dp

        /** Bottom navigation (khớp mặc định Material 3). */
        val BottomBar: Dp = 80.dp

        /** FAB chuẩn / FAB mini. */
        val Fab: Dp = 56.dp
        val FabMini: Dp = 40.dp

        /** Avatar. */
        val Avatar: Dp = 40.dp
        val AvatarLarge: Dp = 56.dp

        /** Badge/tag cao 24dp. */
        val Badge: Dp = 24.dp

        /** Ảnh bìa sự kiện cao tương ứng tỉ lệ 16:9 trên bề rộng màn hình phổ biến. */
        val CoverHeight: Dp = 180.dp

        /** Cạnh tối thiểu của QR để máy quét ở bàn check-in đọc được. */
        val QrMin: Dp = 220.dp

        /** Viền chuẩn của card / text field. */
        val Stroke: Dp = 1.dp
        val StrokeFocused: Dp = 2.dp

        /** CircularProgressBar trạng thái loading (đường kính / nét quay). */
        val Progress: Dp = 40.dp
        val ProgressStroke: Dp = 3.dp

        /** Vòng loading nhỏ nhúng **trong nút** — nhỏ hơn indicator toàn màn hình. */
        val ProgressInline: Dp = 20.dp
        val ProgressInlineStroke: Dp = 2.dp

        /** Icon inline: 18.dp trong nút, 14.dp trong badge. */
        val IconInline: Dp = 18.dp
        val IconBadge: Dp = 14.dp

        /** Icon chuẩn 24.dp — nút icon-only (chuông thông báo) và icon đại diện khi ảnh chưa có. */
        val Icon: Dp = 24.dp

        /** Ảnh thumbnail vuông trong danh sách bảng tin (trang chủ). */
        val Thumbnail: Dp = 80.dp

        /**
         * Filter chip cao 32.dp (`docs/Design-system.md` mục 4.4). Phần nhìn thấy cao
         * 32.dp nhưng phải bọc trong vùng chạm [MinTouchTarget] — chip không tự đạt 48.dp.
         */
        val ChipHeight: Dp = 32.dp

        /** Chấm trạng thái 8.dp — dot "chưa đọc", chấm trang trí cạnh lời chào. */
        val IndicatorDot: Dp = 8.dp

        /** Chiều ngang pill của page indicator đang chọn (cao bằng [IndicatorDot]). */
        val IndicatorBar: Dp = 20.dp

        /** Drag handle của bottom sheet. */
        val SheetHandleWidth: Dp = 32.dp
        val SheetHandleHeight: Dp = 4.dp
    }

    /**
     * Nền nâng (elevation) — cũng phải token hoá, vì `tonalElevation`/`defaultElevation`
     * nhận `Dp` nên rất dễ bị viết số lẻ trực tiếp trong Composable.
     */
    object Elevation {
        /** Card 1.dp — phân tách bằng viền nhiều hơn bằng bóng (mục 4.1). */
        val Card: Dp = 1.dp

        /** Bottom navigation 2.dp. */
        val BottomBar: Dp = 2.dp
    }

    /** Duration (ms) — tắt khi người dùng bật "Giảm chuyển động". */
    object Motion {
        val PressMillis = 100
        val RouteFadeMillis = 200
        val SheetExpandMillis = 250
        val QrRefreshMillis = 150
        val ShimmerMillis = 1200

        /** Ngưỡng đổi màu deadline sang cảnh báo (T-3 ngày) và sang lỗi (48 giờ). */
        val DeadlineWarnDays = 3
        val DeadlineErrorHours = 48
    }
}

/** Bộ bo góc đưa vào `MaterialTheme.shapes`; mọi shape lấy từ đây, không tạo mới trong Composable. */
val UthShapes = Shapes(
    extraSmall = UthDimens.Radius.SmShape,
    small = UthDimens.Radius.MdShape,
    medium = UthDimens.Radius.LgShape,
    large = UthDimens.Radius.XlShape,
)
