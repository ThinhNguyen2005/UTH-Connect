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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
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
                    UthEmptyState(
                        title = stringResource(R.string.lostfound_empty_list),
                        body = stringResource(R.string.lostfound_empty_desc),
                        icon = Icons.Default.Search,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(UthDimens.Spacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
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
private fun LostFoundItemCard(
    listing: LostFoundListing,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        // Tag Kind & Date
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

            Text(
                text = listing.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))

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

        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))

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
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                Text(
                    text = listing.contactName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                // Kind selector: Lost or Found
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    OutlinedButton(
                        onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.LOST)) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_kind_lost_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (form.kind == ListingKind.LOST) FontWeight.Bold else FontWeight.Normal,
                            color = if (form.kind == ListingKind.LOST) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    OutlinedButton(
                        onClick = { onAction(LostFoundAction.UpdateReportKind(ListingKind.FOUND)) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (form.kind == ListingKind.FOUND) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.lostfound_kind_found_btn),
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
                        form.titleError?.let { err ->
                            Text(stringResource(err.messageRes), color = MaterialTheme.colorScheme.error)
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
                        val error = form.descriptionError
                        if (error != null) {
                            val errorText = when (error) {
                                is FormValidationError.DescriptionTooShort -> stringResource(error.messageRes, error.currentLength)
                                else -> stringResource(error.messageRes)
                            }
                            Text(errorText, color = MaterialTheme.colorScheme.error)
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
            Button(
                onClick = { onAction(LostFoundAction.SubmitReport) },
                enabled = !form.isSubmitting,
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(UthDimens.Size.ProgressInline),
                        strokeWidth = UthDimens.Size.ProgressInlineStroke,
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
