package com.giathinh.uthconnect.presentation.books

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@Composable
fun BooksScreen(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.books_back))
                    }
                    Column {
                        Text(stringResource(R.string.books_title), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(R.string.books_header_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { onAction(BooksAction.Search(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.books_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    stringResource(R.string.books_deal_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    BooksFilter.entries.forEach { filter ->
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
                }
                Text(
                    stringResource(R.string.books_condition),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    FilterChip(
                        selected = uiState.condition == null,
                        onClick = { onAction(BooksAction.SetCondition(null)) },
                        label = { Text(stringResource(R.string.books_condition_all)) },
                    )
                    BookCondition.entries.forEach { condition ->
                        FilterChip(
                            selected = uiState.condition == condition,
                            onClick = { onAction(BooksAction.SetCondition(condition)) },
                            label = { Text(stringResource(condition.label)) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.books_price_range),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    OutlinedTextField(
                        value = uiState.minimumPrice,
                        onValueChange = { onAction(BooksAction.SetPriceRange(it, uiState.maximumPrice)) },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.books_min_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.maximumPrice,
                        onValueChange = { onAction(BooksAction.SetPriceRange(uiState.minimumPrice, it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.books_max_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    BooksSort.entries.forEach { sort ->
                        FilterChip(
                            selected = uiState.sort == sort,
                            onClick = { onAction(BooksAction.SetSort(sort)) },
                            label = {
                                Text(
                                    stringResource(
                                        when (sort) {
                                            BooksSort.NEWEST -> R.string.books_sort_newest
                                            BooksSort.PRICE_LOW -> R.string.books_sort_price_low
                                            BooksSort.PRICE_HIGH -> R.string.books_sort_price_high
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = uiState.includeSold,
                        onClick = { onAction(BooksAction.IncludeSold(!uiState.includeSold)) },
                        label = { Text(stringResource(R.string.books_show_sold)) },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.books_result_count, uiState.visibleListings.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                UthPrimaryButton(
                    text = stringResource(R.string.books_create),
                    onClick = {
                        editingId = null
                        newListingId = UUID.randomUUID().toString()
                        saveFailed = false
                        showForm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.currentUserUid != null,
                )
            }
        }

        if (uiState.isLoading) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(UthDimens.Spacing.Lg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(UthDimens.Size.ProgressInline))
                    Text(
                        stringResource(R.string.books_loading),
                        modifier = Modifier.padding(start = UthDimens.Spacing.Sm),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (uiState.saveFailure != null && !showForm) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                when (uiState.saveFailure) {
                                    BooksFailure.DELETE_FAILED -> R.string.books_delete_error
                                    BooksFailure.UPDATE_STATUS_FAILED -> R.string.books_status_error
                                    else -> R.string.books_save_error
                                },
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = { onAction(BooksAction.DismissError) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.books_close),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.loadFailure != null) {
            item {
                UthEmptyState(
                    title = stringResource(if (uiState.currentUserUid == null) R.string.books_auth_required_title else R.string.books_error_title),
                    body = stringResource(if (uiState.currentUserUid == null) R.string.books_auth_required_body else R.string.books_error_body),
                    icon = Icons.Filled.Warning,
                    actionLabel = if (uiState.currentUserUid == null) null else stringResource(R.string.common_retry),
                    onAction = if (uiState.currentUserUid == null) {
                        null
                    } else {
                        { onAction(BooksAction.Retry) }
                    },
                )
            }
        } else if (!uiState.isLoading && uiState.loadFailure == null && uiState.visibleListings.isEmpty()) {
            item {
                UthEmptyState(
                    title = stringResource(if (uiState.listings.isEmpty()) R.string.books_empty_title else R.string.books_no_results_title),
                    body = stringResource(if (uiState.listings.isEmpty()) R.string.books_empty_body else R.string.books_no_results_body),
                    actionLabel = if (uiState.listings.isEmpty()) null else stringResource(R.string.books_clear_filters),
                    onAction = if (uiState.listings.isEmpty()) {
                        null
                    } else {
                        { onAction(BooksAction.ResetFilters) }
                    },
                )
            }
        } else if (!uiState.isLoading && uiState.loadFailure == null) {
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
                )
            }
        }
        item { Spacer(Modifier.height(UthDimens.Spacing.Xl)) }
    }

    if (showForm) {
        if (editingId != null && editingListing == null && uiState.isLoading) {
            AlertDialog(
                onDismissRequest = {
                    showForm = false
                    editingId = null
                    newListingId = null
                },
                title = { Text(stringResource(R.string.books_loading)) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(UthDimens.Spacing.Md),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(UthDimens.Size.ProgressInline))
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        showForm = false
                        editingId = null
                        newListingId = null
                    }) {
                        Text(stringResource(R.string.books_cancel))
                    }
                },
            )
        } else if (editingId != null && editingListing == null && !uiState.isLoading) {
            AlertDialog(
                onDismissRequest = {
                    showForm = false
                    editingId = null
                    newListingId = null
                },
                title = { Text(stringResource(R.string.books_error_title)) },
                text = { Text(stringResource(R.string.books_no_results_title)) },
                confirmButton = {
                    TextButton(onClick = {
                        showForm = false
                        editingId = null
                        newListingId = null
                    }) {
                        Text(stringResource(R.string.books_close))
                    }
                },
            )
        } else {
            val stableFormId = editingId ?: (newListingId ?: rememberSaveable { UUID.randomUUID().toString() }.also { newListingId = it })
            ListingFormDialog(
                initial = editingListing,
                formListingId = stableFormId,
                saving = uiState.isSaving,
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
            )
        }
    }

    details?.let { listing ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(listing.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    listing.photoUrl?.let {
                        AsyncImage(it, listing.title, modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
                    }
                    Text(listing.subject, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.books_condition_format, stringResource(listing.condition.label)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (listing.dealType == BookDealType.SALE) priceText(listing.priceVnd) else stringResource(R.string.books_exchange),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(listing.description)
                    Text(stringResource(R.string.books_seller_format, listing.sellerName))
                    SelectionContainer { Text(listing.contact, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val contact = listing.contact.trim()
                    runCatching {
                        uriHandler.openUri(if ('@' in contact) "mailto:${Uri.encode(contact)}" else "tel:${Uri.encode(contact)}")
                    }.onSuccess { details = null }.onFailure { showContactError = true }
                }) { Text(stringResource(R.string.books_contact_seller)) }
            },
            dismissButton = { TextButton(onClick = { details = null }) { Text(stringResource(R.string.books_close)) } },
        )
    }

    if (showContactError) {
        AlertDialog(
            onDismissRequest = { showContactError = false },
            title = { Text(stringResource(R.string.books_contact_failed_title)) },
            text = { Text(stringResource(R.string.books_contact_failed_body)) },
            confirmButton = { TextButton(onClick = { showContactError = false }) { Text(stringResource(R.string.books_close)) } },
        )
    }

    pendingDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.books_delete_title)) },
            text = { Text(stringResource(R.string.books_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onAction(BooksAction.Delete(listing.id))
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.books_delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.books_cancel)) } },
        )
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
) {
    UthCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        if (listing.photoUrl != null) {
            AsyncImage(
                listing.photoUrl,
                contentDescription = listing.title,
                modifier = Modifier.fillMaxWidth().height(176.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "UTH",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(listing.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listing.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            UthStatusBadge(
                text = stringResource(if (listing.status == BookListingStatus.AVAILABLE) R.string.books_available else R.string.books_sold),
                tone = if (listing.status == BookListingStatus.AVAILABLE) BadgeTone.Success else BadgeTone.Neutral,
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            if (listing.dealType == BookDealType.SALE) priceText(listing.priceVnd) else stringResource(R.string.books_exchange),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.books_condition_format, stringResource(listing.condition.label)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canManage) {
            Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                TextButton(
                    enabled = !isMutating,
                    onClick = {
                        val target = if (listing.status == BookListingStatus.AVAILABLE) BookListingStatus.SOLD else BookListingStatus.AVAILABLE
                        onSetStatus(target)
                    },
                ) {
                    Text(stringResource(if (listing.status == BookListingStatus.AVAILABLE) R.string.books_mark_sold else R.string.books_mark_available))
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = onEdit,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.books_edit))
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = onDelete,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.books_delete))
                }
            }
        }
    }
}

@Composable
private fun ListingFormDialog(
    initial: BookListing?,
    formListingId: String,
    saving: Boolean,
    saveFailed: Boolean,
    onDismiss: () -> Unit,
    onSave: (BookListing) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var subject by rememberSaveable(initial?.id) { mutableStateOf(initial?.subject.orEmpty()) }
    var condition by rememberSaveable(initial?.id) { mutableStateOf(initial?.condition ?: BookCondition.GOOD) }
    var dealType by rememberSaveable(initial?.id) { mutableStateOf(initial?.dealType ?: BookDealType.SALE) }
    var price by rememberSaveable(initial?.id) { mutableStateOf(initial?.priceVnd?.toString().orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var seller by rememberSaveable(initial?.id) { mutableStateOf(initial?.sellerName.orEmpty()) }
    var contact by rememberSaveable(initial?.id) { mutableStateOf(initial?.contact.orEmpty()) }
    var photoUri by rememberSaveable(initial?.id) { mutableStateOf(initial?.photoUri) }
    var photoTooLarge by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val size = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) cursor.getLong(sizeIndex) else null
            }
            photoTooLarge = size != null && size >= MAX_BOOK_PHOTO_BYTES
            if (!photoTooLarge) photoUri = uri.toString()
        }
    }

    val titleTrim = title.trim()
    val subjectTrim = subject.trim()
    val sellerTrim = seller.trim()
    val descriptionTrim = description.trim()
    val contactTrim = contact.trim()

    val titleError = title.isNotBlank() && titleTrim.length > 160
    val subjectError = subject.isNotBlank() && subjectTrim.length > 100
    val sellerError = seller.isNotBlank() && sellerTrim.length > 100
    val descriptionError = descriptionTrim.length > 2000
    val contactError = contact.isNotBlank() && (contactTrim.length !in 5..254 || !isValidContact(contactTrim))

    val priceValue = price.toLongOrNull()
    val priceError = dealType == BookDealType.SALE && price.isNotBlank() && (priceValue == null || priceValue <= 0)

    val valid = titleTrim.isNotEmpty() && !titleError &&
        subjectTrim.isNotEmpty() && !subjectError &&
        sellerTrim.isNotEmpty() && !sellerError &&
        !descriptionError &&
        contactTrim.isNotEmpty() && !contactError &&
        (dealType != BookDealType.SALE || (priceValue != null && priceValue > 0))

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.books_create else R.string.books_edit)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                modifier = Modifier.padding(top = UthDimens.Spacing.Sm).verticalScroll(rememberScrollState()),
            ) {
                FormField(
                    value = title,
                    onValueChange = { title = it },
                    label = R.string.books_book_title,
                    isError = titleError,
                    errorMessage = if (titleError) stringResource(R.string.books_error_title_invalid) else null,
                )
                FormField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = R.string.books_subject,
                    isError = subjectError,
                    errorMessage = if (subjectError) stringResource(R.string.books_error_subject_invalid) else null,
                )
                Text(stringResource(R.string.books_condition))
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    BookCondition.entries.forEach { value ->
                        FilterChip(condition == value, { condition = value }, label = { Text(stringResource(value.label)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    BookDealType.entries.forEach { value ->
                        FilterChip(dealType == value, { dealType = value }, label = {
                            Text(stringResource(if (value == BookDealType.SALE) R.string.books_filter_sale else R.string.books_filter_exchange))
                        })
                    }
                }
                if (dealType == BookDealType.SALE) {
                    FormField(
                        value = price,
                        onValueChange = { price = it },
                        label = R.string.books_price,
                        numeric = true,
                        isError = priceError,
                        errorMessage = if (priceError) stringResource(R.string.books_error_price_invalid) else null,
                    )
                }
                FormField(
                    value = description,
                    onValueChange = { description = it },
                    label = R.string.books_description,
                    singleLine = false,
                    isError = descriptionError,
                    errorMessage = if (descriptionError) stringResource(R.string.books_error_description_invalid) else null,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    AsyncImage(
                        model = photoUri ?: initial?.photoUrl,
                        contentDescription = stringResource(R.string.books_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Button(enabled = !saving, onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(R.string.books_pick_photo))
                }
                if (photoTooLarge) Text(stringResource(R.string.books_photo_too_large), color = MaterialTheme.colorScheme.error)
                FormField(
                    value = seller,
                    onValueChange = { seller = it },
                    label = R.string.books_seller,
                    isError = sellerError,
                    errorMessage = if (sellerError) stringResource(R.string.books_error_seller_invalid) else null,
                )
                FormField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = R.string.books_contact,
                    isError = contactError,
                    errorMessage = if (contactError) stringResource(R.string.books_error_contact_invalid) else null,
                )
                if (saveFailed) Text(stringResource(R.string.books_save_error), color = MaterialTheme.colorScheme.error)
                if (!valid) Text(stringResource(R.string.books_form_invalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        BookListing(
                            id = initial?.id ?: formListingId,
                            title = title.trim(),
                            subject = subject.trim(),
                            condition = condition,
                            dealType = dealType,
                            priceVnd = if (dealType == BookDealType.SALE) priceValue else null,
                            description = description.trim(),
                            sellerName = seller.trim(),
                            contact = contact.trim(),
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
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.books_cancel)) } },
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
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
    )
}

private fun isValidContact(value: String): Boolean {
    val contact = value.trim()
    return contact.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ||
        (contact.matches(Regex("^[+()0-9\\s-]{8,20}$")) && contact.filter(Char::isDigit).length >= 8)
}

@Composable
private fun priceText(value: Long?): String = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value ?: 0) + " ₫"

private const val MAX_BOOK_PHOTO_BYTES = 5L * 1024 * 1024

private val BookCondition.label: Int
    get() = when (this) {
        BookCondition.NEW -> R.string.books_condition_new
        BookCondition.GOOD -> R.string.books_condition_good
        BookCondition.USED -> R.string.books_condition_used
    }

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BooksScreenPreview() = UthPreview { BooksScreen(BooksUiState(isLoading = false), {}, {}) }
