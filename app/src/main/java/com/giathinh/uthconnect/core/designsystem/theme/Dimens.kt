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

        /** Avatar. */
        val Avatar: Dp = 40.dp
        val AvatarLarge: Dp = 56.dp

        /** Badge/tag cao 24dp. */
        val Badge: Dp = 24.dp

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

        /** Icon trong CTA nhỏ (điện thoại/email); không dùng cho vùng chạm. */
        val IconCompact: Dp = 16.dp

        /** Nền icon trong ô Tiện ích; vùng chạm do UthCard của Material 3 cung cấp. */
        val UtilityIconContainer: Dp = 44.dp

        /**
         * Ô Tiện ích hạng nhất (2 cột, có mô tả): nền icon 56.dp và glyph duotone 32.dp.
         * Nhỏ hơn [UtilityIconContainer] sẽ biến tile thành nút icon, lớn hơn sẽ đẩy mô tả
         * xuống quá màn hình đầu tiên.
         */
        val UtilityFeatureIconContainer: Dp = 56.dp
        val UtilityFeatureIcon: Dp = 32.dp

        /** Ô dịch vụ mật độ cao (3 cột, không mô tả): nền 44.dp, glyph [Icon] 24.dp. */
        val UtilityServiceIconContainer: Dp = 44.dp

        /** Kích thước monogram logo UTH trên header thương hiệu. */
        val BrandLogoMonogram: Dp = 48.dp

        /** Chiều cao thanh segmented control chuyển đổi mode đăng nhập (≥ 48dp). */
        val SegmentedControlHeight: Dp = 48.dp


        /** Pill chuyển đổi ngôn ngữ VI / EN. */
        val LanguagePillWidth: Dp = 48.dp
        val LanguagePillHeight: Dp = 36.dp
        val SecurityIconSize: Dp = 16.dp

        /** Chiều cao thanh tiến độ điểm rèn luyện. */
        val TrainingProgressTrack: Dp = 6.dp

        /** Ảnh thumbnail vuông trong danh sách bảng tin (trang chủ). */
        val Thumbnail: Dp = 80.dp

        /** Dải màu nhận diện bên trái thẻ vé dạng boarding pass (tab Vé). */
        val TicketStripe: Dp = 4.dp

        /** Bề ngang cuống vé bên phải — đủ chỗ cho QR mini và nhãn mở vé. */
        val TicketStub: Dp = 88.dp

        /** Cạnh QR mini in trên cuống vé; QR thật khi mở vé vẫn dùng [QrMin]. */
        val TicketStubQr: Dp = 44.dp

        /** Khuyết tròn hai đầu đường xé của thẻ vé ở màn chi tiết (rộng × cao). */
        val TicketNotchWidth: Dp = 16.dp
        val TicketNotchHeight: Dp = 32.dp

        /** Cạnh ô góc ngắm (4 chữ L) trong khung QR của màn chi tiết. */
        val QrDesignator: Dp = 16.dp

        /** Ô vuông chữ "UTH" đặt giữa mã QR — che phần lõi nên chỉ dùng cho QR mô phỏng. */
        val QrMonogram: Dp = 40.dp

        /** Khung ngắm vuông của máy quét QR điểm danh — lớn hơn [QrMin] để vùng lấy nét dư dật. */
        val ScannerFrame: Dp = 260.dp

        /** Nét vẽ khung ngắm + vạch quét đang chạy trên màn máy quét. */
        val ScannerStroke: Dp = 3.dp

        /** Vòng bao hình chữ nhật của nút đèn pin / đóng trên nền camera. */
        val ScannerIconButton: Dp = 44.dp

        /**
         * Filter chip cao 32.dp (`docs/Design-system.md` mục 4.4). Phần nhìn thấy cao
         * 32.dp nhưng phải bọc trong vùng chạm [MinTouchTarget] — chip không tự đạt 48.dp.
         */
        val ChipHeight: Dp = 32.dp

        /** Chấm trạng thái 8.dp — dot "chưa đọc", chấm trang trí cạnh lời chào. */
        val IndicatorDot: Dp = 8.dp

        /** Chiều ngang pill của page indicator đang chọn (cao bằng [IndicatorDot]). */
        val IndicatorBar: Dp = 20.dp

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

    /** Duration (ms) cho các hiệu ứng được dự án quản lý trực tiếp. */
    object Motion {
        const val RouteFadeMillis = 200
        const val ShimmerMillis = 1200

        /** Một lượt quét vạch sáng đi hết khung ngắm trên màn máy quét QR. */
        const val ScannerSweepMillis = 1600
    }

    /** Độ trong suốt của lớp trang trí; màu chữ quan trọng vẫn dùng semantic color. */
    object Opacity {
        const val HeaderMetadata = 0.78f
        const val HeaderActionFill = 0.14f
        const val HeaderActionBorder = 0.22f
        const val HomeHeaderRing = 0.06f
        const val HomeHeaderAccentRing = 0.10f
        const val CoverPlaceholder = 0.28f
        const val CoverLabelSurface = 0.92f
        const val ShimmerHighlight = 0.70f
        const val TicketHeaderRing = 0.14f
        const val TicketSelectedCount = 0.24f
        const val TicketStub = 0.50f
        const val SubtleDivider = 0.50f

        // Modern Minimalist Auth & Translucent Borders
        /** Lớp nền mỹ thuật ambient Canvas góc trên phải. */
        const val AuthAmbientShape = 0.45f
        /** Độ mờ nhẹ cho footer bản quyền & bảo mật. */
        const val AuthFooter = 0.45f
        /** Viền mờ cho nút phụ OutlinedButton (Google SSO). */
        const val AuthOutlineButtonBorder = 0.45f
        /** Viền không focus của input field trên nền Canvas. */
        const val AuthInputUnfocusedBorder = 0.40f
        /** Viền cho banner thông báo thành công. */
        const val AuthSuccessBannerBorder = 0.40f
        /** Viền cho banner thông báo thông tin / pending. */
        const val AuthInfoBannerBorder = 0.30f
        /** Đường kẻ phân cách "hoặc tiếp tục với". */
        const val AuthDivider = 0.35f
        /** Lớp phủ tối quanh khung ngắm để vạch quét nổi lên trên hình camera thật. */
        const val ScannerScrim = 0.55f

        /** Nền panel kết quả / thanh công cụ nổi trên preview của máy quét. */
        const val ScannerPanel = 0.92f
    }

    /** Tỷ lệ bố cục và hình trang trí của các màn hiện có; không thay thế quy tắc đo của M3. */
    object Layout {
        const val CoverAspectRatio = 16f / 9f
        /** Cho phép mục điều hướng có đủ chỗ khi chữ được phóng lớn. */
        val LargeTextNavigationItemWidth: Dp = 144.dp
        const val LargeTextScale = 1.5f
        const val HomeBannerWidthFraction = 0.86f

        // Auth Screen Artistic Background
        /** Tỉ lệ bán kính hình tròn ambient Canvas so với chiều rộng màn hình. */
        const val AuthAmbientRadiusFraction = 0.45f
        /** Vị trí tâm X hình tròn ambient (lệch nhẹ sang mép phải ngoài màn hình). */
        const val AuthAmbientCenterFractionX = 1.05f
        /** Vị trí tâm Y hình tròn ambient (lệch nhẹ lên mép trên ngoài màn hình). */
        const val AuthAmbientCenterFractionY = -0.05f

        const val HomeRingRadiusFraction = 0.42f
        const val HomeRingStrokeFraction = 0.055f
        const val HomeRingOffsetFraction = 0.30f
        const val HomeAccentRingRadiusFactor = 0.66f
        const val HomeAccentRingCenterFraction = 0.92f
        const val HomeAccentRingStrokeFactor = 0.60f

        const val TicketRingRadiusFraction = 0.62f
        const val TicketRingStrokeFraction = 0.008f
        const val TicketRingCenterFraction = 0.95f
        const val TicketDashedLineDashFactor = 1f
        const val TicketDashedLineGapFactor = 1.2f

        const val HomeSkeletonRowCount = 3
        const val HomeSkeletonTitleFraction = 0.62f
        const val HomeSkeletonMetadataFraction = 0.38f
        const val ShimmerBandFraction = 0.60f
    }
}

/** Bộ bo góc đưa vào `MaterialTheme.shapes`; mọi shape lấy từ đây, không tạo mới trong Composable. */
val UthShapes = Shapes(
    extraSmall = UthDimens.Radius.SmShape,
    small = UthDimens.Radius.MdShape,
    medium = UthDimens.Radius.LgShape,
    large = UthDimens.Radius.XlShape,
)
