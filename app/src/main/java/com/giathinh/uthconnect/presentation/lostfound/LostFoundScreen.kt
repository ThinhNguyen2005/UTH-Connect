package com.giathinh.uthconnect.presentation.lostfound

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundScreen(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
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
            // Tabs: Lost vs Found
            TabRow(
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

            // Content List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredListings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.lostfound_empty_list),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.filteredListings,
                        key = { it.id },
                    ) { listing ->
                        LostFoundItemCard(
                            listing = listing,
                            onCallClick = { phone ->
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(dialIntent)
                            },
                        )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun LostFoundItemCard(
    listing: LostFoundListing,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        // Tag Kind & Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (badgeText, badgeBg, badgeTextColor) = if (listing.kind == ListingKind.LOST) {
                Triple(
                    "CẦN TÌM",
                    UthTheme.extendedColors.dangerContainer,
                    UthTheme.extendedColors.onDangerContainer,
                )
            } else {
                Triple(
                    "NHẶT ĐƯỢC",
                    UthTheme.extendedColors.successContainer,
                    UthTheme.extendedColors.onSuccessContainer,
                )
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }

            Text(
                text = listing.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contact Row with Call button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = listing.contactName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (listing.contactPhone.isNotBlank()) {
                OutlinedButton(
                    onClick = { onCallClick(listing.contactPhone) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Kind selector: Lost or Found
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.LOST)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(
                            text = "Tôi bị mất đồ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (form.kind == ListingKind.LOST) FontWeight.Bold else FontWeight.Normal,
                            color = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    OutlinedButton(
                        onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.FOUND)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (form.kind == ListingKind.FOUND) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(
                            text = "Tôi nhặt được đồ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (form.kind == ListingKind.FOUND) FontWeight.Bold else FontWeight.Normal,
                            color = if (form.kind == ListingKind.FOUND) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
                        form.titleError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(
                        ItemCategory.STUDENT_CARD to "Thẻ SV/Xe",
                        ItemCategory.ELECTRONICS to "Điện tử",
                        ItemCategory.WALLET_PAPERS to "Ví/Giấy tờ",
                        ItemCategory.PERSONAL_ITEMS to "Đồ dùng",
                        ItemCategory.OTHER to "Khác",
                    ).forEach { (cat, label) ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { onAction(LostFoundAction.UpdateReportCategory(cat)) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                // Location
                OutlinedTextField(
                    value = form.location,
                    onValueChange = { onAction(LostFoundAction.UpdateReportLocation(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_location)) },
                    placeholder = { Text("Ví dụ: Phòng B204 CS1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Contact Name
                OutlinedTextField(
                    value = form.contactName,
                    onValueChange = { onAction(LostFoundAction.UpdateReportContactName(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_contact_name)) },
                    placeholder = { Text("Tên của bạn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Contact Phone
                OutlinedTextField(
                    value = form.contactPhone,
                    onValueChange = { onAction(LostFoundAction.UpdateReportContactPhone(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_contact_phone)) },
                    placeholder = { Text("Số điện thoại liên hệ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Description
                OutlinedTextField(
                    value = form.description,
                    onValueChange = { onAction(LostFoundAction.UpdateReportDescription(it)) },
                    label = { Text(stringResource(R.string.lostfound_field_desc)) },
                    placeholder = { Text(stringResource(R.string.lostfound_field_desc_hint)) },
                    isError = form.descriptionError != null,
                    supportingText = {
                        val errorText = form.descriptionError
                        if (errorText != null) {
                            Text(errorText, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${form.description.trim().length}/20 ký tự tối thiểu")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(LostFoundAction.SubmitReport) },
                enabled = !form.isSubmitting,
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.lostfound_btn_submit))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.lostfound_btn_cancel))
            }
        },
    )
}
