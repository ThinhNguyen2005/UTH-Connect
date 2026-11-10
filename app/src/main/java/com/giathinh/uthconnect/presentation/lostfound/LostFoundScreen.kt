package com.giathinh.uthconnect.presentation.lostfound

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.R
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

private const val TIMEOUT_MILLIS = 10_000

/**
 * Root Composable for the Lost & Found screen with Scaffold, TopAppBar, FAB, and BottomSheet.
 *
 * @param uiState Current Lost & Found UI state.
 * @param onAction Callback to dispatch user actions.
 * @param onNavigateBack Callback when back navigation is triggered.
 * @param modifier Optional layout modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundScreen(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onCallClick: (String) -> Unit = {},
) {
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
                text = { Text(stringResource(R.string.lostfound_post_action), fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LostFoundSearchBar(
                query = uiState.searchQuery,
                selectedKind = uiState.selectedKind,
                onQueryChange = { onAction(LostFoundAction.UpdateSearchQuery(it)) },
            )

            KindSegmentedRow(
                selectedKind = uiState.selectedKind,
                lostCount = uiState.lostCount,
                foundCount = uiState.foundCount,
                onSelectKind = { onAction(LostFoundAction.SelectKind(it)) },
            )

            CategoryFilterRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { onAction(LostFoundAction.SelectCategory(it)) },
            )

            LostFoundContentBody(
                uiState = uiState,
                onAction = onAction,
                onCallClick = onCallClick,
            )
        }
    }

    if (uiState.isReportDialogOpen) {
        ReportItemBottomSheet(
            form = uiState.reportForm,
            onAction = onAction,
            onDismiss = { onAction(LostFoundAction.CloseReportDialog) },
        )
    }
}

/**
 * Real-time search bar section for filtering lost and found items.
 *
 * @param query Current search keyword.
 * @param selectedKind Active tab (Lost or Found) to adapt the search placeholder hint.
 * @param onQueryChange Callback triggered on text input change.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun LostFoundSearchBar(
    query: String,
    selectedKind: ListingKind,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
        placeholder = {
            Text(
                text = if (selectedKind == ListingKind.LOST) {
                    stringResource(R.string.lostfound_search_hint_lost)
                } else {
                    stringResource(R.string.lostfound_search_hint_found)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.lostfound_search_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.lostfound_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

/**
 * Segmented row allowing switching between Lost items and Found items.
 *
 * @param selectedKind Active listing kind.
 * @param lostCount Number of lost items.
 * @param foundCount Number of found items.
 * @param onSelectKind Callback triggered on tab switch.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun KindSegmentedRow(
    selectedKind: ListingKind,
    lostCount: Int,
    foundCount: Int,
    onSelectKind: (ListingKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val lostSelected = selectedKind == ListingKind.LOST
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelectKind(ListingKind.LOST) },
                shape = CircleShape,
                color = if (lostSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (lostSelected) 2.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.lostfound_tab_lost, lostCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (lostSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (lostSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            val foundSelected = selectedKind == ListingKind.FOUND
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelectKind(ListingKind.FOUND) },
                shape = CircleShape,
                color = if (foundSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (foundSelected) 2.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.lostfound_tab_found, foundCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (foundSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (foundSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/**
 * Main scrollable content displaying search bar, kind tabs, category filter, and listing items.
 *
 * @param uiState Current Lost & Found UI state.
 * @param onAction Callback to dispatch user actions.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun LostFoundContentBody(
    uiState: LostFoundUiState,
    onAction: (LostFoundAction) -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
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
                modifier = modifier,
            )
        }

        uiState.filteredListings.isEmpty() -> {
            if (uiState.searchQuery.isNotBlank()) {
                UthEmptyState(
                    title = stringResource(R.string.lostfound_no_results_title),
                    body = stringResource(R.string.lostfound_no_results_desc, uiState.searchQuery),
                    icon = Icons.Default.Search,
                    modifier = modifier,
                )
            } else {
                UthEmptyState(
                    title = stringResource(R.string.lostfound_empty_list),
                    body = stringResource(R.string.lostfound_empty_desc),
                    icon = Icons.Default.Search,
                    modifier = modifier,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = UthDimens.Spacing.Lg,
                    end = UthDimens.Spacing.Lg,
                    top = UthDimens.Spacing.Sm,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    items = uiState.filteredListings,
                    key = { it.id },
                ) { listing ->
                    LostFoundItemCard(
                        listing = listing,
                        onCallClick = onCallClick,
                        onToggleResolve = {
                            onAction(LostFoundAction.ToggleResolveListing(listing.id))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Horizontally scrollable row of category filter chips.
 *
 * @param selectedCategory Currently active category filter.
 * @param onSelectCategory Callback triggered when a category chip is clicked.
 * @param modifier Optional layout modifier.
 */
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
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
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
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

/**
 * Asynchronously loads and renders an image with strict HTTPS and host allowlist validation.
 *
 * @param imageUrl The remote URL of the image to display.
 * @param contentDescription Accessibility description for the image.
 * @param modifier Optional layout modifier.
 */
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
                val parsed = URL(imageUrl)
                require(
                    parsed.protocol == "https" &&
                        (
                            imageUrl.startsWith("${BuildConfig.SUPABASE_URL}/storage/v1/object/public/lost-found/") ||
                                parsed.host == "images.unsplash.com"
                            ),
                ) { "Untrusted image host" }
                val connection = (parsed.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MILLIS
                    readTimeout = TIMEOUT_MILLIS
                }
                connection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
        isLoading = false
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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

/**
 * Material 3 elevated card representing a lost or found item.
 *
 * @param listing Item data to render.
 * @param onToggleResolve Callback triggered when toggling the item resolved status.
 * @param onCall Contact phone click handler.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun LostFoundItemCard(
    listing: LostFoundListing,
    onCallClick: (String) -> Unit,
    onToggleResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ItemCardHeader(listing = listing)

            if (!listing.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncListingImage(
                    imageUrl = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (listing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (listing.contactName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.lostfound_poster_prefix, listing.contactName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ItemCardActions(
                listing = listing,
                onCallClick = onCallClick,
                onToggleResolve = onToggleResolve,
            )
        }
    }
}

/**
 * Header section of the item card displaying badge, title, date, and category.
 *
 * @param listing Item metadata.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun ItemCardHeader(
    listing: LostFoundListing,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(badgeTextRes),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }

            if (listing.isResolved) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = UthTheme.extendedColors.successContainer,
                    shape = CircleShape,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = UthTheme.extendedColors.onSuccessContainer,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (listing.kind == ListingKind.LOST) {
                                stringResource(R.string.lostfound_status_resolved_lost)
                            } else {
                                stringResource(R.string.lostfound_status_resolved_found)
                            },
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
}

/**
 * Action buttons at the bottom of the card for status toggling and phone contact.
 *
 * @param listing Item metadata.
 * @param onToggleResolve Status toggle callback.
 * @param onCall Contact phone click callback.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun ItemCardActions(
    listing: LostFoundListing,
    onCallClick: (String) -> Unit,
    onToggleResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onToggleResolve,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = if (listing.isResolved) {
                ButtonDefaults.outlinedButtonColors(
                    contentColor = UthTheme.extendedColors.onSuccessContainer,
                    containerColor = UthTheme.extendedColors.successContainer.copy(alpha = 0.2f),
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
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (listing.isResolved) {
                    if (listing.kind == ListingKind.LOST) {
                        stringResource(R.string.lostfound_status_resolved_lost)
                    } else {
                        stringResource(R.string.lostfound_status_resolved_found)
                    }
                } else {
                    if (listing.kind == ListingKind.LOST) {
                        stringResource(R.string.lostfound_action_mark_lost)
                    } else {
                        stringResource(R.string.lostfound_action_mark_found)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }

        if (listing.contactPhone.isNotBlank()) {
            Button(
                onClick = { onCallClick(listing.contactPhone) },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.lostfound_call_contact),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
