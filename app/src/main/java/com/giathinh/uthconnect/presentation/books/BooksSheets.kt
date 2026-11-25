package com.giathinh.uthconnect.presentation.books

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BooksFilterBottomSheet(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.books_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.books_close))
                }
            }

            Text(
                text = stringResource(R.string.books_condition),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
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
                        label = { Text(stringResource(condition.labelRes())) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.books_price_range),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            if (largeText) {
                Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    PriceRangeFields(
                        uiState = uiState,
                        onAction = onAction,
                        firstModifier = Modifier.fillMaxWidth(),
                        secondModifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    PriceRangeFields(
                        uiState = uiState,
                        onAction = onAction,
                        firstModifier = Modifier.weight(1f),
                        secondModifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = stringResource(R.string.books_filter_sort_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
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

            FilterChip(
                selected = uiState.includeSold,
                onClick = { onAction(BooksAction.IncludeSold(!uiState.includeSold)) },
                label = { Text(stringResource(R.string.books_show_sold)) },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    onClick = { onAction(BooksAction.ResetFilters) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.books_clear_filters))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.books_filter_apply))
                }
            }
        }
    }
}

@Composable
private fun PriceRangeFields(
    uiState: BooksUiState,
    onAction: (BooksAction) -> Unit,
    firstModifier: Modifier,
    secondModifier: Modifier,
) {
    OutlinedTextField(
        value = uiState.minimumPrice,
        onValueChange = { onAction(BooksAction.SetPriceRange(it, uiState.maximumPrice)) },
        modifier = firstModifier,
        label = { Text(stringResource(R.string.books_min_price)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        singleLine = true,
    )
    OutlinedTextField(
        value = uiState.maximumPrice,
        onValueChange = { onAction(BooksAction.SetPriceRange(uiState.minimumPrice, it)) },
        modifier = secondModifier,
        label = { Text(stringResource(R.string.books_max_price)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailBottomSheet(
    listing: BookListing,
    onDismiss: () -> Unit,
    onCopyContact: () -> Unit,
    onContact: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isAvailable = listing.status == BookListingStatus.AVAILABLE

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
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UthStatusBadge(
                    text = stringResource(if (isAvailable) R.string.books_available else R.string.books_sold),
                    tone = if (isAvailable) BadgeTone.Success else BadgeTone.Neutral,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.books_close))
                }
            }

            listing.photoUrl?.let { photo ->
                AsyncImage(
                    model = photo,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UthDimens.Size.BookFormImageHeight)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
            }

            Text(
                text = listing.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listing.subject,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (listing.dealType == BookDealType.SALE) {
                    formatBookPrice(listing.priceVnd)
                } else {
                    stringResource(R.string.books_exchange)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.books_condition_format, stringResource(listing.condition.labelRes())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.books_seller_format, listing.sellerName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.books_contact_label, listing.contact),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(
                    onClick = onCopyContact,
                    modifier = Modifier.defaultMinSize(minHeight = UthDimens.Size.MinTouchTarget, minWidth = 0.dp),
                ) {
                    Text(stringResource(R.string.books_copy_contact))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = UthDimens.Spacing.Sm, bottom = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.books_close))
                }
                Button(
                    onClick = onContact,
                    enabled = isAvailable && listing.contact.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(
                        stringResource(
                            if (isAvailable) R.string.books_contact_seller else R.string.books_sold_contact_disabled,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BooksFormHost(
    editingId: String?,
    editingListing: BookListing?,
    newListingId: String?,
    uiState: BooksUiState,
    saveFailed: Boolean,
    onUpdateNewListingId: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (BookListing) -> Unit,
) {
    if (editingId != null && editingListing == null && uiState.isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.books_cancel)) }
            },
        )
    } else if (editingId != null && editingListing == null && !uiState.isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.books_error_title)) },
            text = { Text(stringResource(R.string.books_no_results_title)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.books_close)) }
            },
        )
    } else {
        val stableFormId = editingId ?: (
            newListingId ?: rememberSaveable { UUID.randomUUID().toString() }.also(onUpdateNewListingId)
            )
        ListingFormBottomSheet(
            initial = editingListing,
            formListingId = stableFormId,
            saving = uiState.isSaving,
            saveFailed = saveFailed,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ListingFormBottomSheet(
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
    var removeExistingPhoto by rememberSaveable(initial?.id) { mutableStateOf(false) }
    var photoTooLarge by rememberSaveable { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable(initial?.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val size = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) cursor.getLong(sizeIndex) else null
            }
            photoTooLarge = size != null && size >= MAX_BOOK_PHOTO_BYTES
            if (!photoTooLarge) {
                photoUri = uri.toString()
                removeExistingPhoto = false
            }
        }
    }

    val titleTrim = title.trim()
    val subjectTrim = subject.trim()
    val sellerTrim = seller.trim()
    val descriptionTrim = description.trim()
    val contactTrim = contact.trim()

    val titleError = (title.isNotBlank() || attemptedSubmit) &&
        (titleTrim.isEmpty() || titleTrim.length > MAX_BOOK_TITLE_LEN)
    val subjectError = (subject.isNotBlank() || attemptedSubmit) &&
        (subjectTrim.isEmpty() || subjectTrim.length > MAX_BOOK_TEXT_LEN)
    val sellerError = (seller.isNotBlank() || attemptedSubmit) &&
        (sellerTrim.isEmpty() || sellerTrim.length > MAX_BOOK_TEXT_LEN)
    val descriptionError = descriptionTrim.length > MAX_BOOK_DESC_LEN
    val contactError = (contact.isNotBlank() || attemptedSubmit) &&
        (contactTrim.length !in MIN_CONTACT_LEN..MAX_CONTACT_LEN || !isValidContact(contactTrim))

    val priceValue = price.toLongOrNull()
    val priceError = dealType == BookDealType.SALE &&
        (price.isNotBlank() || attemptedSubmit) &&
        (priceValue == null || priceValue <= 0)

    val valid = titleTrim.isNotEmpty() && !titleError &&
        subjectTrim.isNotEmpty() && !subjectError &&
        sellerTrim.isNotEmpty() && !sellerError &&
        !descriptionError &&
        contactTrim.isNotEmpty() && !contactError &&
        (dealType != BookDealType.SALE || (priceValue != null && priceValue > 0))

    val displayedPhoto: Any? = photoUri ?: initial?.photoUrl?.takeIf { !removeExistingPhoto }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = UthDimens.Spacing.Xl)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (initial == null) R.string.books_create else R.string.books_edit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                IconButton(
                    enabled = !saving,
                    onClick = onDismiss,
                    modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.books_close))
                }
            }

            FormField(
                value = title,
                onValueChange = { title = it },
                label = R.string.books_book_title,
                isError = titleError,
                errorMessage = if (titleError) stringResource(R.string.books_error_title_invalid) else null,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = subject,
                onValueChange = { subject = it },
                label = R.string.books_subject,
                isError = subjectError,
                errorMessage = if (subjectError) stringResource(R.string.books_error_subject_invalid) else null,
                imeAction = ImeAction.Next,
            )
            Text(
                text = stringResource(R.string.books_condition),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                BookCondition.entries.forEach { value ->
                    FilterChip(
                        selected = condition == value,
                        onClick = { condition = value },
                        label = { Text(stringResource(value.labelRes())) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.books_deal_type),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
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
                    isError = priceError,
                    errorMessage = if (priceError) stringResource(R.string.books_error_price_invalid) else null,
                    imeAction = ImeAction.Next,
                )
            }
            FormField(
                value = description,
                onValueChange = { description = it },
                label = R.string.books_description,
                singleLine = false,
                isError = descriptionError,
                errorMessage = if (descriptionError) stringResource(R.string.books_error_description_invalid) else null,
                imeAction = ImeAction.Default,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UthDimens.Size.BookFormImageHeight),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (displayedPhoto != null) {
                    AsyncImage(
                        model = displayedPhoto,
                        contentDescription = stringResource(R.string.books_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.books_photo_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                FilledTonalButton(
                    enabled = !saving,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.books_pick_photo))
                }
                if (displayedPhoto != null) {
                    OutlinedButton(
                        enabled = !saving,
                        onClick = {
                            photoUri = null
                            removeExistingPhoto = true
                            photoTooLarge = false
                        },
                        modifier = Modifier.heightIn(min = UthDimens.Size.ButtonHeight),
                    ) {
                        Text(stringResource(R.string.books_remove_photo))
                    }
                }
            }
            if (photoTooLarge) {
                Text(stringResource(R.string.books_photo_too_large), color = MaterialTheme.colorScheme.error)
            }
            FormField(
                value = seller,
                onValueChange = { seller = it },
                label = R.string.books_seller,
                isError = sellerError,
                errorMessage = if (sellerError) stringResource(R.string.books_error_seller_invalid) else null,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = contact,
                onValueChange = { contact = it },
                label = R.string.books_contact,
                isError = contactError,
                errorMessage = if (contactError) stringResource(R.string.books_error_contact_invalid) else null,
                imeAction = ImeAction.Done,
            )
            if (saveFailed) {
                Text(stringResource(R.string.books_save_error), color = MaterialTheme.colorScheme.error)
            }
            if (attemptedSubmit && !valid) {
                Text(stringResource(R.string.books_form_invalid), color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = UthDimens.Spacing.Sm, bottom = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    enabled = !saving,
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.books_cancel))
                }
                Button(
                    enabled = !saving,
                    onClick = {
                        attemptedSubmit = true
                        if (valid) {
                            onSave(
                                BookListing(
                                    id = initial?.id ?: formListingId,
                                    title = titleTrim,
                                    subject = subjectTrim,
                                    condition = condition,
                                    dealType = dealType,
                                    priceVnd = if (dealType == BookDealType.SALE) priceValue else null,
                                    description = descriptionTrim,
                                    sellerName = sellerTrim,
                                    contact = contactTrim,
                                    photoUrl = if (removeExistingPhoto) null else initial?.photoUrl,
                                    photoUri = photoUri,
                                    status = initial?.status ?: BookListingStatus.AVAILABLE,
                                    createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                                    imagePath = if (removeExistingPhoto) null else initial?.imagePath,
                                ),
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(UthDimens.Size.ProgressInline),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = UthDimens.Size.ProgressInlineStroke,
                        )
                    } else {
                        Text(stringResource(R.string.books_save))
                    }
                }
            }
        }
    }
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
    imeAction: ImeAction = ImeAction.Next,
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
            imeAction = imeAction,
        ),
    )
}

private fun isValidContact(value: String): Boolean {
    val contact = value.trim()
    return contact.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ||
        (contact.matches(Regex("^[+()0-9\\s-]{8,20}$")) && contact.filter(Char::isDigit).length >= MIN_PHONE_DIGITS)
}

@Composable
internal fun formatBookPrice(value: Long?): String {
    val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value ?: 0)
    return "${formatted}đ"
}

internal fun BookCondition.labelRes(): Int = when (this) {
    BookCondition.NEW -> R.string.books_condition_new
    BookCondition.GOOD -> R.string.books_condition_good
    BookCondition.USED -> R.string.books_condition_used
}

private const val MAX_BOOK_PHOTO_BYTES = 5L * 1024 * 1024
private const val MAX_BOOK_TITLE_LEN = 160
private const val MAX_BOOK_TEXT_LEN = 100
private const val MAX_BOOK_DESC_LEN = 2000
private const val MIN_CONTACT_LEN = 5
private const val MAX_CONTACT_LEN = 254
private const val MIN_PHONE_DIGITS = 8
