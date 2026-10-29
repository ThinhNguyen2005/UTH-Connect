package com.giathinh.uthconnect.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Bảng màu UTH Connect — nguồn sự thật: `docs/Design-system.md` mục 1.
 *
 * KHÔNG dùng màu này trực tiếp trong Composable. Hãy lấy qua
 * `MaterialTheme.colorScheme.*` hoặc `UthTheme.extendedColors` (xem `Theme.kt`).
 *
 * Tỷ số tương phản đã đo theo WCAG 2.1 với nền trắng #FFFFFF.
 */

// ───────────────────────── Brand (màu nhận diện trường UTH) ─────────────────────────

/** Xanh Navy #003366 — màu chính: nút primary, tab đang chọn, top bar. Chữ trắng 12.6:1 (AAA). */
val UthNavy = Color(0xFF003366)

/** Navy sáng dùng làm nền dark-mode surface/primary-on-dark. */
val UthNavyLight = Color(0xFF1B4F80)

/** Vàng Gold #F5A623 — màu nhấn: FAB, huy hiệu, điểm báo. CHỈ dùng làm nền + chữ Navy (6.2:1). */
val UthGold = Color(0xFFF5A623)

/** Vàng nhạt dùng làm nền badge/tooltip. Chữ [UthGoldText] đạt 6.5:1. */
val UthGoldContainer = Color(0xFFFFF3DC)

/**
 * Chữ vàng đậm. KHÔNG bao giờ dùng [UthGold] làm màu chữ trên nền trắng
 * (chỉ 2.0:1, fail WCAG AA).
 */
val UthGoldText = Color(0xFF7A4E00)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ───────────────────────── Neutral (light) ─────────────────────────

/** Nền màn hình, tông ấm dịu mắt. */
val UthBackgroundLight = Color(0xFFFAFAF9)

/** Card / BottomSheet / Dialog. */
val UthSurfaceLight = Color(0xFFFFFFFF)

/** List item xen kẽ, vùng inactive, skeleton. */
val UthSurfaceSubtleLight = Color(0xFFF0F3F6)

/** Viền 1.dp của card và text field. */
val UthOutlineLight = Color(0xFFD5DCE3)

/** Viền phân tách nhẹ (divider, outlineVariant) — mờ hơn [UthOutlineLight]. */
val UthOutlineVariantLight = Color(0xFFE4E7EB)

/** Nền `primaryContainer` — xanh rất nhạt cho chip/button đã chọn. */
val UthNavyContainerLight = Color(0xFFE6EDF5)

/** Chữ trên [UthNavyContainerLight] / [UthTertiaryContainerLight]. */
val UthOnNavyContainerLight = Color(0xFF0A2A4A)

/** Nền `tertiaryContainer` — nhạt hơn [UthNavyContainerLight] một bậc. */
val UthTertiaryContainerLight = Color(0xFFDCE8F5)

/** Chữ chính — 15.6:1 (AAA). */
val UthTextPrimaryLight = Color(0xFF14213D)

/** Chữ phụ, mô tả, timestamp — 5.5:1 (AA). */
val UthTextSecondaryLight = Color(0xFF5A6B7C)

/** Chữ disabled/trang trí — 2.9:1, KHÔNG dùng cho nội dung bắt buộc đọc. */
val UthTextTertiaryLight = Color(0xFF8C99A6)

// ───────────────────────── Neutral (dark) ─────────────────────────

val UthBackgroundDark = Color(0xFF0B1622)
val UthSurfaceDark = Color(0xFF13202E)
val UthSurfaceSubtleDark = Color(0xFF1C2C3D)
val UthOutlineDark = Color(0xFF33465C)
val UthTextPrimaryDark = Color(0xFFEAF1F8)
val UthTextSecondaryDark = Color(0xFFA9B7C6)
val UthTextTertiaryDark = Color(0xFF6B7A8A)

/** Viền phân tách nhẹ trong dark. */
val UthOutlineVariantDark = Color(0xFF26364A)

/** Chữ trên `primary` sáng của dark-mode (Navy gốc quá tối nên cần bản đậm đặc). */
val UthOnNavyDark = Color(0xFF062038)

/** Chữ Navy trên nền Gold sáng [UthGoldOnDark] — 10.0:1. */
val UthOnGoldDark = Color(0xFF0B1622)
val UthGoldContainerDark = Color(0xFF3A2C10)
val UthOnGoldContainerDark = Color(0xFFFCD98A)

/** Chữ trên `error` đỏ sáng trong dark (trùng giá trị [ErrorContainerDark] theo ý đồ). */
val UthOnErrorDark = Color(0xFF2A1414)

/** Navy quá tối trên nền dark → primary-on-dark là xanh sáng. */
val UthNavyOnDark = Color(0xFF7FB2E5)
val UthNavyContainerDark = Color(0xFF143252)
val UthOnNavyContainerDark = Color(0xFFBBD8F5)
val UthGoldOnDark = Color(0xFFF5B740)

// ───────────────────────── Semantic (light) ─────────────────────────
// Quy tắc: màu thuần (*.main) chỉ cho icon/nền trạng thái/viền.
// Chữ BẮT BUỘC dùng *.container + *.onContainer.

/** Trạng thái 10B981 — làm chữ trên trắng chỉ 2.5:1 nên không dùng cho text. */
val SuccessMainLight = Color(0xFF10B981)
val SuccessContainerLight = Color(0xFFECFDF5)
val SuccessOnContainerLight = Color(0xFF047857) // 5.2:1 trên container (AA)

val WarningMainLight = Color(0xFFF59E0B)
val WarningContainerLight = Color(0xFFFFFBEB)
val WarningOnContainerLight = Color(0xFF92400E) // 6.8:1 (AA)

val ErrorMainLight = Color(0xFFEF4444)
val ErrorContainerLight = Color(0xFFFEF2F2)
val ErrorOnContainerLight = Color(0xFFB91C1C) // 5.9:1 (AA)

val InfoMainLight = Color(0xFF0EA5E9)
val InfoContainerLight = Color(0xFFF0F9FF)
val InfoOnContainerLight = Color(0xFF075985)

// ───────────────────────── Semantic (dark) ─────────────────────────

val SuccessMainDark = Color(0xFF34D399)
val SuccessContainerDark = Color(0xFF0E241C)
val SuccessOnContainerDark = Color(0xFF6EE7B7)

val WarningMainDark = Color(0xFFFBBF24)
val WarningContainerDark = Color(0xFF2A2110)
val WarningOnContainerDark = Color(0xFFFCD34D)

val ErrorMainDark = Color(0xFFF87171)
val ErrorContainerDark = Color(0xFF2A1414)
val ErrorOnContainerDark = Color(0xFFFCA5A5)

val InfoMainDark = Color(0xFF38BDF8)
val InfoContainerDark = Color(0xFF0D2436)
val InfoOnContainerDark = Color(0xFF7DD3FC)
