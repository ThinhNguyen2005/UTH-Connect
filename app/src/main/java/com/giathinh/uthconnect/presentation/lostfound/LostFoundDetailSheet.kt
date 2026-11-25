package com.giathinh.uthconnect.presentation.lostfound

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import java.net.URL

@StringRes
internal fun ItemCategory.labelRes(): Int = when (this) {
    ItemCategory.ALL -> R.string.lostfound_filter_all
    ItemCategory.STUDENT_CARD -> R.string.lostfound_filter_student_card
    ItemCategory.ELECTRONICS -> R.string.lostfound_filter_electronics
    ItemCategory.WALLET_PAPERS -> R.string.lostfound_filter_wallet_papers
    ItemCategory.PERSONAL_ITEMS -> R.string.lostfound_filter_personal_items
    ItemCategory.OTHER -> R.string.lostfound_filter_other
}

internal fun String?.isTrustedLostFoundImageUrl(): Boolean {
    if (this.isNullOrBlank()) return false
    val supabasePrefix = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/lost-found/"
    return startsWith(supabasePrefix) || runCatching { URL(this) }.getOrNull()?.let {
        it.protocol == "https" && it.host == "images.unsplash.com"
    } == true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LostFoundDetailBottomSheet(
    listing: LostFoundListing,
    isOwner: Boolean,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onCallClick: (String) -> Unit,
    onCopyPhone: (String) -> Unit,
    onToggleResolve: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = UthDimens.Spacing.Xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            DetailSheetHeader(listing = listing, onDismiss = onDismiss)

            if (listing.imageUrl.isTrustedLostFoundImageUrl()) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(UthDimens.Layout.CoverAspectRatio)
                        .clip(MaterialTheme.shapes.medium),
                )
            }

            if (listing.description.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailMetadataSection(
                listing = listing,
                onCopyPhone = onCopyPhone,
            )

            DetailActionSection(
                listing = listing,
                isOwner = isOwner,
                isDeleting = isDeleting,
                onCallClick = onCallClick,
                onToggleResolve = onToggleResolve,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
            )

            Spacer(modifier = Modifier.height(UthDimens.Spacing.Lg))
        }
    }
}

@Composable
private fun DetailSheetHeader(
    listing: LostFoundListing,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.lostfound_detail_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.lostfound_btn_close),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UthStatusBadge(
                text = stringResource(
                    if (listing.kind == ListingKind.LOST) {
                        R.string.lostfound_badge_lost
                    } else {
                        R.string.lostfound_badge_found
                    },
                ),
                tone = if (listing.kind == ListingKind.LOST) BadgeTone.Danger else BadgeTone.Info,
                icon = null,
            )
            if (listing.isResolved) {
                UthStatusBadge(
                    text = stringResource(
                        if (listing.kind == ListingKind.LOST) {
                            R.string.lostfound_status_resolved_lost
                        } else {
                            R.string.lostfound_status_resolved_found
                        },
                    ),
                    tone = BadgeTone.Success,
                    icon = Icons.Filled.CheckCircle,
                )
            }
            Text(
                text = listing.date,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailMetadataSection(
    listing: LostFoundListing,
    onCopyPhone: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        Text(
            text = stringResource(listing.category.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(
                    R.string.lostfound_poster_prefix,
                    listing.contactName.ifBlank { stringResource(R.string.lostfound_contact_unnamed) },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (listing.contactPhone.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionContainer {
                    Text(
                        text = listing.contactPhone,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(
                    onClick = { onCopyPhone(listing.contactPhone) },
                    modifier = Modifier.heightIn(min = UthDimens.Size.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.lostfound_copy_phone))
                }
            }
        }
    }
}

@Composable
private fun DetailActionSection(
    listing: LostFoundListing,
    isOwner: Boolean,
    isDeleting: Boolean,
    onCallClick: (String) -> Unit,
    onToggleResolve: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        if (!listing.isResolved && listing.contactPhone.isNotBlank()) {
            Button(
                onClick = { onCallClick(listing.contactPhone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = UthDimens.Size.ButtonHeight),
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                Text(stringResource(R.string.lostfound_call_contact))
            }
        }

        if (isOwner) {
            FilledTonalButton(
                onClick = onToggleResolve,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = UthDimens.Size.ButtonHeight),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                Text(
                    text = stringResource(
                        if (listing.kind == ListingKind.LOST) {
                            R.string.lostfound_action_mark_lost
                        } else {
                            R.string.lostfound_action_mark_found
                        },
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    enabled = !isDeleting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                    Text(stringResource(R.string.lostfound_action_edit))
                }

                TextButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                    Text(stringResource(R.string.lostfound_action_delete))
                }
            }
        }
    }
}

@Composable
internal fun DeleteLostFoundDialog(
    listing: LostFoundListing,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.lostfound_delete_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.lostfound_delete_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.lostfound_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.lostfound_btn_cancel))
            }
        },
    )
}
