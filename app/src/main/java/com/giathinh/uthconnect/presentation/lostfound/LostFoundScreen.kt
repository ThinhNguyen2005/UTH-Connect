package com.giathinh.uthconnect.presentation.lostfound

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
    val cleanNumber = phoneNumber.replace(" ", "").trim()
    if (cleanNumber.isNotBlank()) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
        }
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundScreen(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = uiState.userMessage?.let { stringResource(it.messageRes) }
    LaunchedEffect(uiState.userMessage) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onAction(LostFoundAction.DismissMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.lostfound_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(LostFoundAction.OpenReportDialog) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.lostfound_post_action)) },
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
            // Thanh tìm kiếm thời gian thực chuẩn Google Pill
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onAction(LostFoundAction.UpdateSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
                placeholder = {
                    Text(
                        text = if (uiState.selectedKind == ListingKind.LOST) {
                            "Tìm đồ thất lạc (ví, thẻ sinh viên...)"
                        } else {
                            "Tìm đồ nhặt được..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onAction(LostFoundAction.UpdateSearchQuery("")) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            // Tabs: Lost vs Found
            PrimaryTabRow(
                selectedTabIndex = if (uiState.selectedKind == ListingKind.LOST) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(
                    selected = uiState.selectedKind == ListingKind.LOST,
                    onClick = { onAction(LostFoundAction.SelectKind(ListingKind.LOST)) },
                    text = {
                        Text(
                            text = stringResource(R.string.lostfound_tab_lost, uiState.lostCount),
                            fontWeight = if (uiState.selectedKind == ListingKind.LOST) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                Tab(
                    selected = uiState.selectedKind == ListingKind.FOUND,
                    onClick = { onAction(LostFoundAction.SelectKind(ListingKind.FOUND)) },
                    text = {
                        Text(
                            text = stringResource(R.string.lostfound_tab_found, uiState.foundCount),
                            fontWeight = if (uiState.selectedKind == ListingKind.FOUND) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }

            // Category Filter Chips
            CategoryFilterRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { onAction(LostFoundAction.SelectCategory(it)) },
            )

            // Content List / Loading / Error / Empty
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(UthDimens.Size.Progress),
                            strokeWidth = UthDimens.Size.ProgressStroke,
                        )
                    }
                }

                uiState.isError -> {
                    UthEmptyState(
                        title = stringResource(R.string.lostfound_load_error_title),
                        body = stringResource(R.string.lostfound_load_error_desc),
                        icon = Icons.Filled.Warning,
                        actionLabel = stringResource(R.string.common_retry),
                        onAction = { onAction(LostFoundAction.Retry) },
                    )
                }

                uiState.filteredListings.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank()) {
                        UthEmptyState(
                            title = "Không tìm thấy kết quả",
                            body = "Không có tin đăng nào phù hợp với từ khóa " +
                                "\"${uiState.searchQuery}\". Hãy thử từ khóa khác.",
                            icon = Icons.Default.Search,
                        )
                    } else {
                        UthEmptyState(
                            title = stringResource(R.string.lostfound_empty_list),
                            body = stringResource(R.string.lostfound_empty_desc),
                            icon = Icons.Default.Search,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = UthDimens.Spacing.Lg,
                            end = UthDimens.Spacing.Lg,
                            bottom = 80.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                    ) {
                        items(
                            items = uiState.filteredListings,
                            key = { it.id },
                        ) { listing ->
                            LostFoundItemCard(
                                listing = listing,
                                onCallClick = { phone ->
                                    dialPhoneNumber(context, phone)
                                },
                                onToggleResolve = {
                                    onAction(LostFoundAction.ToggleResolveListing(listing.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isReportDialogOpen) {
        ReportItemDialog(
            form = uiState.reportForm,
            onAction = onAction,
            onDismiss = { onAction(LostFoundAction.CloseReportDialog) },
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ItemCategory,
    onCategorySelected: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        ItemCategory.entries.forEach { cat ->
            val label = when (cat) {
                ItemCategory.ALL -> stringResource(R.string.lostfound_filter_all)
                ItemCategory.STUDENT_CARD -> stringResource(R.string.lostfound_filter_student_card)
                ItemCategory.ELECTRONICS -> stringResource(R.string.lostfound_filter_electronics)
                ItemCategory.WALLET_PAPERS -> stringResource(R.string.lostfound_filter_wallet_papers)
                ItemCategory.PERSONAL_ITEMS -> stringResource(R.string.lostfound_filter_personal_items)
                ItemCategory.OTHER -> stringResource(R.string.lostfound_filter_other)
            }
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun AsyncListingImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                connection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
        isLoading = false
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun LostFoundItemCard(
    listing: LostFoundListing,
    onCallClick: (String) -> Unit,
    onToggleResolve: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        // Tag Kind, Trạng thái giải quyết (Resolved) & Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (badgeTextRes, badgeBg, badgeTextColor) = if (listing.kind == ListingKind.LOST) {
                Triple(
                    R.string.lostfound_badge_lost,
                    UthTheme.extendedColors.dangerContainer,
                    UthTheme.extendedColors.onDangerContainer,
                )
            } else {
                Triple(
                    R.string.lostfound_badge_found,
                    UthTheme.extendedColors.successContainer,
                    UthTheme.extendedColors.onSuccessContainer,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeBg,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(badgeTextRes),
                        modifier = Modifier.padding(
                            horizontal = UthDimens.Spacing.Sm,
                            vertical = UthDimens.Spacing.Xs,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                    )
                }

                // Huy hiệu trạng thái đã nhận lại / đã trao trả
                if (listing.isResolved) {
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    Surface(
                        color = UthTheme.extendedColors.successContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = UthDimens.Spacing.Sm,
                                vertical = UthDimens.Spacing.Xs,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = UthTheme.extendedColors.onSuccessContainer,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (listing.kind == ListingKind.LOST) "Đã nhận lại" else "Đã trao trả",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UthTheme.extendedColors.onSuccessContainer,
                            )
                        }
                    }
                }
            }

            Text(
                text = listing.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))

        // Image if present
        if (!listing.imageUrl.isNullOrBlank()) {
            AsyncListingImage(
                imageUrl = listing.imageUrl,
                contentDescription = listing.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        }

        // Title
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))

        // Description
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))

        // Location
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Contact Name
        if (listing.contactName.isNotBlank()) {
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                Text(
                    text = "Người liên hệ: ${listing.contactName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Action Buttons Row: Đánh dấu đã nhận lại & Gọi điện
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Nút đánh dấu đã nhận lại / đã trao trả
            OutlinedButton(
                onClick = onToggleResolve,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(
                    horizontal = UthDimens.Spacing.Sm,
                    vertical = UthDimens.Spacing.Xs,
                ),
                colors = if (listing.isResolved) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = UthTheme.extendedColors.onSuccessContainer,
                        containerColor = UthTheme.extendedColors.successContainer.copy(alpha = 0.3f),
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Icon(
                    imageVector = if (listing.isResolved) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (listing.isResolved) {
                        if (listing.kind == ListingKind.LOST) "Đã nhận lại" else "Đã trao trả"
                    } else {
                        if (listing.kind == ListingKind.LOST) "Đánh dấu đã tìm thấy" else "Đánh dấu đã trả đồ"
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            // Nút gọi điện
            if (listing.contactPhone.isNotBlank()) {
                OutlinedButton(
                    onClick = { onCallClick(listing.contactPhone) },
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(
                        horizontal = UthDimens.Spacing.Md,
                        vertical = UthDimens.Spacing.Xs,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(UthDimens.Size.IconBadge),
                    )
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                    Text(
                        text = stringResource(R.string.lostfound_call_contact),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportItemDialog(
    form: ReportFormState,
    onAction: (LostFoundAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            onAction(LostFoundAction.UpdateReportImage(bytes, mimeType, uri.toString()))
        }
    }

    AlertDialog(
        onDismissRequest = { if (!form.isSubmitting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.lostfound_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                // Kind selector: Lost or Found
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f)
                            .heightIn(min = UthDimens.Size.MinTouchTarget)
                            .selectable(
                                selected = form.kind == ListingKind.LOST,
                                role = Role.RadioButton,
                                onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.LOST)) },
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_kind_lost_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (form.kind == ListingKind.LOST) FontWeight.Bold else FontWeight.Normal,
                            color = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(UthDimens.Spacing.Md),
                        )
                    }

                    Surface(
                        modifier = Modifier.weight(1f)
                            .heightIn(min = UthDimens.Size.MinTouchTarget)
                            .selectable(
                                selected = form.kind == ListingKind.FOUND,
                                role = Role.RadioButton,
                                onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.FOUND)) },
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (form.kind == ListingKind.FOUND) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_kind_found_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (form.kind == ListingKind.FOUND) FontWeight.Bold else FontWeight.Normal,
                            color = if (form.kind == ListingKind.FOUND) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(UthDimens.Spacing.Md),
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { onAction(LostFoundAction.UpdateReportTitle(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_title)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_title_hint)) },
                    isError = form.titleError != null,
                    supportingText = {
                        form.titleError?.let { err ->
                            Text(stringResource(err.messageRes), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Category chips
                Text(
                    text = stringResource(R.string.lostfound_field_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                ) {
                    listOf(
                        ItemCategory.STUDENT_CARD to R.string.lostfound_filter_student_card,
                        ItemCategory.ELECTRONICS to R.string.lostfound_filter_electronics,
                        ItemCategory.WALLET_PAPERS to R.string.lostfound_filter_wallet_papers,
                        ItemCategory.PERSONAL_ITEMS to R.string.lostfound_filter_personal_items,
                        ItemCategory.OTHER to R.string.lostfound_filter_other,
                    ).forEach { (cat, labelRes) ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { onAction(LostFoundAction.UpdateReportCategory(cat)) },
                            label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                // Location
                OutlinedTextField(
                    value = form.location,
                    onValueChange = { onAction(LostFoundAction.UpdateReportLocation(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_location)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_location_hint)) },
                    isError = form.locationError != null,
                    supportingText = { form.locationError?.let { Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.onErrorContainer) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Contact Name
                OutlinedTextField(
                    value = form.contactName,
                    onValueChange = { onAction(LostFoundAction.UpdateReportContactName(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_contact_name)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_contact_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Contact Phone
                OutlinedTextField(
                    value = form.contactPhone,
                    onValueChange = { onAction(LostFoundAction.UpdateReportContactPhone(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_contact_phone)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_contact_phone_hint)) },
                    isError = form.phoneError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    supportingText = {
                        Text(
                            text = form.phoneError?.let { stringResource(it.messageRes) }
                                ?: stringResource(R.string.lostfound_phone_public_notice),
                            color = if (form.phoneError != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Image attachment (PickVisualMedia)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                ) {
                    Text(
                        text = stringResource(R.string.lostfound_field_image),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val imageBytes = form.imageBytes
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        val previewBitmap = remember(imageBytes) {
                            runCatching {
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                            }.getOrNull()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap,
                                    contentDescription = "Ảnh đính kèm",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            IconButton(
                                onClick = { onAction(LostFoundAction.RemoveReportImage) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(UthDimens.Spacing.Xs)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(50)),
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
                            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.lostfound_btn_change_image))
                            }
                            OutlinedButton(
                                onClick = { onAction(LostFoundAction.RemoveReportImage) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(stringResource(R.string.lostfound_btn_remove_image))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(UthDimens.Spacing.Sm))
                            Text(stringResource(R.string.lostfound_btn_pick_image))
                        }
                        Text(
                            text = stringResource(R.string.lostfound_image_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Description
                OutlinedTextField(
                    value = form.description,
                    onValueChange = { onAction(LostFoundAction.UpdateReportDescription(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_desc)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_desc_hint)) },
                    isError = form.descriptionError != null,
                    supportingText = {
                        val error = form.descriptionError
                        if (error != null) {
                            val errorText = when (error) {
                                is FormValidationError.DescriptionTooShort -> stringResource(error.messageRes, error.currentLength)
                                else -> stringResource(error.messageRes)
                            }
                            Text(errorText, color = MaterialTheme.colorScheme.onErrorContainer)
                        } else {
                            Text(stringResource(R.string.lostfound_desc_counter, form.description.trim().length))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            val loadingDescription = stringResource(R.string.common_loading)
            Button(
                onClick = { onAction(LostFoundAction.SubmitReport) },
                enabled = !form.isSubmitting,
                modifier = Modifier.semantics {
                    if (form.isSubmitting) stateDescription = loadingDescription
                },
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(UthDimens.Size.ProgressInline).clearAndSetSemantics { },
                        strokeWidth = UthDimens.Size.ProgressInlineStroke,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                }
                Text(stringResource(R.string.lostfound_btn_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.isSubmitting) {
                Text(stringResource(R.string.lostfound_btn_cancel))
            }
        },
    )
}
