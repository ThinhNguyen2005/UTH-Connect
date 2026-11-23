package com.giathinh.uthconnect.presentation.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventComment
import com.giathinh.uthconnect.domain.model.StudentProfile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDiscussionBottomSheet(
    event: Event,
    comments: List<EventComment>,
    commentDraft: String,
    isPosting: Boolean,
    onDiscussionAction: (EventDiscussionAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onDiscussionAction(EventDiscussionAction.Close) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg)
                .padding(bottom = UthDimens.Spacing.Lg)
                .imePadding(),
        ) {
            EventDiscussionHeader(event, onDismiss = { onDiscussionAction(EventDiscussionAction.Close) })
            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = UthDimens.Spacing.Sm))
            Text(
                text = stringResource(R.string.event_comments_title) + " (${comments.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            DiscussionCommentsList(
                comments = comments,
                onOpenGuestProfile = { onDiscussionAction(EventDiscussionAction.OpenGuestProfile(it)) },
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            CommentInputRow(
                commentDraft = commentDraft,
                isPosting = isPosting,
                onCommentDraftChange = { onDiscussionAction(EventDiscussionAction.DraftChange(it)) },
                onSendComment = { onDiscussionAction(EventDiscussionAction.Send(event.id)) },
            )
        }
    }
}

@Composable
private fun EventDiscussionHeader(event: Event, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = event.orgName.ifBlank { "Ban tổ chức UTH" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.event_close),
            )
        }
    }
}

@Composable
private fun DiscussionCommentsList(
    comments: List<EventComment>,
    onOpenGuestProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        if (comments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UthDimens.Spacing.Xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.event_comments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    onOpenGuestProfile = onOpenGuestProfile,
                )
            }
        }
    }
}

@Composable
private fun CommentInputRow(
    commentDraft: String,
    isPosting: Boolean,
    onCommentDraftChange: (String) -> Unit,
    onSendComment: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        OutlinedTextField(
            value = commentDraft,
            onValueChange = onCommentDraftChange,
            placeholder = { Text(stringResource(R.string.event_comment_placeholder)) },
            modifier = Modifier.weight(1f),
            maxLines = 3,
            singleLine = false,
            enabled = !isPosting,
        )
        IconButton(
            onClick = onSendComment,
            enabled = commentDraft.isNotBlank() && !isPosting,
            modifier = Modifier.background(
                if (commentDraft.isNotBlank() && !isPosting) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                CircleShape,
            ),
        ) {
            if (isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.event_comment_send),
                    tint = if (commentDraft.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: EventComment,
    onOpenGuestProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        CommentAvatar(
            name = comment.authorName,
            onClick = { onOpenGuestProfile(comment.authorStudentId) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(UthDimens.Spacing.Md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenGuestProfile(comment.authorStudentId) },
                )
                Text(
                    text = formatCommentTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val subtitle = listOf(comment.authorClass, comment.authorMajor)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CommentAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = UthDimens.Size.Avatar,
    onClick: (() -> Unit)? = null,
) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .takeLast(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val baseModifier = modifier
        .size(size)
        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
    val clickableModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier
    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "SV" },
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = if (size >= UthDimens.Size.AvatarLarge) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.labelMedium
            },
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun GuestProfileDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommentAvatar(
                    name = student.displayName,
                    size = UthDimens.Size.AvatarLarge,
                )
                Spacer(Modifier.width(UthDimens.Spacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val desc = listOf(student.studentId, student.className)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (student.major.isNotBlank()) {
                        Text(
                            text = student.major,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.profile_section_school),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                GuestProfileRow(stringResource(R.string.auth_profile_student_id), student.studentId)
                GuestProfileRow(stringResource(R.string.auth_profile_class), student.className)
                GuestProfileRow(stringResource(R.string.auth_profile_major), student.major)
                GuestProfileRow(stringResource(R.string.auth_profile_faculty), student.faculty)
                GuestProfileRow(stringResource(R.string.auth_profile_email), student.email)
                if (student.uthVerified) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            stringResource(R.string.auth_verified_demo_badge),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = UthDimens.Spacing.Sm,
                                vertical = UthDimens.Spacing.Xs,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(R.string.event_guest_profile_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.event_close))
            }
        },
    )
}

@Composable
private fun GuestProfileRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = UthDimens.Spacing.Xs),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L

private fun formatCommentTime(millis: Long): String {
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0)
    val minutes = diff / MILLIS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return when {
        minutes < 1 -> "Vừa xong"

        minutes < MINUTES_PER_HOUR -> "$minutes phút trước"

        hours < HOURS_PER_DAY -> "$hours giờ trước"

        days < DAYS_PER_WEEK -> "$days ngày trước"

        else -> Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
}
