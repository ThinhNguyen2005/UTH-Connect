package com.giathinh.uthconnect.presentation.announcement

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.AnnouncementAttachment
import com.giathinh.uthconnect.domain.model.AnnouncementBlock
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.AttachmentKind
import com.giathinh.uthconnect.domain.model.Milestone
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Màn hình Chi tiết thông báo — stateless, chỉ nhận `UiState` + callback.
 *
 * Khác bản thiết kế tham chiếu ở ba điểm đã thống nhất:
 * - **Không có avatar** ở thanh trên (avatar thuộc tab Cá nhân, không thuộc ngữ cảnh đọc bài).
 * - **Không có khối "Nhắc nhở tự động / Lưu lịch"** ở cuối bài.
 * - **Không có bottom navigation**: đây là màn hình con, [AnnouncementRoute] được mở trên graph
 *   con nên `UthApp` ẩn thanh tab.
 *
 * Thân bài được vẽ theo từng [AnnouncementBlock], nên thông báo ngắn (chỉ tiêu đề + đoạn mở
 * đầu), thông báo có mốc thời gian và thông báo có tệp đính kèm dùng chung một màn hình.
 *
 * Nhãn nhóm dùng lại `home_category_*` vì đó là nhãn của chính `AnnouncementCategory` — hai màn
 * hình không được dịch cùng một enum thành hai câu khác nhau.
 */
@Composable
fun AnnouncementScreen(
    uiState: AnnouncementUiState,
    onAction: (AnnouncementAction) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = uiState.detail

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnnouncementTopBar(
            showShare = detail != null,
            onBack = onBack,
            onShare = onShare,
        )
        when {
            uiState.isLoading -> AnnouncementLoading()

            uiState.failure != null -> AnnouncementError(
                onRetry = { onAction(AnnouncementAction.Retry) },
            )

            detail != null -> AnnouncementArticle(detail = detail)

            else -> AnnouncementMissing()
        }
    }
}

/** Thanh trên 64dp: quay lại, tiêu đề, chia sẻ. Không avatar (`docs/Design-system.md` mục 3.3). */
@Composable
private fun AnnouncementTopBar(
    showShare: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UthDimens.Size.TopBar)
                .padding(horizontal = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = stringResource(R.string.announcement_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.announcement_detail_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = UthDimens.Spacing.Xs),
            )
            if (showShare) {
                TopBarAction(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.announcement_share),
                    onClick = onShare,
                )
            } else {
                Spacer(Modifier.width(UthDimens.Size.MinTouchTarget))
            }
        }
        HorizontalDivider(
            thickness = UthDimens.Size.Stroke,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Nút icon-only đạt vùng chạm 48dp; nhãn nằm ở `contentDescription`, không phải text ẩn. */
@Composable
private fun TopBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(UthDimens.Size.MinTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(UthDimens.Size.Icon),
        )
    }
}

/** Toàn bộ bài viết. Mỗi khối là một item riêng để bài dài vẫn cuộn nhẹ. */
@Composable
private fun AnnouncementArticle(
    detail: AnnouncementDetail,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UthDimens.Spacing.Xl,
            end = UthDimens.Spacing.Xl,
            top = UthDimens.Spacing.Lg,
            bottom = UthDimens.Spacing.Xxl,
        ),
    ) {
        item(key = AnnouncementKey.Header) { AnnouncementHeader(detail = detail) }

        item(key = AnnouncementKey.Cover) {
            AnnouncementCover(
                detail = detail,
                modifier = Modifier.padding(top = UthDimens.Spacing.Lg),
            )
        }

        detail.lead?.let { lead ->
            item(key = AnnouncementKey.Lead) {
                Text(
                    text = lead,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = UthDimens.Spacing.Xl),
                )
            }
        }

        itemsIndexed(
            items = detail.blocks,
            key = { index, _ -> "${AnnouncementKey.BlockPrefix}$index" },
        ) { _, block ->
            AnnouncementBlockView(
                block = block,
                modifier = Modifier.padding(top = UthDimens.Spacing.Xxl),
            )
        }

        if (detail.attachments.isNotEmpty()) {
            item(key = AnnouncementKey.Attachments) {
                AnnouncementAttachments(
                    attachments = detail.attachments,
                    modifier = Modifier.padding(top = UthDimens.Spacing.Xxl),
                )
            }
        }
    }
}

/** Nhóm, thời lượng đọc, tiêu đề, dòng meta và (nếu có) mốc cập nhật. */
@Composable
private fun AnnouncementHeader(
    detail: AnnouncementDetail,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UthStatusBadge(
                text = stringResource(detail.category.labelRes()).uppercase(),
                tone = BadgeTone.Accent,
                icon = null,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(
                    id = R.plurals.announcement_read_minutes,
                    count = detail.readMinutes,
                    detail.readMinutes,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        AnnouncementMeta(
            publisher = detail.publisher,
            trailing = detail.publishedAt.format(DetailDateFormatter),
        )
        detail.updatedAt?.let { updatedAt ->
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(
                    R.string.announcement_updated_at,
                    updatedAt.format(DetailDateFormatter),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
    }
}

/** Dòng "đơn vị • ngày" — cùng nhịp với dòng meta của bảng tin. */
@Composable
private fun AnnouncementMeta(
    publisher: String,
    trailing: String,
    modifier: Modifier = Modifier,
) {
    val secondary = UthTheme.extendedColors.textSecondary

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = publisher,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = stringResource(R.string.common_meta_separator),
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xs),
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Ảnh bìa 16:9 (mục 3.3). Khi chưa có ảnh thật, nền gradient + icon nhóm giữ đúng khung và
 * chip nguồn vẫn đọc được trên nền đó.
 */
@Composable
private fun AnnouncementCover(
    detail: AnnouncementDetail,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = detail.source

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CoverAspectRatio)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    listOf(colors.primaryContainer, UthTheme.extendedColors.surfaceSubtle),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = detail.category.icon(),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(UthDimens.Size.AvatarLarge),
        )
        if (source != null) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(UthDimens.Spacing.Md)
                    .background(
                        color = colors.surface.copy(alpha = CoverChipAlpha),
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                    .padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Xs,
                    ),
            )
        }
    }
}

/** Rẽ nhánh theo loại khối — thêm dạng nội dung mới chỉ phải thêm một nhánh ở đây. */
@Composable
private fun AnnouncementBlockView(
    block: AnnouncementBlock,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is AnnouncementBlock.Heading -> SectionHeading(text = block.text, modifier = modifier)
        is AnnouncementBlock.Paragraph -> Paragraph(text = block.text, modifier = modifier)
        is AnnouncementBlock.Bullets -> Bullets(items = block.items, modifier = modifier)
        is AnnouncementBlock.Milestones -> Milestones(items = block.items, modifier = modifier)
        is AnnouncementBlock.Contact -> Contact(block = block, modifier = modifier)
    }
}

/** Tiêu đề mục: thanh nhấn 4×16dp + `titleMedium` (cùng nhịp với mục lớn ở tab Trang chủ). */
@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = UthDimens.Spacing.Xs, height = UthDimens.Spacing.Lg)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = UthDimens.Spacing.Md),
        )
    }
}

@Composable
private fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = UthTheme.extendedColors.textSecondary,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun Bullets(items: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(top = UthDimens.Spacing.Sm)
                        .size(UthDimens.Spacing.Xs)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UthTheme.extendedColors.textSecondary,
                    modifier = Modifier.padding(start = UthDimens.Spacing.Md),
                )
            }
        }
    }
}

@Composable
private fun Milestones(items: List<Milestone>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        items.forEach { milestone -> MilestoneCard(milestone = milestone) }
    }
}

/** Một mốc thời gian: chỉ mốc đang diễn ra mới dùng nền + badge nhấn. */
@Composable
private fun MilestoneCard(milestone: Milestone, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val extended = UthTheme.extendedColors
    val badge = milestone.badge

    UthCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(UthDimens.Size.Avatar)
                    .background(
                        color = if (milestone.emphasized) colors.primaryContainer else extended.surfaceSubtle,
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = if (milestone.emphasized) colors.primary else extended.textSecondary,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = UthDimens.Spacing.Md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = milestone.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (badge != null) {
                        UthStatusBadge(
                            text = badge,
                            tone = if (milestone.emphasized) BadgeTone.Accent else BadgeTone.Neutral,
                            icon = null,
                            modifier = Modifier.padding(start = UthDimens.Spacing.Sm),
                        )
                    }
                }
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = milestone.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                )
            }
        }
    }
}

/** Khối liên hệ: nhãn + nội dung + email. Email chưa bấm được (chưa có backend gửi thư). */
@Composable
private fun Contact(block: AnnouncementBlock.Contact, modifier: Modifier = Modifier) {
    val extended = UthTheme.extendedColors

    UthCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Text(
                text = block.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = UthDimens.Spacing.Sm),
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = block.body,
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
        )
        block.email?.let { email ->
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = email,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Mục "Tài liệu đính kèm (n)" — ẩn hoàn toàn khi bài không có tệp. */
@Composable
private fun AnnouncementAttachments(
    attachments: List<AnnouncementAttachment>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeading(
            text = stringResource(R.string.announcement_attachments_title, attachments.size),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            attachments.forEach { attachment -> AttachmentRow(attachment = attachment) }
        }
    }
}

/**
 * Một tệp đính kèm. Nút tải hiện sẵn nhưng **chưa bấm được** — sẽ nối Firebase Storage
 * ở M0, nên icon tải về đang là phần trang trí (`contentDescription = null`), không phải
 * nút giả.
 */
@Composable
private fun AttachmentRow(attachment: AnnouncementAttachment, modifier: Modifier = Modifier) {
    val extended = UthTheme.extendedColors
    val kindLabel = attachmentKindLabel(attachment.kind)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(extended.surfaceSubtle)
            .padding(UthDimens.Spacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_announcement_attach),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = UthDimens.Spacing.Md),
        ) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.announcement_attachment_meta,
                    kindLabel,
                    attachment.sizeLabel,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = extended.textSecondary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_announcement_download),
            contentDescription = null,
            tint = extended.textSecondary,
            modifier = Modifier.size(UthDimens.Size.IconInline),
        )
    }
}

@Composable
private fun attachmentKindLabel(kind: AttachmentKind): String = stringResource(
    when (kind) {
        AttachmentKind.PDF -> R.string.announcement_attachment_kind_pdf
        AttachmentKind.DOCX -> R.string.announcement_attachment_kind_docx
        AttachmentKind.IMAGE -> R.string.announcement_attachment_kind_image
        AttachmentKind.OTHER -> R.string.announcement_attachment_kind_other
    },
)

@Composable
private fun AnnouncementLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UthDimens.Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = UthDimens.Size.ProgressStroke,
            modifier = Modifier.size(UthDimens.Size.Progress),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Text(
            text = stringResource(R.string.announcement_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = UthTheme.extendedColors.textSecondary,
        )
    }
}

/** enum → chuỗi đặt ở UI: ViewModel không được cầm `@StringRes`. */
@Composable
private fun AnnouncementError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(modifier = modifier) {
        UthEmptyState(
            title = stringResource(R.string.announcement_error_title),
            body = stringResource(R.string.announcement_error_body),
            icon = Icons.Filled.Warning,
            actionLabel = stringResource(R.string.common_retry),
            onAction = onRetry,
        )
    }
}

/**
 * Bài bị gỡ hoặc đường dẫn sai. Không có CTA riêng: lối thoát là nút quay lại trên thanh
 * trên (CTA duy nhất của `UthEmptyState` luôn gắn icon "làm mới", không đúng nghĩa với
 * trường hợp này).
 */
@Composable
private fun AnnouncementMissing(modifier: Modifier = Modifier) {
    CenteredState(modifier = modifier) {
        UthEmptyState(
            title = stringResource(R.string.announcement_missing_title),
            body = stringResource(R.string.announcement_missing_body),
            icon = Icons.Filled.Info,
        )
    }
}

@Composable
private fun CenteredState(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = UthDimens.Spacing.Xl),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Khoá item của `LazyColumn` — không rải chuỗi thô trong code. */
private object AnnouncementKey {
    const val Header = "announcement_header"
    const val Cover = "announcement_cover"
    const val Lead = "announcement_lead"
    const val Attachments = "announcement_attachments"

    /** Tiền tố key cho từng khối nội dung; số thứ tự nối vào sau. */
    const val BlockPrefix = "announcement_block_"
}

/** Ảnh bìa 16:9 theo mục 3.3. */
private const val CoverAspectRatio = 16f / 9f

/** Chip trên ảnh bìa gần đặc để không phụ thuộc độ sáng của ảnh nền. */
private const val CoverChipAlpha = 0.92f

/** Định dạng ngày theo locale trường: `dd/MM/yyyy` (mục 8). */
private val DetailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Nhãn nhóm: dùng chung với bảng tin vì cùng một `AnnouncementCategory`. */
@StringRes
private fun AnnouncementCategory.labelRes(): Int = when (this) {
    AnnouncementCategory.TRAINING -> R.string.home_category_training
    AnnouncementCategory.STUDENT_AFFAIRS -> R.string.home_category_student_affairs
    AnnouncementCategory.TUITION -> R.string.home_category_tuition
    AnnouncementCategory.YOUTH_UNION -> R.string.home_category_youth_union
}

/** Icon đại diện khi chưa có ảnh bìa thật; bộ `material-icons-core` đã đóng băng (mục 7). */
private fun AnnouncementCategory.icon(): ImageVector = when (this) {
    AnnouncementCategory.TRAINING -> Icons.Filled.DateRange
    AnnouncementCategory.STUDENT_AFFAIRS -> Icons.Filled.Person
    AnnouncementCategory.TUITION -> Icons.Filled.ShoppingCart
    AnnouncementCategory.YOUTH_UNION -> Icons.Filled.Star
}

/** Bài mẫu đầy đủ: mốc thời gian, gạch đầu dòng, liên hệ và hai tệp đính kèm. */
private val PreviewDetail = AnnouncementDetail(
    id = "preview-detail",
    category = AnnouncementCategory.TRAINING,
    title = "Kế hoạch đăng ký học phần học kỳ mới & lịch thi bổ sung",
    publisher = "Phòng Đào tạo",
    publishedAt = LocalDateTime.of(2026, 10, 28, 9, 0),
    source = "Cổng thông tin Đào tạo UTH",
    updatedAt = LocalDateTime.of(2026, 10, 29, 8, 30),
    lead = "Cổng đăng ký học phần mở từ 08:00 ngày 01/11/2026 cho sinh viên khóa 2023 - 2025.",
    blocks = listOf(
        AnnouncementBlock.Heading("Khung thời gian cụ thể"),
        AnnouncementBlock.Milestones(
            listOf(
                Milestone(
                    title = "Mở cổng đăng ký",
                    body = "Từ 08:00 ngày 01/11/2026 đến 17:00 ngày 05/11/2026.",
                    badge = "Đợt 1",
                    emphasized = true,
                ),
                Milestone(
                    title = "Điều chỉnh học phần",
                    body = "Từ 08:00 ngày 06/11/2026 đến 17:00 ngày 08/11/2026.",
                    badge = "Đợt 2",
                ),
            ),
        ),
        AnnouncementBlock.Heading("Hướng dẫn kỹ thuật"),
        AnnouncementBlock.Bullets(
            listOf(
                "Đăng nhập cổng sinh viên bằng tài khoản UTH.",
                "Chọn đúng học kỳ và cơ sở trước khi thêm học phần.",
            ),
        ),
        AnnouncementBlock.Contact(
            label = "Kênh hỗ trợ trực tuyến",
            body = "Trong giờ hành chính, gửi yêu cầu kèm mã số sinh viên.",
            email = "daotao@uth.edu.vn",
        ),
    ),
    attachments = listOf(
        AnnouncementAttachment("Ke-hoach-dang-ky-hoc-phan.pdf", "2.4 MB", AttachmentKind.PDF),
        AnnouncementAttachment("Mau-phieu-dieu-chinh.docx", "340 KB", AttachmentKind.DOCX),
    ),
)

/** Bài mẫu ngắn: chỉ tiêu đề + đoạn mở đầu, để thấy layout không giả định mục nào. */
private val PreviewShortDetail = AnnouncementDetail(
    id = "preview-short",
    category = AnnouncementCategory.YOUTH_UNION,
    title = "Hoạt động sắp diễn ra: Hội thao & Ngày hội CLB",
    publisher = "Đoàn Trường UTH",
    publishedAt = LocalDateTime.of(2026, 10, 24, 11, 0),
    source = "Cộng điểm ĐRL",
    lead = "Ghi nhận điểm rèn luyện cho sinh viên tham gia cổ vũ và thi đấu các bộ môn.",
)

@Composable
private fun AnnouncementStatePreview(
    state: AnnouncementUiState,
    darkTheme: Boolean = false,
) = UthPreview(darkTheme = darkTheme) {
    AnnouncementScreen(
        uiState = state,
        onAction = {},
        onBack = {},
        onShare = {},
    )
}

@Preview(name = "content light", showBackground = true)
@Composable
private fun AnnouncementContentPreview() =
    AnnouncementStatePreview(AnnouncementUiState(isLoading = false, detail = PreviewDetail))

@Preview(name = "content dark", showBackground = true)
@Composable
private fun AnnouncementContentDarkPreview() = AnnouncementStatePreview(
    AnnouncementUiState(isLoading = false, detail = PreviewDetail),
    darkTheme = true,
)

/** Bài ngắn: đúng dữ liệu thật của bảng tin (chỉ tóm tắt), không thêm mục rỗng. */
@Preview(name = "short content", showBackground = true)
@Composable
private fun AnnouncementShortContentPreview() =
    AnnouncementStatePreview(AnnouncementUiState(isLoading = false, detail = PreviewShortDetail))

@Preview(name = "loading light", showBackground = true)
@Composable
private fun AnnouncementLoadingPreview() = AnnouncementStatePreview(AnnouncementUiState())

@Preview(name = "loading dark", showBackground = true)
@Composable
private fun AnnouncementLoadingDarkPreview() =
    AnnouncementStatePreview(AnnouncementUiState(), darkTheme = true)

@Preview(name = "error light", showBackground = true)
@Composable
private fun AnnouncementErrorPreview() = AnnouncementStatePreview(
    AnnouncementUiState(isLoading = false, failure = AnnouncementFailure.LoadFailed),
)

@Preview(name = "error dark", showBackground = true)
@Composable
private fun AnnouncementErrorDarkPreview() = AnnouncementStatePreview(
    AnnouncementUiState(isLoading = false, failure = AnnouncementFailure.LoadFailed),
    darkTheme = true,
)

@Preview(name = "missing light", showBackground = true)
@Composable
private fun AnnouncementMissingPreview() = AnnouncementStatePreview(AnnouncementUiState(isLoading = false))

@Preview(name = "missing dark", showBackground = true)
@Composable
private fun AnnouncementMissingDarkPreview() = AnnouncementStatePreview(
    AnnouncementUiState(isLoading = false),
    darkTheme = true,
)

/** Font scale 130 % — kiểm tra tiêu đề dài và dòng meta không bị cắt (`Design-system` mục 8). */
@Preview(name = "content fontScale 130%", showBackground = true, fontScale = 1.3f)
@Composable
private fun AnnouncementContentFontScalePreview() =
    AnnouncementStatePreview(AnnouncementUiState(isLoading = false, detail = PreviewDetail))
