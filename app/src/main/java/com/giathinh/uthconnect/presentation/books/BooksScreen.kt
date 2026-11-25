package com.giathinh.uthconnect.presentation.books

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun BooksScreen(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var newListingId by rememberSaveable { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<BookListing?>(null) }
    var pendingDelete by remember { mutableStateOf<BookListing?>(null) }
    var savePending by rememberSaveable { mutableStateOf(false) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var showContactError by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val editingListing = remember(editingId, uiState.listings) {
        editingId?.let { id -> uiState.listings.find { it.id == id } }
    }

    LaunchedEffect(uiState.lastSavedId, uiState.isSaving, uiState.saveFailure) {
        if (savePending && !uiState.isSaving) {
            val activeId = editingId ?: newListingId
            if (uiState.saveFailure == null && uiState.lastSavedId == activeId) {
                showForm = false
                editingId = null
                newListingId = null
                saveFailed = false
            } else if (uiState.saveFailure != null) {
                saveFailed = true
            }
            savePending = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            UthBrandTopBar(
                title = stringResource(R.string.books_title),
                subtitle = stringResource(R.string.books_header_subtitle),
                onNavigateBack = onBack,
            )
        },
        floatingActionButton = {
            if (uiState.currentUserUid != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingId = null
                        newListingId = UUID.randomUUID().toString()
                        saveFailed = false
                        showForm = true
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.books_create), fontWeight = FontWeight.Bold) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                top = UthDimens.Spacing.Md,
                bottom = UthDimens.Size.FabBottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            item {
                BooksSearchAndQuickFilters(
                    uiState = uiState,
                    onAction = onAction,
                    onOpenFilterSheet = { showFilterSheet = true },
                )
            }

            if (uiState.isLoading && uiState.listings.isEmpty()) {
                item { UthContentSkeleton() }
            } else if (uiState.saveFailure != null && !showForm) {
                item {
                    BooksSaveFailureBanner(
                        failure = uiState.saveFailure,
                        onDismiss = { onAction(BooksAction.DismissError) },
                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
                    )
                }
            }

            if (!uiState.isLoading && uiState.loadFailure != null) {
                item {
                    val isAuthRequired = uiState.currentUserUid == null
                    UthEmptyState(
                        title = stringResource(
                            if (isAuthRequired) R.string.books_auth_required_title else R.string.books_error_title,
                        ),
                        body = stringResource(
                            if (isAuthRequired) R.string.books_auth_required_body else R.string.books_error_body,
                        ),
                        icon = Icons.Filled.Warning,
                        actionLabel = if (isAuthRequired) null else stringResource(R.string.common_retry),
                        onAction = if (isAuthRequired) {
                            null
                        } else {
                            { onAction(BooksAction.Retry) }
                        },
                    )
                }
            } else if (uiState.visibleListings.isEmpty() && (!uiState.isLoading || uiState.listings.isNotEmpty())) {
                item {
                    UthEmptyState(
                        title = stringResource(
                            if (uiState.listings.isEmpty()) {
                                R.string.books_empty_title
                            } else {
                                R.string.books_no_results_title
                            },
                        ),
                        body = stringResource(
                            if (uiState.listings.isEmpty()) {
                                R.string.books_empty_body
                            } else {
                                R.string.books_no_results_body
                            },
                        ),
                        actionLabel = if (uiState.listings.isEmpty()) {
                            null
                        } else {
                            stringResource(R.string.books_clear_filters)
                        },
                        onAction = if (uiState.listings.isEmpty()) {
                            null
                        } else {
                            { onAction(BooksAction.ResetFilters) }
                        },
                    )
                }
            } else if (uiState.visibleListings.isNotEmpty()) {
                items(uiState.visibleListings, key = { it.id }) { listing ->
                    ListingCard(
                        listing = listing,
                        canManage = listing.ownerUid == uiState.currentUserUid,
                        isMutating = listing.id in uiState.mutatingIds,
                        onOpen = { details = listing },
                        onEdit = {
                            editingId = listing.id
                            newListingId = null
                            saveFailed = false
                            showForm = true
                        },
                        onDelete = { pendingDelete = listing },
                        onSetStatus = { status -> onAction(BooksAction.SetStatus(listing, status)) },
                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        BooksFilterBottomSheet(
            uiState = uiState,
            onAction = onAction,
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showForm) {
        BooksFormHost(
            editingId = editingId,
            editingListing = editingListing,
            newListingId = newListingId,
            uiState = uiState,
            saveFailed = saveFailed,
            onUpdateNewListingId = { newListingId = it },
            onDismiss = {
                showForm = false
                editingId = null
                newListingId = null
                savePending = false
                saveFailed = false
            },
            onSave = { listing ->
                savePending = true
                saveFailed = false
                onAction(BooksAction.Save(listing))
            },
        )
    }

    details?.let { listing ->
        val uriHandler = LocalUriHandler.current
        BookDetailBottomSheet(
            listing = listing,
            onDismiss = { details = null },
            onCopyContact = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("book_contact", listing.contact))
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.books_contact_copied))
                }
            },
            onContact = {
                val contact = listing.contact.trim()
                runCatching {
                    val uri = if ('@' in contact) {
                        "mailto:${Uri.encode(contact)}"
                    } else {
                        "tel:${Uri.encode(contact)}"
                    }
                    uriHandler.openUri(uri)
                }.onSuccess { details = null }.onFailure { showContactError = true }
            },
        )
    }

    if (showContactError) {
        AlertDialog(
            onDismissRequest = { showContactError = false },
            title = { Text(stringResource(R.string.books_contact_failed_title)) },
            text = { Text(stringResource(R.string.books_contact_failed_body)) },
            confirmButton = {
                TextButton(onClick = { showContactError = false }) {
                    Text(stringResource(R.string.books_close))
                }
            },
        )
    }

    pendingDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.books_delete_title)) },
            text = { Text(stringResource(R.string.books_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(BooksAction.Delete(listing.id))
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.books_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.books_cancel))
                }
            },
        )
    }
}

@Composable
private fun BooksSearchAndQuickFilters(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    onOpenFilterSheet: () -> Unit,
) {
    val hasAnyFilter = uiState.activeFilterCount > 0 ||
        uiState.onlyMine ||
        uiState.filter != BooksFilter.ALL ||
        uiState.query.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onAction(BooksAction.Search(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.books_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onAction(BooksAction.Search("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.books_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            FilledTonalButton(
                onClick = onOpenFilterSheet,
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Md),
                modifier = Modifier.heightIn(min = UthDimens.Size.ButtonHeight),
            ) {
                Text(
                    text = if (uiState.activeFilterCount > 0) {
                        stringResource(R.string.books_filter_button_count, uiState.activeFilterCount)
                    } else {
                        stringResource(R.string.books_filter_button)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(BooksFilter.entries) { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { onAction(BooksAction.SetFilter(filter)) },
                    label = {
                        Text(
                            stringResource(
                                when (filter) {
                                    BooksFilter.ALL -> R.string.books_filter_all
                                    BooksFilter.SALE -> R.string.books_filter_sale
                                    BooksFilter.EXCHANGE -> R.string.books_filter_exchange
                                },
                            ),
                        )
                    },
                )
            }
            if (uiState.currentUserUid != null) {
                item {
                    FilterChip(
                        selected = uiState.onlyMine,
                        onClick = { onAction(BooksAction.SetOnlyMine(!uiState.onlyMine)) },
                        label = { Text(stringResource(R.string.books_filter_only_mine)) },
                    )
                }
            }
            if (hasAnyFilter) {
                item {
                    TextButton(
                        onClick = { onAction(BooksAction.ResetFilters) },
                        modifier = Modifier.defaultMinSize(minHeight = UthDimens.Size.MinTouchTarget, minWidth = 0.dp),
                    ) {
                        Text(stringResource(R.string.books_clear_filters))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.books_result_count, uiState.visibleListings.size),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
        )
    }
}

@Composable
private fun BooksSaveFailureBanner(
    failure: BooksFailure,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    when (failure) {
                        BooksFailure.DELETE_FAILED -> R.string.books_delete_error
                        BooksFailure.UPDATE_STATUS_FAILED -> R.string.books_status_error
                        else -> R.string.books_save_error
                    },
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.books_close),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: BookListing,
    canManage: Boolean,
    isMutating: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetStatus: (BookListingStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
            if (listing.photoUrl != null) {
                AsyncImage(
                    model = listing.photoUrl,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UthDimens.Size.BookCardImageHeight)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UthDimens.Size.BookPlaceholderHeight)
                        .clearAndSetSemantics { },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.home_logo_abbr),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(UthDimens.Spacing.Md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listing.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
                UthStatusBadge(
                    text = stringResource(
                        if (listing.status == BookListingStatus.AVAILABLE) {
                            R.string.books_available
                        } else {
                            R.string.books_sold
                        },
                    ),
                    tone = if (listing.status == BookListingStatus.AVAILABLE) BadgeTone.Success else BadgeTone.Neutral,
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = if (listing.dealType == BookDealType.SALE) {
                    formatBookPrice(listing.priceVnd)
                } else {
                    stringResource(R.string.books_exchange)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.books_condition_format, stringResource(listing.condition.labelRes())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canManage) {
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = !isMutating,
                    onClick = {
                        val target = if (listing.status == BookListingStatus.AVAILABLE) {
                            BookListingStatus.SOLD
                        } else {
                            BookListingStatus.AVAILABLE
                        }
                        onSetStatus(target)
                    },
                    modifier = Modifier.defaultMinSize(minHeight = UthDimens.Size.MinTouchTarget, minWidth = 0.dp),
                ) {
                    Text(
                        stringResource(
                            if (listing.status == BookListingStatus.AVAILABLE) {
                                R.string.books_mark_sold
                            } else {
                                R.string.books_mark_available
                            },
                        ),
                    )
                }
                if (isMutating) {
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    CircularProgressIndicator(
                        modifier = Modifier.size(UthDimens.Size.ProgressInline),
                        strokeWidth = UthDimens.Size.ProgressInlineStroke,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    IconButton(
                        enabled = !isMutating,
                        onClick = onEdit,
                        modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.books_edit))
                    }
                    IconButton(
                        enabled = !isMutating,
                        onClick = onDelete,
                        modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.books_delete),
                            tint = if (isMutating) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BooksScreenPreview() = UthPreview { BooksScreen(BooksUiState(isLoading = false), {}, {}) }
