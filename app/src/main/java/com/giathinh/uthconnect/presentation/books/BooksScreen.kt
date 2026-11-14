package com.giathinh.uthconnect.presentation.books

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private const val SEARCH_DEBOUNCE_MS = 300L
private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024
private const val BOOK_TITLE_MAX_LENGTH = 160
private const val BOOK_SUBJECT_MAX_LENGTH = 100
private const val BOOK_DESC_MAX_LENGTH = 2000
private const val BOOK_COVER_ASPECT_RATIO = 0.85f
private const val GRID_COLUMNS = 2

@Composable
fun BooksScreen(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRequireSignIn: () -> Unit = onBack,
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var newListingId by rememberSaveable { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<BookListing?>(null) }
    var pendingDelete by remember { mutableStateOf<BookListing?>(null) }
    var savePending by rememberSaveable { mutableStateOf(false) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var showContactError by rememberSaveable { mutableStateOf(false) }

    val editingListing = remember(editingId, uiState.listings) {
        editingId?.let { id -> uiState.listings.find { it.id == id } }
    }

    LaunchedEffect(uiState.lastSavedId, uiState.isSaving, uiState.saveFailure, uiState.saveStep) {
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
        topBar = {
            BooksPinnedTopBar(
                query = uiState.query,
                filter = uiState.filter,
                condition = uiState.condition,
                sort = uiState.sort,
                includeSold = uiState.includeSold,
                totalResults = uiState.visibleListings.size,
                onBack = onBack,
                onSearch = { onAction(BooksAction.Search(it)) },
                onFilterChange = { onAction(BooksAction.SetFilter(it)) },
                onConditionChange = { onAction(BooksAction.SetCondition(it)) },
                onSortChange = { onAction(BooksAction.SetSort(it)) },
                onToggleIncludeSold = { onAction(BooksAction.IncludeSold(!uiState.includeSold)) },
                onCreateListing = {
                    if (uiState.currentUserUid == null && uiState.currentProfile == null) {
                        onRequireSignIn()
                    } else {
                        editingId = null
                        newListingId = UUID.randomUUID().toString()
                        saveFailed = false
                        showForm = true
                    }
                },
                canCreate = uiState.currentUserUid != null || uiState.currentProfile != null,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(UthDimens.Size.ProgressInline))
                            Spacer(Modifier.width(UthDimens.Spacing.Sm))
                            Text(
                                stringResource(R.string.books_loading),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                uiState.loadFailure != null -> {
                    val authRequired = uiState.currentUserUid == null && uiState.currentProfile == null
                    UthEmptyState(
                        title = stringResource(
                            if (authRequired) R.string.books_auth_required_title else R.string.books_error_title,
                        ),
                        body = stringResource(
                            if (authRequired) R.string.books_auth_required_body else R.string.books_error_body,
                        ),
                        icon = Icons.Filled.Warning,
                        actionLabel = stringResource(if (authRequired) R.string.auth_sign_up else R.string.common_retry),
                        onAction = if (authRequired) {
                            onRequireSignIn
                        } else {
                            { onAction(BooksAction.Retry) }
                        },
                    )
                }

                uiState.visibleListings.isEmpty() -> {
                    val noBooks = uiState.listings.isEmpty()
                    UthEmptyState(
                        title = stringResource(if (noBooks) R.string.books_empty_title else R.string.books_no_results_title),
                        body = stringResource(if (noBooks) R.string.books_empty_body else R.string.books_no_results_body),
                        actionLabel = if (noBooks) null else stringResource(R.string.books_clear_filters),
                        onAction = if (noBooks) {
                            null
                        } else {
                            { onAction(BooksAction.ResetFilters) }
                        },
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = UthDimens.Spacing.Md,
                            end = UthDimens.Spacing.Md,
                            top = UthDimens.Spacing.Sm,
                            bottom = UthDimens.Spacing.Xl,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                    ) {
                        if (uiState.saveFailure != null && !showForm) {
                            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                                SaveFailureAlert(
                                    failure = uiState.saveFailure,
                                    onDismiss = { onAction(BooksAction.DismissError) },
                                )
                            }
                        }

                        items(uiState.visibleListings, key = { it.id }) { listing ->
                            val isOwner = listing.ownerUid.isNotBlank() &&
                                (
                                    listing.ownerUid == uiState.currentUserUid ||
                                        listing.ownerUid == uiState.currentProfile?.studentId
                                    )
                            BookGridCard(
                                listing = listing,
                                canManage = isOwner,
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
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        val stableFormId = editingId ?: (
            newListingId ?: rememberSaveable { UUID.randomUUID().toString() }
                .also { newListingId = it }
            )
        ListingFormDialog(
            initial = editingListing,
            formListingId = stableFormId,
            currentProfile = uiState.currentProfile,
            currentUserUid = uiState.currentUserUid,
            saving = uiState.isSaving,
            saveStep = uiState.saveStep,
            saveFailed = saveFailed,
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
            onRequireSignIn = onRequireSignIn,
        )
    }

    details?.let { listing ->
        BookDetailDialog(
            listing = listing,
            onDismiss = { details = null },
            onErrorOpeningContact = { showContactError = true },
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
                ) {
                    Text(stringResource(R.string.books_delete), color = MaterialTheme.colorScheme.error)
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
private fun BooksPinnedTopBar(
    query: String,
    filter: BooksFilter,
    condition: BookCondition?,
    sort: BooksSort,
    includeSold: Boolean,
    totalResults: Int,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onFilterChange: (BooksFilter) -> Unit,
    onConditionChange: (BookCondition?) -> Unit,
    onSortChange: (BooksSort) -> Unit,
    onToggleIncludeSold: () -> Unit,
    onCreateListing: () -> Unit,
    canCreate: Boolean,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.books_back),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.books_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.books_header_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                UthPrimaryButton(
                    text = stringResource(R.string.books_create),
                    onClick = onCreateListing,
                    enabled = canCreate,
                )
            }

            ShopeeStyleSearchBar(
                query = query,
                onSearch = onSearch,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BooksFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { onFilterChange(f) },
                        label = {
                            Text(
                                stringResource(
                                    when (f) {
                                        BooksFilter.ALL -> R.string.books_filter_all
                                        BooksFilter.SALE -> R.string.books_filter_sale
                                        BooksFilter.EXCHANGE -> R.string.books_filter_exchange
                                    },
                                ),
                            )
                        },
                    )
                }

                FilterChip(
                    selected = condition == null,
                    onClick = { onConditionChange(null) },
                    label = { Text(stringResource(R.string.books_condition_all)) },
                )
                BookCondition.entries.forEach { c ->
                    FilterChip(
                        selected = condition == c,
                        onClick = { onConditionChange(c) },
                        label = { Text(stringResource(c.labelRes)) },
                    )
                }

                BooksSort.entries.forEach { s ->
                    FilterChip(
                        selected = sort == s,
                        onClick = { onSortChange(s) },
                        label = {
                            Text(
                                stringResource(
                                    when (s) {
                                        BooksSort.NEWEST -> R.string.books_sort_newest
                                        BooksSort.PRICE_LOW -> R.string.books_sort_price_low
                                        BooksSort.PRICE_HIGH -> R.string.books_sort_price_high
                                    },
                                ),
                            )
                        },
                    )
                }

                FilterChip(
                    selected = includeSold,
                    onClick = onToggleIncludeSold,
                    label = { Text(stringResource(R.string.books_show_sold)) },
                )

                Spacer(Modifier.width(UthDimens.Spacing.Sm))
                Text(
                    stringResource(R.string.books_result_count, totalResults),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShopeeStyleSearchBar(
    query: String,
    onSearch: (String) -> Unit,
) {
    var textInput by rememberSaveable(query) { mutableStateOf(query) }

    LaunchedEffect(textInput) {
        if (textInput != query) {
            delay(SEARCH_DEBOUNCE_MS)
            onSearch(textInput)
        }
    }

    OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = {
            Text(
                stringResource(R.string.books_search),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingIcon = {
            if (textInput.isNotEmpty()) {
                IconButton(onClick = {
                    textInput = ""
                    onSearch("")
                }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.books_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun BookGridCard(
    listing: BookListing,
    canManage: Boolean,
    isMutating: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetStatus: (BookListingStatus) -> Unit,
) {
    UthCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BOOK_COVER_ASPECT_RATIO)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!listing.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = listing.photoUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "UTH",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.books_photo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(UthDimens.Spacing.Xs),
            ) {
                UthStatusBadge(
                    text = stringResource(
                        if (listing.status == BookListingStatus.AVAILABLE) {
                            R.string.books_available
                        } else {
                            R.string.books_sold
                        },
                    ),
                    tone = if (listing.status == BookListingStatus.AVAILABLE) {
                        BadgeTone.Success
                    } else {
                        BadgeTone.Neutral
                    },
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xs))

        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = if (listing.dealType == BookDealType.SALE) {
                formatPrice(listing.priceVnd)
            } else {
                stringResource(R.string.books_exchange)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(listing.condition.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (listing.sellerName.isNotBlank()) {
                Text(
                    text = listing.sellerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (canManage) {
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = !isMutating,
                    onClick = {
                        val nextStatus = if (listing.status == BookListingStatus.AVAILABLE) {
                            BookListingStatus.SOLD
                        } else {
                            BookListingStatus.AVAILABLE
                        }
                        onSetStatus(nextStatus)
                    },
                ) {
                    Text(
                        stringResource(
                            if (listing.status == BookListingStatus.AVAILABLE) {
                                R.string.books_mark_sold
                            } else {
                                R.string.books_mark_available
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.books_edit),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.books_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingFormDialog(
    initial: BookListing?,
    formListingId: String,
    currentProfile: StudentProfile?,
    currentUserUid: String?,
    saving: Boolean,
    saveStep: BookSaveStep,
    saveFailed: Boolean,
    onDismiss: () -> Unit,
    onSave: (BookListing) -> Unit,
    onRequireSignIn: () -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var subject by rememberSaveable(initial?.id) { mutableStateOf(initial?.subject.orEmpty()) }
    var condition by rememberSaveable(initial?.id) { mutableStateOf(initial?.condition ?: BookCondition.GOOD) }
    var dealType by rememberSaveable(initial?.id) { mutableStateOf(initial?.dealType ?: BookDealType.SALE) }
    var price by rememberSaveable(initial?.id) { mutableStateOf(initial?.priceVnd?.toString().orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var photoUri by rememberSaveable(initial?.id) { mutableStateOf(initial?.photoUri) }
    var photoTooLarge by rememberSaveable { mutableStateOf(false) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val size = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) cursor.getLong(sizeIndex) else null
            }
            photoTooLarge = size != null && size >= MAX_PHOTO_BYTES
            if (!photoTooLarge) photoUri = uri.toString()
        }
    }

    val effectiveSellerName = initial?.sellerName?.takeIf { it.isNotBlank() }
        ?: currentProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentProfile?.studentId.orEmpty()

    val effectiveContact = initial?.contact?.takeIf { it.isNotBlank() }
        ?: currentProfile?.phoneNumber?.takeIf { it.isNotBlank() }
        ?: currentProfile?.email.orEmpty()

    val effectiveOwnerUid = initial?.ownerUid?.takeIf { it.isNotBlank() }
        ?: currentUserUid
        ?: currentProfile?.studentId.orEmpty()

    val titleTrim = title.trim()
    val subjectTrim = subject.trim()
    val descriptionTrim = description.trim()

    val titleError = titleTrim.isEmpty() || titleTrim.length > BOOK_TITLE_MAX_LENGTH
    val subjectError = subjectTrim.isEmpty() || subjectTrim.length > BOOK_SUBJECT_MAX_LENGTH
    val descriptionError = descriptionTrim.length > BOOK_DESC_MAX_LENGTH

    val priceValue = price.toLongOrNull()
    val priceError = dealType == BookDealType.SALE && (priceValue == null || priceValue <= 0)
    val hasPhoto = !photoUri.isNullOrBlank() || !initial?.photoUrl.isNullOrBlank()
    val photoError = !hasPhoto

    val isFormValid = !titleError && !subjectError && !descriptionError && !priceError && !photoError

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(stringResource(if (initial == null) R.string.books_create else R.string.books_edit))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                SellerIdentitySection(
                    profile = currentProfile,
                    fallbackSellerName = effectiveSellerName,
                    fallbackContact = effectiveContact,
                    onRequireSignIn = onRequireSignIn,
                )

                FormField(
                    value = title,
                    onValueChange = { title = it },
                    label = R.string.books_book_title,
                    isError = showValidationErrors && titleError,
                    errorMessage = if (titleTrim.isEmpty()) {
                        stringResource(R.string.books_error_title_invalid)
                    } else if (titleTrim.length > BOOK_TITLE_MAX_LENGTH) {
                        stringResource(R.string.books_error_title_invalid)
                    } else {
                        null
                    },
                )

                FormField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = R.string.books_subject,
                    isError = showValidationErrors && subjectError,
                    errorMessage = if (subjectError) stringResource(R.string.books_error_subject_invalid) else null,
                )

                Text(
                    stringResource(R.string.books_condition),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    BookCondition.entries.forEach { value ->
                        FilterChip(
                            selected = condition == value,
                            onClick = { condition = value },
                            label = { Text(stringResource(value.labelRes)) },
                        )
                    }
                }

                Text(
                    stringResource(R.string.books_deal_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    BookDealType.entries.forEach { value ->
                        FilterChip(
                            selected = dealType == value,
                            onClick = { dealType = value },
                            label = {
                                Text(
                                    stringResource(
                                        if (value == BookDealType.SALE) {
                                            R.string.books_filter_sale
                                        } else {
                                            R.string.books_filter_exchange
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }

                if (dealType == BookDealType.SALE) {
                    FormField(
                        value = price,
                        onValueChange = { price = it },
                        label = R.string.books_price,
                        numeric = true,
                        isError = showValidationErrors && priceError,
                        errorMessage = if (priceError) stringResource(R.string.books_error_price_invalid) else null,
                    )
                }

                FormField(
                    value = description,
                    onValueChange = { description = it },
                    label = R.string.books_description,
                    singleLine = false,
                    isError = showValidationErrors && descriptionError,
                    errorMessage = if (descriptionError) {
                        stringResource(R.string.books_error_description_invalid)
                    } else {
                        null
                    },
                )

                BookPhotoPickerSection(
                    photoUri = photoUri,
                    existingPhotoUrl = initial?.photoUrl,
                    saving = saving,
                    photoTooLarge = photoTooLarge,
                    showError = showValidationErrors && photoError,
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )

                if (saving) {
                    SaveProgressIndicator(saveStep = saveStep)
                }

                if (saveFailed) {
                    Text(
                        stringResource(R.string.books_save_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    if (!isFormValid) {
                        showValidationErrors = true
                        return@TextButton
                    }
                    onSave(
                        BookListing(
                            id = initial?.id ?: formListingId,
                            title = titleTrim,
                            subject = subjectTrim,
                            condition = condition,
                            dealType = dealType,
                            priceVnd = if (dealType == BookDealType.SALE) priceValue else null,
                            description = descriptionTrim,
                            sellerName = effectiveSellerName,
                            contact = effectiveContact,
                            ownerUid = effectiveOwnerUid,
                            photoUrl = initial?.photoUrl,
                            photoUri = photoUri,
                            status = initial?.status ?: BookListingStatus.AVAILABLE,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            imagePath = initial?.imagePath,
                        ),
                    )
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(UthDimens.Size.ProgressInline))
                } else {
                    Text(stringResource(R.string.books_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.books_cancel))
            }
        },
    )
}

@Composable
private fun SellerIdentitySection(
    profile: StudentProfile?,
    fallbackSellerName: String,
    fallbackContact: String,
    onRequireSignIn: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.books_seller_account_info),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = profile?.displayName?.ifBlank { fallbackSellerName } ?: fallbackSellerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (profile != null) {
                Text(
                    text = "MSSV: ${profile.studentId} • Lớp: ${profile.className}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Liên hệ: ${profile.phoneNumber.ifBlank { profile.email }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.books_auth_required_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(
                    onClick = onRequireSignIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.auth_sign_up))
                }
            }
        }
    }
}

@Composable
private fun BookPhotoPickerSection(
    photoUri: String?,
    existingPhotoUrl: String?,
    saving: Boolean,
    photoTooLarge: Boolean,
    showError: Boolean,
    onPickPhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Text(
            stringResource(R.string.books_photo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            val previewModel = photoUri ?: existingPhotoUrl
            if (previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = stringResource(R.string.books_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.books_photo_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OutlinedButton(
            enabled = !saving,
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.books_pick_photo))
        }

        if (photoTooLarge) {
            Text(
                stringResource(R.string.books_photo_too_large),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (showError) {
            Text(
                stringResource(R.string.books_photo_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SaveProgressIndicator(saveStep: BookSaveStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UthDimens.Spacing.Xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(UthDimens.Size.ProgressInline))
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        Text(
            text = when (saveStep) {
                BookSaveStep.UPLOADING_IMAGE -> stringResource(R.string.books_uploading_photo)
                BookSaveStep.SAVING_LISTING -> stringResource(R.string.books_saving_listing)
                else -> stringResource(R.string.books_loading)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SaveFailureAlert(
    failure: BooksFailure,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = UthDimens.Spacing.Sm),
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
            IconButton(onClick = onDismiss) {
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
private fun BookDetailDialog(
    listing: BookListing,
    onDismiss: () -> Unit,
    onErrorOpeningContact: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listing.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                if (!listing.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = listing.photoUrl,
                        contentDescription = listing.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(listing.subject, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.books_condition_format, stringResource(listing.condition.labelRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (listing.dealType == BookDealType.SALE) formatPrice(listing.priceVnd) else stringResource(R.string.books_exchange),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (listing.description.isNotBlank()) {
                    Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.books_seller_format, listing.sellerName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(listing.contact, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val contact = listing.contact.trim()
                    runCatching {
                        val destination = if ('@' in contact) {
                            "mailto:${Uri.encode(contact)}"
                        } else {
                            "tel:${Uri.encode(contact)}"
                        }
                        uriHandler.openUri(destination)
                    }.onSuccess { onDismiss() }.onFailure { onErrorOpeningContact() }
                },
            ) {
                Text(stringResource(R.string.books_contact_seller))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.books_close))
            }
        },
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
        ),
    )
}

private fun formatPrice(value: Long?): String = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("vi").setRegion("VN").build())
    .format(value ?: 0) + " ₫"

private val BookCondition.labelRes: Int
    get() = when (this) {
        BookCondition.NEW -> R.string.books_condition_new
        BookCondition.GOOD -> R.string.books_condition_good
        BookCondition.USED -> R.string.books_condition_used
    }
