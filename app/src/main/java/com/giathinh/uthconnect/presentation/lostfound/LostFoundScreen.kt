package com.giathinh.uthconnect.presentation.lostfound

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthBrandTopBar
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthContentSkeleton
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import kotlinx.coroutines.launch

/**
 * Main screen for the Lost & Found feature following UTH Design System and accessibility standards.
 */
@Composable
fun LostFoundScreen(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onNavigateBack: () -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var detailListingId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeDetail = detailListingId?.let { id ->
        uiState.listings.find { it.id == id }
    }

    val messageText = uiState.userMessage?.let { stringResource(it.messageRes) }
    LaunchedEffect(uiState.userMessage) {
        if (uiState.userMessage != null && messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onAction(LostFoundAction.DismissMessage)
        }
    }

    val phoneCopiedText = stringResource(R.string.lostfound_phone_copied)
    val copyPhone: (String) -> Unit = { phone ->
        copyPhoneToClipboard(context, phone)
        coroutineScope.launch { snackbarHostState.showSnackbar(phoneCopiedText) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            UthBrandTopBar(
                title = stringResource(R.string.lostfound_title),
                onNavigateBack = onNavigateBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(LostFoundAction.OpenReportDialog) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(
                        text = stringResource(R.string.lostfound_post_action),
                        fontWeight = FontWeight.Bold,
                    )
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LostFoundTopControls(
                uiState = uiState,
                onAction = onAction,
            )

            LostFoundBody(
                uiState = uiState,
                onAction = onAction,
                onCallClick = onCallClick,
                onOpenDetail = { detailListingId = it.id },
                modifier = Modifier.weight(1f),
            )
        }
    }

    activeDetail?.let { listing ->
        val isOwner = listing.userId != null && listing.userId == uiState.currentUserId
        LostFoundDetailBottomSheet(
            listing = listing,
            isOwner = isOwner,
            isDeleting = listing.id in uiState.deletingIds,
            onDismiss = { detailListingId = null },
            onCallClick = onCallClick,
            onCopyPhone = copyPhone,
            onToggleResolve = { onAction(LostFoundAction.ToggleResolveListing(listing.id)) },
            onEditClick = {
                detailListingId = null
                onAction(LostFoundAction.OpenEditDialog(listing))
            },
            onDeleteClick = {
                detailListingId = null
                onAction(LostFoundAction.RequestDeleteListing(listing))
            },
        )
    }

    uiState.pendingDeleteListing?.let { pending ->
        DeleteLostFoundDialog(
            listing = pending,
            onDismiss = { onAction(LostFoundAction.CancelDeleteListing) },
            onConfirm = { onAction(LostFoundAction.ConfirmDeleteListing) },
        )
    }

    if (uiState.isReportDialogOpen) {
        ReportItemBottomSheet(
            form = uiState.reportForm,
            onAction = onAction,
            onDismiss = { onAction(LostFoundAction.CloseReportDialog) },
        )
    }
}

@Composable
private fun LostFoundTopControls(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UthDimens.Spacing.Sm),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        LostFoundSearchBar(
            query = uiState.searchQuery,
            selectedKind = uiState.selectedKind,
            onQueryChange = { onAction(LostFoundAction.UpdateSearchQuery(it)) },
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
        )

        KindSegmentedRow(
            selectedKind = uiState.selectedKind,
            lostCount = uiState.lostCount,
            foundCount = uiState.foundCount,
            onSelectKind = { onAction(LostFoundAction.SelectKind(it)) },
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            if (uiState.currentUserId != null) {
                item {
                    FilterChip(
                        selected = uiState.onlyMine,
                        onClick = { onAction(LostFoundAction.SetOnlyMine(!uiState.onlyMine)) },
                        leadingIcon = if (uiState.onlyMine) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                                )
                            }
                        } else {
                            null
                        },
                        label = { Text(stringResource(R.string.lostfound_filter_only_mine)) },
                    )
                }
            }

            item {
                FilterChip(
                    selected = !uiState.showResolved,
                    onClick = { onAction(LostFoundAction.SetShowResolved(!uiState.showResolved)) },
                    leadingIcon = if (!uiState.showResolved) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(UthDimens.Size.IconCompact),
                            )
                        }
                    } else {
                        null
                    },
                    label = { Text(stringResource(R.string.lostfound_filter_hide_resolved)) },
                )
            }

            items(ItemCategory.entries) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onAction(LostFoundAction.SelectCategory(category)) },
                    label = { Text(stringResource(category.labelRes())) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LostFoundBody(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onCallClick: (String) -> Unit,
    onOpenDetail: (LostFoundListing) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading && uiState.listings.isEmpty()) {
        UthContentSkeleton(
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { onAction(LostFoundAction.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isError -> {
                UthEmptyState(
                    title = stringResource(R.string.lostfound_load_error_title),
                    body = stringResource(R.string.lostfound_load_error_desc),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(LostFoundAction.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.isEmpty -> {
                val emptyTitle = if (uiState.searchQuery.isNotBlank()) {
                    stringResource(R.string.lostfound_no_results_title)
                } else {
                    stringResource(R.string.lostfound_empty_list)
                }
                val emptyBody = if (uiState.searchQuery.isNotBlank()) {
                    stringResource(R.string.lostfound_no_results_desc, uiState.searchQuery)
                } else {
                    stringResource(R.string.lostfound_empty_desc)
                }
                val emptyActionLabel = if (uiState.hasActiveFilters) {
                    stringResource(R.string.lostfound_reset_filters)
                } else {
                    stringResource(R.string.lostfound_post_action)
                }
                val emptyOnAction: () -> Unit = if (uiState.hasActiveFilters) {
                    { onAction(LostFoundAction.ResetFilters) }
                } else {
                    { onAction(LostFoundAction.OpenReportDialog) }
                }
                UthEmptyState(
                    title = emptyTitle,
                    body = emptyBody,
                    actionLabel = emptyActionLabel,
                    onAction = emptyOnAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = UthDimens.Spacing.Lg,
                        end = UthDimens.Spacing.Lg,
                        top = UthDimens.Spacing.Sm,
                        bottom = UthDimens.Size.FabBottomClearance,
                    ),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                ) {
                    items(
                        items = uiState.filteredListings,
                        key = { it.id },
                    ) { item ->
                        val isOwner = item.userId != null && item.userId == uiState.currentUserId
                        LostFoundItemCard(
                            listing = item,
                            isOwner = isOwner,
                            isDeleting = item.id in uiState.deletingIds,
                            onOpenDetail = { onOpenDetail(item) },
                            onToggleResolve = { onAction(LostFoundAction.ToggleResolveListing(item.id)) },
                            onEditClick = { onAction(LostFoundAction.OpenEditDialog(item)) },
                            onDeleteClick = { onAction(LostFoundAction.RequestDeleteListing(item)) },
                            onCallClick = { onCallClick(item.contactPhone) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LostFoundSearchBar(
    query: String,
    selectedKind: ListingKind,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hintRes = if (selectedKind == ListingKind.LOST) {
        R.string.lostfound_search_hint_lost
    } else {
        R.string.lostfound_search_hint_found
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(hintRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.lostfound_clear_search),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun KindSegmentedRow(
    selectedKind: ListingKind,
    lostCount: Int,
    foundCount: Int,
    onSelectKind: (ListingKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        ListingKind.LOST to stringResource(R.string.lostfound_tab_lost, lostCount),
        ListingKind.FOUND to stringResource(R.string.lostfound_tab_found, foundCount),
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        shape = UthDimens.Radius.PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UthDimens.Spacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            tabs.forEach { (kind, label) ->
                val selected = selectedKind == kind
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.SegmentedControlHeight)
                        .clip(UthDimens.Radius.PillShape)
                        .selectable(
                            selected = selected,
                            onClick = { onSelectKind(kind) },
                            role = Role.Tab,
                        ),
                    shape = UthDimens.Radius.PillShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shadowElevation = if (selected) UthDimens.Elevation.Card else UthDimens.Elevation.None,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(
                            horizontal = UthDimens.Spacing.Sm,
                            vertical = UthDimens.Spacing.Sm,
                        ),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LostFoundItemCard(
    listing: LostFoundListing,
    isOwner: Boolean,
    isDeleting: Boolean,
    onOpenDetail: () -> Unit,
    onToggleResolve: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit,
) {
    UthCard(onClick = onOpenDetail) {
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            ItemCardHeader(listing = listing)

            if (listing.imageUrl.isTrustedLostFoundImageUrl()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(UthDimens.Layout.CoverAspectRatio)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (listing.contactName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.lostfound_poster_prefix, listing.contactName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            ItemCardActions(
                listing = listing,
                isOwner = isOwner,
                isDeleting = isDeleting,
                onToggleResolve = onToggleResolve,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onCallClick = onCallClick,
            )
        }
    }
}

@Composable
private fun ItemCardHeader(listing: LostFoundListing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
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
        }
        Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
        Text(
            text = listing.date,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ItemCardActions(
    listing: LostFoundListing,
    isOwner: Boolean,
    isDeleting: Boolean,
    onToggleResolve: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        if (isOwner) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onToggleResolve,
                    enabled = !isDeleting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(UthDimens.Size.IconCompact),
                    )
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                    Text(
                        text = stringResource(
                            if (listing.kind == ListingKind.LOST) {
                                R.string.lostfound_action_mark_lost
                            } else {
                                R.string.lostfound_action_mark_found
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onEditClick,
                    enabled = !isDeleting,
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.lostfound_action_edit),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.lostfound_action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!listing.isResolved && listing.contactPhone.isNotBlank()) {
            Button(
                onClick = onCallClick,
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
    }
}

private fun copyPhoneToClipboard(context: Context, phone: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("phone", phone))
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true)
@Composable
private fun LostFoundScreenPreview() {
    UthPreview {
        LostFoundScreen(
            uiState = LostFoundUiState(
                currentUserId = "owner-1",
                listings = listOf(
                    LostFoundListing(
                        id = "1",
                        title = "Thẻ sinh viên UTH",
                        description = "Rơi gần khu vực thư viện CS1 vào buổi sáng",
                        kind = ListingKind.LOST,
                        category = ItemCategory.STUDENT_CARD,
                        location = "Thư viện CS1",
                        date = "26/11/2026",
                        contactName = "Nguyễn Văn A",
                        contactPhone = "0912345678",
                        userId = "owner-1",
                    ),
                ),
            ),
            onAction = {},
            onNavigateBack = {},
            onCallClick = {},
        )
    }
}
