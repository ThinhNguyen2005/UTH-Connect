package com.giathinh.uthconnect.presentation.lostfound

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import coil3.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_IMAGE_BYTES = 15 * 1024 * 1024
private const val READ_BUFFER_SIZE = 8192
private const val DESCRIPTION_MIN_LINES = 3
private const val PICKER_SURFACE_ALPHA = 0.4f

/**
 * Modal bottom sheet for creating or editing a Lost & Found listing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItemBottomSheet(
    form: ReportFormState,
    onAction: (LostFoundAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = form.editingListingId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.lostfound_edit_title else R.string.lostfound_dialog_title,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.lostfound_bottomsheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

            ReportKindSelector(
                selectedKind = form.kind,
                onSelectKind = { onAction(LostFoundAction.UpdateReportKind(it)) },
            )

            OutlinedTextField(
                value = form.title,
                onValueChange = { onAction(LostFoundAction.UpdateReportTitle(it)) },
                label = { Text(stringResource(R.string.lostfound_field_title)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_title_hint)) },
                isError = form.titleError != null,
                supportingText = form.titleError?.let { err ->
                    { Text(stringResource(err.messageRes)) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            ReportCategorySelector(
                selectedCategory = form.category,
                onSelectCategory = { onAction(LostFoundAction.UpdateReportCategory(it)) },
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = { onAction(LostFoundAction.UpdateReportDescription(it)) },
                label = { Text(stringResource(R.string.lostfound_field_desc)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_desc_hint)) },
                isError = form.descriptionError != null,
                supportingText = form.descriptionError?.let { err ->
                    val text = if (err is FormValidationError.DescriptionTooShort) {
                        stringResource(err.messageRes, err.currentLength)
                    } else {
                        stringResource(err.messageRes)
                    }
                    { Text(text) }
                },
                minLines = DESCRIPTION_MIN_LINES,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.location,
                onValueChange = { onAction(LostFoundAction.UpdateReportLocation(it)) },
                label = { Text(stringResource(R.string.lostfound_field_location)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_location_hint)) },
                isError = form.locationError != null,
                supportingText = form.locationError?.let { err ->
                    { Text(stringResource(err.messageRes)) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.contactName,
                onValueChange = { onAction(LostFoundAction.UpdateReportContactName(it)) },
                label = { Text(stringResource(R.string.lostfound_field_contact_name)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_contact_name_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.contactPhone,
                onValueChange = { onAction(LostFoundAction.UpdateReportContactPhone(it)) },
                label = { Text(stringResource(R.string.lostfound_field_contact_phone)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_contact_phone_hint)) },
                isError = form.phoneError != null,
                supportingText = {
                    val err = form.phoneError
                    Text(
                        text = if (err != null) {
                            stringResource(err.messageRes)
                        } else {
                            stringResource(R.string.lostfound_phone_public_notice)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            ReportImagePicker(form = form, onAction = onAction)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UthDimens.Spacing.Lg),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !form.isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    Text(stringResource(R.string.lostfound_btn_cancel))
                }
                Button(
                    onClick = { onAction(LostFoundAction.SubmitReport) },
                    enabled = !form.isSubmitting && !form.isProcessingImage,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                ) {
                    if (form.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(UthDimens.Size.ProgressInline),
                            strokeWidth = UthDimens.Size.ProgressInlineStroke,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (isEditing) R.string.lostfound_btn_save_edit else R.string.lostfound_btn_submit,
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKindSelector(
    selectedKind: ListingKind,
    onSelectKind: (ListingKind) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        val options = listOf(
            ListingKind.LOST to R.string.lostfound_kind_lost_btn,
            ListingKind.FOUND to R.string.lostfound_kind_found_btn,
        )
        options.forEach { (kind, labelRes) ->
            val selected = selectedKind == kind
            val containerColor = when {
                !selected -> MaterialTheme.colorScheme.surfaceVariant
                kind == ListingKind.LOST -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val contentColor = when {
                !selected -> MaterialTheme.colorScheme.onSurfaceVariant
                kind == ListingKind.LOST -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = UthDimens.Size.MinTouchTarget)
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = selected,
                        onClick = { onSelectKind(kind) },
                        role = Role.RadioButton,
                    ),
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                border = if (selected) {
                    BorderStroke(UthDimens.Size.Stroke, contentColor)
                } else {
                    null
                },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Md,
                    ),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCategorySelector(
    selectedCategory: ItemCategory,
    onSelectCategory: (ItemCategory) -> Unit,
) {
    val categories = ItemCategory.entries.filter { it != ItemCategory.ALL }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
    ) {
        categories.forEach { category ->
            val selected = selectedCategory == category
            FilterChip(
                selected = selected,
                onClick = { onSelectCategory(category) },
                leadingIcon = if (selected) {
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
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@Composable
private fun ReportImagePicker(
    form: ReportFormState,
    onAction: (LostFoundAction) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { imageJob?.cancel() }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        imageJob?.cancel()
        onAction(LostFoundAction.SetReportImageProcessing(true))
        imageJob = scope.launch {
            val result = withContext(Dispatchers.IO) { readBoundedImageBytes(context, uri) }
            if (!isActive) return@launch
            when (result) {
                is ImageReadResult.Success -> onAction(
                    LostFoundAction.UpdateReportImage(
                        bytes = result.bytes,
                        mimeType = result.mimeType,
                        previewUri = uri.toString(),
                    ),
                )

                ImageReadResult.TooLarge -> {
                    Toast.makeText(context, R.string.lostfound_image_too_large, Toast.LENGTH_SHORT).show()
                    onAction(LostFoundAction.SetReportImageProcessing(false))
                }

                ImageReadResult.ReadError -> {
                    Toast.makeText(context, R.string.lostfound_image_read_error, Toast.LENGTH_SHORT).show()
                    onAction(LostFoundAction.SetReportImageProcessing(false))
                }
            }
        }
    }

    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val localBitmap = remember(form.imageBytes) {
        form.imageBytes?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }
    val hasExistingRemoteImage = !form.removeExistingImage && form.existingImageUrl.isTrustedLostFoundImageUrl()

    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Text(
            text = stringResource(R.string.lostfound_field_image),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when {
            localBitmap != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(UthDimens.Layout.CoverAspectRatio)
                        .clip(MaterialTheme.shapes.medium),
                ) {
                    Image(
                        bitmap = localBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.lostfound_image_attached_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    TextButton(onClick = launchPicker, enabled = !form.isProcessingImage) {
                        Text(stringResource(R.string.lostfound_btn_change_image))
                    }
                    TextButton(
                        onClick = { onAction(LostFoundAction.RemoveReportImage) },
                        enabled = !form.isProcessingImage,
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_btn_remove_image),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            hasExistingRemoteImage -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(UthDimens.Layout.CoverAspectRatio)
                        .clip(MaterialTheme.shapes.medium),
                ) {
                    AsyncImage(
                        model = form.existingImageUrl,
                        contentDescription = stringResource(R.string.lostfound_image_attached_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    TextButton(onClick = launchPicker, enabled = !form.isProcessingImage) {
                        Text(stringResource(R.string.lostfound_btn_change_image))
                    }
                    TextButton(
                        onClick = { onAction(LostFoundAction.RemoveReportImage) },
                        enabled = !form.isProcessingImage,
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_btn_remove_image),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            else -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PICKER_SURFACE_ALPHA),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.ButtonHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = !form.isProcessingImage, onClick = launchPicker),
                ) {
                    Row(
                        modifier = Modifier.padding(UthDimens.Spacing.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (form.isProcessingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(UthDimens.Size.ProgressInline),
                                strokeWidth = UthDimens.Size.ProgressInlineStroke,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(UthDimens.Size.Icon)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                                )
                            }
                            Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                            Text(
                                text = stringResource(R.string.lostfound_btn_pick_image),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.lostfound_image_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private sealed interface ImageReadResult {
    data class Success(val bytes: ByteArray, val mimeType: String?) : ImageReadResult
    data object TooLarge : ImageReadResult
    data object ReadError : ImageReadResult
}

private fun readBoundedImageBytes(context: Context, uri: Uri): ImageReadResult = runCatching {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(READ_BUFFER_SIZE)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > MAX_IMAGE_BYTES) return ImageReadResult.TooLarge
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    } ?: return ImageReadResult.ReadError
    ImageReadResult.Success(bytes = bytes, mimeType = mimeType)
}.getOrElse { ImageReadResult.ReadError }
