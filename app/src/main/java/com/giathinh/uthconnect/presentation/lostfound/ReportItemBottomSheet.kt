package com.giathinh.uthconnect.presentation.lostfound

import android.graphics.BitmapFactory
import android.net.Uri
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
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val maxBytes = 5 * 1024 * 1024
                            val buffer = stream.readBytes()
                            if (buffer.size <= maxBytes) buffer else null
                        }
                    }.getOrNull()
                }
                if (bytes != null) {
                    onAction(LostFoundAction.UpdateReportImage(bytes, mimeType, uri.toString()))
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
