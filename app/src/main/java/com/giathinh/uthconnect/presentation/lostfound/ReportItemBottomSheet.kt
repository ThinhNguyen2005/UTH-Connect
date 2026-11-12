package com.giathinh.uthconnect.presentation.lostfound

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modal bottom sheet presenting the form to report a lost or found item.
 *
 * @param form Current form validation state and fields.
 * @param onAction Callback to handle user form actions.
 * @param onDismiss Callback triggered when user dismisses the sheet.
 * @param modifier Optional modifier for the bottom sheet layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItemBottomSheet(
    form: ReportFormState,
    onAction: (LostFoundAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!form.isSubmitting) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.lostfound_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.lostfound_bottomsheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, enabled = !form.isSubmitting) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.lostfound_btn_close),
                    )
                }
            }

            // Chọn loại tin (Mất đồ vs Nhặt được đồ)
            ReportKindSelector(kind = form.kind, onAction = onAction)

            // Tiêu đề đồ vật
            OutlinedTextField(
                value = form.title,
                onValueChange = { onAction(LostFoundAction.UpdateReportTitle(it)) },
                label = { Text(stringResource(R.string.lostfound_field_title)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_title_hint)) },
                isError = form.titleError != null,
                supportingText = {
                    form.titleError?.let { err ->
                        Text(stringResource(err.messageRes), color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Danh mục đồ vật
            ReportCategorySelector(category = form.category, onAction = onAction)

            // Địa điểm
            OutlinedTextField(
                value = form.location,
                onValueChange = { onAction(LostFoundAction.UpdateReportLocation(it)) },
                label = { Text(stringResource(R.string.lostfound_field_location)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_location_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                },
                isError = form.locationError != null,
                supportingText = {
                    form.locationError?.let {
                        Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Mô tả chi tiết (Tùy chọn - Không bắt buộc)
            OutlinedTextField(
                value = form.description,
                onValueChange = { onAction(LostFoundAction.UpdateReportDescription(it)) },
                label = { Text(stringResource(R.string.lostfound_field_desc)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_desc_hint)) },
                supportingText = {
                    Text(
                        text = stringResource(R.string.lostfound_desc_counter, form.description.trim().length),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
            )

            // Khung chọn ảnh minh chứng (Tỷ lệ 16:9 khi có ảnh)
            ReportImagePicker(
                imageBytes = form.imageBytes,
                onAction = onAction,
            )

            // Người liên hệ (Tùy chọn)
            OutlinedTextField(
                value = form.contactName,
                onValueChange = { onAction(LostFoundAction.UpdateReportContactName(it)) },
                label = { Text(stringResource(R.string.lostfound_field_contact_name)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_contact_name_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Số điện thoại liên hệ (Bắt buộc)
            OutlinedTextField(
                value = form.contactPhone,
                onValueChange = { onAction(LostFoundAction.UpdateReportContactPhone(it)) },
                label = { Text(stringResource(R.string.lostfound_field_contact_phone)) },
                placeholder = { Text(stringResource(R.string.lostfound_field_contact_phone_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null)
                },
                isError = form.phoneError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = {
                    Text(
                        text = form.phoneError?.let { stringResource(it.messageRes) }
                            ?: stringResource(R.string.lostfound_phone_public_notice),
                        color = if (form.phoneError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Nút Submit chính dạng Google Pill Button
            val loadingDescription = stringResource(R.string.common_loading)
            Button(
                onClick = { onAction(LostFoundAction.SubmitReport) },
                enabled = !form.isSubmitting,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics {
                        if (form.isSubmitting) stateDescription = loadingDescription
                    },
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(UthDimens.Size.ProgressInline).clearAndSetSemantics { },
                        strokeWidth = UthDimens.Size.ProgressInlineStroke,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                }
                Text(
                    text = stringResource(R.string.lostfound_btn_submit),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Selector for choosing whether the item is Lost or Found.
 *
 * @param kind Currently selected listing kind.
 * @param onAction Action dispatcher callback.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun ReportKindSelector(
    kind: ListingKind,
    onAction: (LostFoundAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val isLost = kind == ListingKind.LOST
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .selectable(
                    selected = isLost,
                    role = Role.RadioButton,
                    onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.LOST)) },
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isLost) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
            border = BorderStroke(
                width = if (isLost) 2.dp else 1.dp,
                color = if (isLost) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.lostfound_kind_lost_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isLost) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLost) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        val isFound = kind == ListingKind.FOUND
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .selectable(
                    selected = isFound,
                    role = Role.RadioButton,
                    onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.FOUND)) },
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isFound) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
            border = BorderStroke(
                width = if (isFound) 2.dp else 1.dp,
                color = if (isFound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.lostfound_kind_found_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isFound) FontWeight.Bold else FontWeight.Medium,
                    color = if (isFound) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/**
 * Filter chip group to select the category for the reported item.
 *
 * @param category Currently selected category.
 * @param onAction Action dispatcher callback.
 * @param modifier Optional layout modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCategorySelector(
    category: ItemCategory,
    onAction: (LostFoundAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.lostfound_field_category),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                ItemCategory.STUDENT_CARD to R.string.lostfound_filter_student_card,
                ItemCategory.ELECTRONICS to R.string.lostfound_filter_electronics,
                ItemCategory.WALLET_PAPERS to R.string.lostfound_filter_wallet_papers,
                ItemCategory.PERSONAL_ITEMS to R.string.lostfound_filter_personal_items,
                ItemCategory.OTHER to R.string.lostfound_filter_other,
            ).forEach { (cat, labelRes) ->
                FilterChip(
                    selected = category == cat,
                    onClick = { onAction(LostFoundAction.UpdateReportCategory(cat)) },
                    label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
                    shape = CircleShape,
                )
            }
        }
    }
}

/**
 * Composable providing photo picker integration and image attachment preview.
 *
 * @param imageBytes Attached image bytes if available.
 * @param onAction Action dispatcher callback.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun ReportImagePicker(
    imageBytes: ByteArray?,
    onAction: (LostFoundAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    processAndCompressImage(context, uri)
                }
                when (result) {
                    is ImageProcessResult.Success -> {
                        onAction(LostFoundAction.UpdateReportImage(result.bytes, result.mimeType, uri.toString()))
                    }

                    ImageProcessResult.TooLarge -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.lostfound_image_too_large),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    ImageProcessResult.ReadFailed -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.lostfound_image_read_error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.lostfound_field_image),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val previewBitmap = remember(imageBytes) {
                runCatching {
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                }.getOrNull()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = stringResource(R.string.lostfound_image_attached_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                IconButton(
                    onClick = { onAction(LostFoundAction.RemoveReportImage) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.lostfound_btn_remove_image),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                ) {
                    Text(stringResource(R.string.lostfound_btn_change_image))
                }
                OutlinedButton(
                    onClick = { onAction(LostFoundAction.RemoveReportImage) },
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.lostfound_btn_remove_image))
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lostfound_btn_pick_image),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.lostfound_image_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val MAX_IMAGE_INPUT_BYTES = 15 * 1024 * 1024
private const val MAX_IMAGE_DIMENSION = 1280
private const val IMAGE_COMPRESSION_QUALITY = 80
private const val STREAM_BUFFER_CHUNK_SIZE = 8192
private const val ROTATION_90 = 90f
private const val ROTATION_180 = 180f
private const val ROTATION_270 = 270f

private fun readImageBytesBounded(
    context: Context,
    uri: Uri,
    maxBytes: Int,
): Pair<ByteArray?, Boolean> {
    val buffer = java.io.ByteArrayOutputStream()
    val stream = context.contentResolver.openInputStream(uri) ?: return Pair(null, false)
    val isTooLarge = stream.use { input ->
        val chunk = ByteArray(STREAM_BUFFER_CHUNK_SIZE)
        var total = 0
        var read = input.read(chunk)
        while (read != -1 && total + read <= maxBytes) {
            total += read
            buffer.write(chunk, 0, read)
            read = input.read(chunk)
        }
        read != -1
    }
    return Pair(if (isTooLarge) null else buffer.toByteArray(), isTooLarge)
}

private fun decodeExifOrientation(bytes: ByteArray): Int = runCatching {
    java.io.ByteArrayInputStream(bytes).use { exifStream ->
        val exif = ExifInterface(exifStream)
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun computeSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    var sampleSize = 1
    while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun scaleAndRotate(
    bitmap: Bitmap,
    orientation: Int,
    maxDimension: Int,
): Bitmap {
    val maxDim = maxOf(bitmap.width, bitmap.height)
    val scaled = if (maxDim > maxDimension) {
        val scale = maxDimension.toFloat() / maxDim
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else {
        bitmap
    }

    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> ROTATION_90
        ExifInterface.ORIENTATION_ROTATE_180 -> ROTATION_180
        ExifInterface.ORIENTATION_ROTATE_270 -> ROTATION_270
        else -> 0f
    }

    return if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true).also {
            if (it != scaled) scaled.recycle()
        }
    } else {
        scaled
    }
}

private fun compressRawBytes(
    rawBytes: ByteArray,
    maxDimension: Int,
    quality: Int,
): ImageProcessResult {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        return ImageProcessResult.ReadFailed
    }

    val orientation = decodeExifOrientation(rawBytes)
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = computeSampleSize(boundsOptions, maxDimension)
    }
    val sampledBitmap = BitmapFactory.decodeByteArray(
        rawBytes,
        0,
        rawBytes.size,
        decodeOptions,
    ) ?: return ImageProcessResult.ReadFailed

    val finalBitmap = scaleAndRotate(sampledBitmap, orientation, maxDimension)
    val outStream = java.io.ByteArrayOutputStream()
    try {
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
        return ImageProcessResult.Success(outStream.toByteArray(), "image/jpeg")
    } finally {
        finalBitmap.recycle()
    }
}

/**
 * Reads, scales down, handles EXIF orientation, and compresses the selected image into JPEG.
 *
 * @param context Android context for content resolution.
 * @param uri Image source URI.
 * @param maxInputBytes Maximum allowable size for the input stream before compression.
 * @param maxDimension Target maximum width/height for downscaling.
 * @param quality JPEG compression quality factor (0..100).
 * @return Outcome status wrapping compressed JPEG bytes or failure reason.
 */
internal fun processAndCompressImage(
    context: Context,
    uri: Uri,
    maxInputBytes: Int = MAX_IMAGE_INPUT_BYTES,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    quality: Int = IMAGE_COMPRESSION_QUALITY,
): ImageProcessResult = runCatching {
    val (rawBytes, isTooLarge) = readImageBytesBounded(context, uri, maxInputBytes)
    when {
        isTooLarge -> ImageProcessResult.TooLarge
        rawBytes == null || rawBytes.isEmpty() -> ImageProcessResult.ReadFailed
        else -> compressRawBytes(rawBytes, maxDimension, quality)
    }
}.getOrElse { ImageProcessResult.ReadFailed }

/**
 * Result representing outcome of user image selection and decoding.
 */
internal sealed interface ImageProcessResult {
    data class Success(val bytes: ByteArray, val mimeType: String) : ImageProcessResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
        }

        override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
    }

    data object TooLarge : ImageProcessResult
    data object ReadFailed : ImageProcessResult
}
