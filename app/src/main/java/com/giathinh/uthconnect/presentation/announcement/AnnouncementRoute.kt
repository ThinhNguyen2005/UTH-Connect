package com.giathinh.uthconnect.presentation.announcement

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R

/**
 * Tầng nối của màn hình Chi tiết: chỗ **duy nhất** trong module lấy ViewModel từ Hilt và chỗ
 * duy nhất chạm `Context`/`Intent` (Screen thuần Compose, không có `Context`).
 *
 * - Quay lại: do graph con truyền vào (`AnnouncementNavigation.kt`) — Route không tự quyết định
 *   back stack.
 * - Chia sẻ: gom tiêu đề + đoạn mở đầu thành chữ rồi mở share sheet hệ thống. Chưa có
 *   deep link nên văn bản chia sẻ không kèm đường dẫn; khi có Firebase/Deep link, chỉ
 *   thêm link vào đây.
 */
@Composable
fun AnnouncementRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnnouncementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.detail
    val context = LocalContext.current

    val chooserTitle = stringResource(R.string.announcement_share_chooser)
    val shareText = detail?.let { loaded ->
        stringResource(
            R.string.announcement_share_body,
            loaded.title,
            loaded.lead ?: loaded.publisher,
        )
    }
    val shareSubject = detail?.title.orEmpty()

    AnnouncementScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onShare = {
            if (shareText != null) {
                context.startActivity(
                    shareChooser(text = shareText, subject = shareSubject, title = chooserTitle),
                )
            }
        },
        modifier = modifier,
    )
}

/** Share sheet chuẩn: chỉ gửi chữ thuần, không đính kèm tệp và không xin quyền gì thêm. */
private fun shareChooser(text: String, subject: String, title: String): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        },
        title,
    )
