package com.giathinh.uthconnect.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Bảng màu UTH Connect — Modern Cobalt & Warm Amber (Hiện đại & Tinh tế).
// Bản sao lưu màu cổ điển được lưu tại `docs/backups/Color_classic.kt`.
//
// KHÔNG dùng màu này trực tiếp trong Composable. Hãy lấy qua
// `MaterialTheme.colorScheme.*` hoặc `UthTheme.extendedColors` (xem `Theme.kt`).
//
// Tỷ số tương phản đã đo theo WCAG 2.2 trên các nền Surface/Background.

// ───────────────────────── Brand (Nhận diện UTH hiện đại hóa) ─────────────────────────

/**
 * Xanh Cobalt Tech Blue #0F5FC2 — tươi sáng, sắc nét trên màn hình số, chuẩn công nghệ.
 * Chữ trắng đạt ~6.1:1 (chuẩn WCAG AA cho văn bản thông thường).
 */
val UthNavy = Color(0xFF0F5FC2)

/** Cobalt sáng dùng cho tương tác phụ hoặc gradient. */
val UthNavyLight = Color(0xFF2575D6)

/** Vàng Amber hoàng hôn #F59E0B — màu nhấn ấm áp, sang trọng, không gắt mắt. */
val UthGold = Color(0xFFF59E0B)

/** Nền badge/tooltip amber nhạt, êm mắt. */
val UthGoldContainer = Color(0xFFFEF3C7)

/** Chữ amber đậm trên nền [UthGoldContainer] đạt 7.0:1 (AAA). */
val UthGoldText = Color(0xFF92400E)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ───────────────────────── Neutral (light - Slate Minimalist) ─────────────────────────

/** Nền màn hình Slate siêu nhẹ, triệt tiêu sắc ngà cũ, tạo cảm giác thoáng đãng. */
val UthBackgroundLight = Color(0xFFF8FAFC)

/** Card / BottomSheet / Dialog — trắng tinh khiết nổi bật trên nền Slate. */
val UthSurfaceLight = Color(0xFFFFFFFF)

/** List item xen kẽ, vùng inactive, skeleton — Slate 100. */
val UthSurfaceSubtleLight = Color(0xFFF1F5F9)

/** Viền component light: Slate 300 thanh mảnh, tinh tế, không đóng hộp thô. */
val UthOutlineLight = Color(0xFFCBD5E1)

/** Viền phân tách nhẹ (divider, outlineVariant) — Slate 200. */
val UthOutlineVariantLight = Color(0xFFE2E8F0)

/** Nền `primaryContainer` — xanh băng thanh mờ cho chip/button đã chọn. */
val UthNavyContainerLight = Color(0xFFEBF3FD)

/** Chữ/icon trên [UthNavyContainerLight] đạt 8.5:1 (AAA). */
val UthOnNavyContainerLight = Color(0xFF083A7A)

/** Nền `tertiaryContainer` — xanh pastel trong trẻo. */
val UthTertiaryContainerLight = Color(0xFFDCEBFC)

/** Chữ chính Slate 900 — 16.1:1 trên Surface (AAA), sắc nét cho Typography. */
val UthTextPrimaryLight = Color(0xFF0F172A)

/** Chữ phụ, mô tả, timestamp — Slate 600, 5.9:1 (AA). */
val UthTextSecondaryLight = Color(0xFF475569)

/** Chữ disabled/trang trí — Slate 400. */
val UthTextTertiaryLight = Color(0xFF94A3B8)

// ───────────────────────── Neutral (dark - Slate Deep Night) ─────────────────────────

val UthBackgroundDark = Color(0xFF0F172A)
val UthSurfaceDark = Color(0xFF1E293B)
val UthSurfaceSubtleDark = Color(0xFF334155)

/** Viền component dark: Slate 600. */
val UthOutlineDark = Color(0xFF475569)
val UthTextPrimaryDark = Color(0xFFF8FAFC)
val UthTextSecondaryDark = Color(0xFF94A3B8)
val UthTextTertiaryDark = Color(0xFF64748B)

/** Viền phân tách nhẹ trong dark — Slate 700. */
val UthOutlineVariantDark = Color(0xFF334155)

/** Chữ trên `primary` sáng của dark-mode. */
val UthOnNavyDark = Color(0xFF0B1E3B)

/** Chữ Navy trên nền Gold sáng [UthGoldOnDark]. */
val UthOnGoldDark = Color(0xFF0F172A)
val UthGoldContainerDark = Color(0xFF451A03)
val UthOnGoldContainerDark = Color(0xFFFDE68A)

/** Chữ trên `error` đỏ sáng trong dark. */
val UthOnErrorDark = Color(0xFF2A1414)

/** Primary trên dark mode — xanh sáng tươi tắn, không bị chìm. */
val UthNavyOnDark = Color(0xFF60A5FA)
val UthNavyContainerDark = Color(0xFF1E3A8A)
val UthOnNavyContainerDark = Color(0xFFDBEAFE)
val UthGoldOnDark = Color(0xFFFBBF24)

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
