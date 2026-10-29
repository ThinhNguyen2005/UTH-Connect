package com.giathinh.uthconnect.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Theme UTH Connect.
 *
 * `dynamicColor` mặc định **false**: nếu bật, Material You (Android 12+ = mọi
 * thiết bị app hỗ trợ vì minSdk 31) sẽ ghi đè Navy/Gold bằng màu hình nền của
 * máy, khiến nhận diện trường và các cặp tương phản đã đo bị vô hiệu.
 * Chi tiết: `docs/Design-system.md` mục 1.5.
 */

/**
 * Màu mở rộng chưa có chỗ trong [androidx.compose.material3.ColorScheme]:
 * nền phụ, chữ phụ, và 4 nhóm trạng thái (thành công / cảnh báo / lỗi / thông tin).
 */
@Immutable
data class UthExtendedColors(
    val surfaceSubtle: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    /** Nền/huyền cho badge "SV đã xác thực" và "Gợi ý bởi AI". */
    val accentContainer: Color,
    val onAccentContainer: Color,
    val isLight: Boolean,
)

private val LightExtended = UthExtendedColors(
    surfaceSubtle = UthSurfaceSubtleLight,
    textSecondary = UthTextSecondaryLight,
    textTertiary = UthTextTertiaryLight,
    success = SuccessMainLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = SuccessOnContainerLight,
    warning = WarningMainLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = WarningOnContainerLight,
    danger = ErrorMainLight,
    dangerContainer = ErrorContainerLight,
    onDangerContainer = ErrorOnContainerLight,
    info = InfoMainLight,
    infoContainer = InfoContainerLight,
    onInfoContainer = InfoOnContainerLight,
    accentContainer = UthGoldContainer,
    onAccentContainer = UthGoldText,
    isLight = true,
)

private val DarkExtended = UthExtendedColors(
    surfaceSubtle = UthSurfaceSubtleDark,
    textSecondary = UthTextSecondaryDark,
    textTertiary = UthTextTertiaryDark,
    success = SuccessMainDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = SuccessOnContainerDark,
    warning = WarningMainDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = WarningOnContainerDark,
    danger = ErrorMainDark,
    dangerContainer = ErrorContainerDark,
    onDangerContainer = ErrorOnContainerDark,
    info = InfoMainDark,
    infoContainer = InfoContainerDark,
    onInfoContainer = InfoOnContainerDark,
    accentContainer = UthNavyContainerDark,
    onAccentContainer = UthOnNavyContainerDark,
    isLight = false,
)

val LocalUthExtendedColors: ProvidableCompositionLocal<UthExtendedColors> =
    staticCompositionLocalOf { LightExtended }

/** Trỏ tới token mở rộng: `UthTheme.extendedColors.success`. */
object UthTheme {
    val extendedColors: UthExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUthExtendedColors.current
}

private val UthLightScheme = lightColorScheme(
    primary = UthNavy,
    onPrimary = White,
    primaryContainer = UthNavyContainerLight,
    onPrimaryContainer = UthOnNavyContainerLight,
    secondary = UthGold,
    onSecondary = UthNavy, // Navy trên Gold = 6.2:1 (AA). KHÔNG dùng chữ trắng trên Gold (2.0:1).
    secondaryContainer = UthGoldContainer,
    onSecondaryContainer = UthGoldText,
    tertiary = UthNavyLight,
    onTertiary = White,
    tertiaryContainer = UthTertiaryContainerLight,
    onTertiaryContainer = UthOnNavyContainerLight,
    background = UthBackgroundLight,
    onBackground = UthTextPrimaryLight,
    surface = UthSurfaceLight,
    onSurface = UthTextPrimaryLight,
    surfaceVariant = UthSurfaceSubtleLight,
    onSurfaceVariant = UthTextSecondaryLight,
    outline = UthOutlineLight,
    outlineVariant = UthOutlineVariantLight,
    error = ErrorMainLight,
    onError = White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight,
    surfaceTint = UthNavy,
)

private val UthDarkScheme = darkColorScheme(
    primary = UthNavyOnDark,
    onPrimary = UthOnNavyDark,
    primaryContainer = UthNavyContainerDark,
    onPrimaryContainer = UthOnNavyContainerDark,
    secondary = UthGoldOnDark,
    onSecondary = UthOnGoldDark,
    secondaryContainer = UthGoldContainerDark,
    onSecondaryContainer = UthOnGoldContainerDark,
    tertiary = UthNavyLight,
    onTertiary = White,
    tertiaryContainer = UthNavyContainerDark,
    onTertiaryContainer = UthOnNavyContainerDark,
    background = UthBackgroundDark,
    onBackground = UthTextPrimaryDark,
    surface = UthSurfaceDark,
    onSurface = UthTextPrimaryDark,
    surfaceVariant = UthSurfaceSubtleDark,
    onSurfaceVariant = UthTextSecondaryDark,
    outline = UthOutlineDark,
    outlineVariant = UthOutlineVariantDark,
    error = ErrorMainDark,
    onError = UthOnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorOnContainerDark,
    surfaceTint = UthNavyOnDark,
)

@Composable
fun UTHConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val extendedColors = if (darkTheme) DarkExtended else LightExtended
    // dynamicColor để mặc định false và chỉ bật khi lập trình viên chủ động xin:
    // Material You sẽ thay Navy/Gold bằng màu hình nền của máy.
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> UthDarkScheme
        else -> UthLightScheme
    }

    CompositionLocalProvider(LocalUthExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UthTypography,
            shapes = UthShapes,
            content = content,
        )
    }
}

/** Helper cho `@Preview` của mọi màn hình: light + dark, đủ 4 trạng thái. */
@Composable
fun UthPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    UTHConnectTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
}
