package com.giathinh.uthconnect.presentation.housing.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus

/**
 * Form đăng tin Ghép trọ — tuân thủ Design System (core/designsystem).
 * Header nhận diện thương hiệu UTH (nền Navy, ambient rings, logo UTH),
 * các cụm card thông tin có icon nhóm, không có banner xác thực theo yêu cầu.
 */
@Composable
fun CreateHousingPostScreen(
    uiState: CreateHousingPostUiState,
    onAction: (CreateHousingPostAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPickPhotos: () -> Unit = {},
    onPickLocation: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CreateHousingHeader(onNavigateBack = onNavigateBack) },
        bottomBar = {
            CreateBottomBar(
                isSubmitting = uiState.isSubmitting,
                onSaveDraft = { onAction(CreateHousingPostAction.SaveDraft) },
                onSubmit = { onAction(CreateHousingPostAction.Submit) },
            )
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LaunchedEffect(uiState.validationNonce) {
            if (uiState.validationError != null) {
                listState.animateScrollToItem(0)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
        ) {
            if (uiState.validationError != null) {
                item { ValidationErrorBanner(error = uiState.validationError) }
            }
            item {
                TypeAndCampusCard(
                    type = uiState.type,
                    campus = uiState.campus,
                    onTypeChange = { onAction(CreateHousingPostAction.ChangeType(it)) },
                    onCampusChange = { onAction(CreateHousingPostAction.ChangeCampus(it)) },
                )
            }
            item {
                LocationTitleCard(
                    title = uiState.title,
                    address = uiState.address,
                    area = uiState.areaInput,
                    hasLocation = uiState.hasLocation,
                    onTitleChange = { onAction(CreateHousingPostAction.ChangeTitle(it)) },
                    onAddressChange = { onAction(CreateHousingPostAction.ChangeAddress(it)) },
                    onAreaChange = { onAction(CreateHousingPostAction.ChangeArea(it)) },
                    onPickLocation = onPickLocation,
                )
            }
            item {
                CostRequirementsCard(
                    rentInput = uiState.rentInput,
                    depositInput = uiState.depositInput,
                    utilitiesIncluded = uiState.utilitiesIncluded,
                    slots = uiState.slots,
                    gender = uiState.genderPreference,
                    scheduleNote = uiState.scheduleNote,
                    onRentChange = { onAction(CreateHousingPostAction.ChangeRent(it)) },
                    onDepositChange = { onAction(CreateHousingPostAction.ChangeDeposit(it)) },
                    onUtilitiesChange = { onAction(CreateHousingPostAction.ChangeUtilitiesIncluded(it)) },
                    onSlotsChange = { onAction(CreateHousingPostAction.ChangeSlots(it)) },
                    onGenderChange = { onAction(CreateHousingPostAction.ChangeGender(it)) },
                    onScheduleChange = { onAction(CreateHousingPostAction.ChangeScheduleNote(it)) },
                )
            }
            item {
                AmenitiesCard(
                    selectedAmenities = uiState.selectedAmenities,
                    onToggle = { onAction(CreateHousingPostAction.ToggleAmenity(it)) },
                )
            }
            item {
                PhotosCard(
                    photos = uiState.photos,
                    onAddPhoto = onPickPhotos,
                    onRemovePhoto = { onAction(CreateHousingPostAction.RemovePhoto(it)) },
                )
            }
            item {
                DescriptionContactCard(
                    description = uiState.description,
                    phone = uiState.contactPhone,
                    zalo = uiState.contactZalo,
                    allowZalo = uiState.allowZalo,
                    onDescriptionChange = { onAction(CreateHousingPostAction.ChangeDescription(it)) },
                    onPhoneChange = { onAction(CreateHousingPostAction.ChangeContactPhone(it)) },
                    onZaloChange = { onAction(CreateHousingPostAction.ChangeContactZalo(it)) },
                    onAllowZaloChange = { onAction(CreateHousingPostAction.ChangeAllowZalo(it)) },
                )
            }
            item { GuidelinesNotice() }
        }
    }
}

/** Header nền xanh thương hiệu UTH đồng bộ với màn Ghép trọ. */
@Composable
private fun CreateHousingHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primary)
            .drawBehind {
                val ringRadius = size.width * UthDimens.Layout.HomeRingRadiusFraction
                val ringStroke = size.width * UthDimens.Layout.HomeRingStrokeFraction
                drawCircle(
                    color = colors.onPrimary.copy(alpha = UthDimens.Opacity.HomeHeaderRing),
                    radius = ringRadius,
                    center = Offset(size.width + ringRadius * UthDimens.Layout.HomeRingOffsetFraction, size.height),
                    style = Stroke(width = ringStroke),
                )
                drawCircle(
                    color = colors.secondary.copy(alpha = UthDimens.Opacity.HomeHeaderAccentRing),
                    radius = ringRadius * UthDimens.Layout.HomeAccentRingRadiusFactor,
                    center = Offset(size.width * UthDimens.Layout.HomeAccentRingCenterFraction, 0f),
                    style = Stroke(width = ringStroke * UthDimens.Layout.HomeAccentRingStrokeFactor),
                )
            }
            .padding(
                start = UthDimens.Spacing.Sm,
                end = UthDimens.Spacing.Lg,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Sm,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(UthDimens.Size.Avatar),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                tint = colors.onPrimary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .clip(MaterialTheme.shapes.small)
                .background(colors.onPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_logo_abbr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Md))
        Text(
            text = stringResource(R.string.housing_create_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Tiêu đề nhóm mục có icon danh mục nhận diện trực quan. */
@Composable
private fun FormSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    iconVector: ImageVector? = null,
    iconRes: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Cụm 1: Phân loại bài đăng & Cơ sở UTH. */
@Composable
private fun TypeAndCampusCard(
    type: HousingListingType,
    campus: UthCampus,
    onTypeChange: (HousingListingType) -> Unit,
    onCampusChange: (UthCampus) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconVector = Icons.Filled.Home,
            title = stringResource(R.string.housing_create_type_label),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        SegmentedControlWithIcons(
            items = listOf(
                SegmentedItem(
                    label = stringResource(R.string.housing_create_type_share),
                    icon = Icons.Filled.Person,
                ),
                SegmentedItem(
                    label = stringResource(R.string.housing_create_type_entire),
                    icon = Icons.Filled.Home,
                ),
                SegmentedItem(
                    label = stringResource(R.string.housing_type_looking_for_room),
                    icon = Icons.Filled.Place,
                ),
            ),
            selectedIndex = when (type) {
                HousingListingType.LOOKING_FOR_ROOMMATE -> 0
                HousingListingType.ROOM_FOR_RENT -> 1
                HousingListingType.LOOKING_FOR_ROOM -> 2
            },
            onSelect = { index ->
                onTypeChange(
                    when (index) {
                        0 -> HousingListingType.LOOKING_FOR_ROOMMATE
                        1 -> HousingListingType.ROOM_FOR_RENT
                        else -> HousingListingType.LOOKING_FOR_ROOM
                    },
                )
            },
            labelMaxLines = 2,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Lg))
        Text(
            text = stringResource(R.string.housing_create_campus_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.housing_create_campus_hint),
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            UthCampus.entries.forEach { entry ->
                FilterChip(
                    selected = campus == entry,
                    onClick = { onCampusChange(entry) },
                    label = { Text(stringResource(entry.labelRes()), style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconBadge),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

/**
 * Cụm 2: Thông tin vị trí, Tiêu đề & Diện tích.
 * Không có ô "khoảng cách tới trường": khoảng cách thật chỉ tính được từ tọa độ,
 * nhập tay sẽ bị ghi đè và hiển thị sai ở màn chi tiết.
 */
@Composable
private fun LocationTitleCard(
    title: String,
    address: String,
    area: String,
    hasLocation: Boolean,
    onTitleChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onAreaChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconVector = Icons.Filled.LocationOn,
            title = stringResource(R.string.housing_create_section_location),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.housing_create_title_label)) },
            placeholder = { Text(stringResource(R.string.housing_create_title_hint)) },
            trailingIcon = if (title.isNotEmpty()) {
                {
                    IconButton(onClick = { onTitleChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(R.string.housing_create_address_label)) },
            placeholder = { Text(stringResource(R.string.housing_create_address_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        OutlinedButton(
            onClick = onPickLocation,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UthDimens.Size.ButtonHeight),
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
            Text(stringResource(R.string.housing_location_pick_button))
        }
        if (hasLocation) {
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(R.string.housing_location_picked),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        OutlinedTextField(
            value = area,
            onValueChange = onAreaChange,
            label = { Text(stringResource(R.string.housing_create_area_label)) },
            placeholder = { Text("25") },
            trailingIcon = {
                Text(
                    text = "m²",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = UthDimens.Spacing.Sm),
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
    }
}

/** Cụm 3: Chi phí & Yêu cầu bạn cùng phòng. */
@Composable
private fun CostRequirementsCard(
    rentInput: String,
    depositInput: String,
    utilitiesIncluded: Boolean,
    slots: Int,
    gender: GenderPreference,
    scheduleNote: String,
    onRentChange: (String) -> Unit,
    onDepositChange: (String) -> Unit,
    onUtilitiesChange: (Boolean) -> Unit,
    onSlotsChange: (Int) -> Unit,
    onGenderChange: (GenderPreference) -> Unit,
    onScheduleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconRes = R.drawable.ic_housing_payments,
            title = stringResource(R.string.housing_create_section_cost),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            OutlinedTextField(
                value = rentInput,
                onValueChange = { onRentChange(it.filter(Char::isDigit)) },
                label = { Text(stringResource(R.string.housing_create_rent_label)) },
                trailingIcon = {
                    Text(
                        text = "đ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = UthDimens.Spacing.Sm),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = MoneyVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
            )
            OutlinedTextField(
                value = depositInput,
                onValueChange = { onDepositChange(it.filter(Char::isDigit)) },
                label = { Text(stringResource(R.string.housing_create_deposit_label)) },
                trailingIcon = {
                    Text(
                        text = "đ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = UthDimens.Spacing.Sm),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = MoneyVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = utilitiesIncluded, onCheckedChange = onUtilitiesChange)
            Text(
                text = stringResource(R.string.housing_create_utilities_included),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        SlotsStepper(slots = slots, onChange = onSlotsChange)
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Lg))
        Text(
            text = stringResource(R.string.housing_create_gender_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        SegmentedControlWithIcons(
            items = listOf(
                SegmentedItem(
                    label = stringResource(R.string.housing_gender_male),
                    iconRes = R.drawable.ic_gender_male,
                ),
                SegmentedItem(
                    label = stringResource(R.string.housing_gender_female),
                    iconRes = R.drawable.ic_gender_female,
                ),
                SegmentedItem(label = stringResource(R.string.housing_filter_all), icon = Icons.Filled.Check),
            ),
            selectedIndex = when (gender) {
                GenderPreference.MALE -> 0
                GenderPreference.FEMALE -> 1
                GenderPreference.ANY -> 2
            },
            onSelect = { index ->
                onGenderChange(
                    when (index) {
                        0 -> GenderPreference.MALE
                        1 -> GenderPreference.FEMALE
                        else -> GenderPreference.ANY
                    },
                )
            },
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Lg))
        OutlinedTextField(
            value = scheduleNote,
            onValueChange = onScheduleChange,
            label = { Text(stringResource(R.string.housing_create_schedule_label)) },
            placeholder = { Text(stringResource(R.string.housing_create_schedule_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
    }
}

private data class SegmentedItem(
    val label: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null, // drawable resource id
)

@Composable
private fun SegmentedControlWithIcons(
    items: List<SegmentedItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelMaxLines: Int = 1,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .border(BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant)),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = UthDimens.Size.SegmentedControlHeight),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    UthTheme.extendedColors.surfaceSubtle
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                        Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                    } else if (item.iconRes != null) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                        Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = labelMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotsStepper(slots: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.housing_create_slots_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.housing_create_slots_hint),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            val decreaseLabel = stringResource(R.string.housing_create_slots_decrease)
            IconButton(
                onClick = { onChange(slots - 1) },
                enabled = slots > 1,
                modifier = Modifier
                    .size(UthDimens.Size.MinTouchTarget)
                    .clip(MaterialTheme.shapes.small)
                    .background(UthTheme.extendedColors.surfaceSubtle)
                    .semantics { contentDescription = decreaseLabel },
            ) {
                Box(
                    modifier = Modifier
                        .width(UthDimens.Size.IconInline)
                        .height(UthDimens.Size.StrokeFocused)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = slots.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = UthDimens.Size.Avatar),
            )
            IconButton(
                onClick = { onChange(slots + 1) },
                modifier = Modifier
                    .size(UthDimens.Size.MinTouchTarget)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.housing_create_slots_increase),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Cụm 4: Tiện ích phòng trọ (Multi-select). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesCard(
    selectedAmenities: Set<RoomAmenity>,
    onToggle: (RoomAmenity) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconRes = R.drawable.ic_housing_amenities,
            title = stringResource(R.string.housing_create_amenities_label),
            trailing = {
                Text(
                    text = stringResource(
                        R.string.housing_create_amenities_count,
                        selectedAmenities.size,
                        RoomAmenity.entries.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            RoomAmenity.entries.forEach { amenity ->
                val isSelected = selectedAmenities.contains(amenity)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(amenity) },
                    shape = CircleShape,
                    border = null,
                    label = {
                        Text(
                            text = stringResource(amenity.labelRes()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(amenity.iconRes()),
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(UthDimens.Size.IconBadge),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = UthTheme.extendedColors.surfaceSubtle,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

/** Cụm 5: Hình ảnh thực tế. */
@Composable
private fun PhotosCard(
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconRes = R.drawable.ic_housing_photo_library,
            title = stringResource(R.string.housing_create_section_photos),
            trailing = {
                Text(
                    text = stringResource(R.string.housing_detail_photo_count, photos.size, MAX_HOUSING_PHOTOS),
                    style = MaterialTheme.typography.labelSmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            },
        )
        Text(
            text = stringResource(R.string.housing_create_photos_hint),
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        val showAddTile = photos.size < MAX_HOUSING_PHOTOS
        val cellCount = photos.size + if (showAddTile) 1 else 0
        val columns = UthDimens.Layout.HousingPhotoGridColumns
        for (row in (0 until cellCount).toList().chunked(columns)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                row.forEach { cell ->
                    if (cell < photos.size) {
                        PhotoThumb(
                            index = cell,
                            uri = photos[cell],
                            onRemove = { onRemovePhoto(cell) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        AddPhotoTile(onClick = onAddPhoto, modifier = Modifier.weight(1f))
                    }
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        }
    }
}

@Composable
private fun PhotoThumb(index: Int, uri: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (index == 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(UthDimens.Spacing.Xs),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.housing_create_photo_cover),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs),
                )
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.housing_create_photo_remove, index + 1),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = UthTheme.extendedColors.surfaceSubtle,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(UthDimens.Spacing.Sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_housing_camera),
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.housing_create_photo_add),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.housing_create_photo_max, MAX_HOUSING_PHOTOS),
                style = MaterialTheme.typography.labelSmall,
                color = UthTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Cụm 6: Mô tả chi tiết & Liên hệ. */
@Composable
private fun DescriptionContactCard(
    description: String,
    phone: String,
    zalo: String,
    allowZalo: Boolean,
    onDescriptionChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onZaloChange: (String) -> Unit,
    onAllowZaloChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        FormSectionHeader(
            iconRes = R.drawable.ic_housing_notes,
            title = stringResource(R.string.housing_create_section_contact),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.housing_create_desc_label)) },
            placeholder = { Text(stringResource(R.string.housing_create_desc_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            minLines = 3,
            maxLines = 6,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.housing_create_phone_label)) },
            placeholder = { Text(stringResource(R.string.housing_create_phone_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.housing_create_zalo_toggle),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.housing_create_zalo_toggle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }
            Switch(checked = allowZalo, onCheckedChange = onAllowZaloChange)
        }
        if (allowZalo) {
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            OutlinedTextField(
                value = zalo,
                onValueChange = onZaloChange,
                label = { Text(stringResource(R.string.housing_create_zalo_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
            )
        }
    }
}

/** Cụm 7: Hộp lưu ý cộng đồng UTH Connect. */
@Composable
private fun GuidelinesNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = UthTheme.extendedColors.infoContainer,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verified_shield),
                contentDescription = null,
                tint = UthTheme.extendedColors.onInfoContainer,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Column {
                Text(
                    text = stringResource(R.string.housing_create_guidelines_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = UthTheme.extendedColors.onInfoContainer,
                )
                Text(
                    text = stringResource(R.string.housing_create_guidelines_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.onInfoContainer,
                )
            }
        }
    }
}

@Composable
private fun ValidationErrorBanner(error: CreateHousingPostError, modifier: Modifier = Modifier) {
    val messageRes = when (error) {
        CreateHousingPostError.EMPTY_TITLE -> R.string.housing_create_error_title
        CreateHousingPostError.INVALID_TITLE_LENGTH -> R.string.housing_create_error_title_length
        CreateHousingPostError.INVALID_RENT -> R.string.housing_create_error_rent
        CreateHousingPostError.INVALID_DEPOSIT -> R.string.housing_create_error_deposit
        CreateHousingPostError.INVALID_AREA -> R.string.housing_create_error_area
        CreateHousingPostError.EMPTY_ADDRESS -> R.string.housing_create_error_address
        CreateHousingPostError.EMPTY_PHONE -> R.string.housing_create_error_phone
        CreateHousingPostError.INVALID_ZALO -> R.string.housing_create_error_zalo
        CreateHousingPostError.SIGN_IN_REQUIRED -> R.string.housing_create_sign_in_required
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Thanh hành động cố định: Lưu nháp (tonal) + Đăng tin ngay (primary). */
@Composable
private fun CreateBottomBar(
    isSubmitting: Boolean,
    onSaveDraft: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = UthDimens.Elevation.BottomBar,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onSaveDraft,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = UthDimens.Size.ButtonHeight),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(R.string.housing_create_save_draft),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            UthPrimaryButton(
                text = stringResource(R.string.housing_create_submit),
                onClick = onSubmit,
                loading = isSubmitting,
                leadingIcon = Icons.Filled.Send,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@DrawableRes
private fun RoomAmenity.iconRes(): Int = when (this) {
    RoomAmenity.MEZZANINE -> R.drawable.ic_amenity_mezzanine
    RoomAmenity.AIR_CONDITIONER -> R.drawable.ic_amenity_ac
    RoomAmenity.MACHINE_WASH -> R.drawable.ic_amenity_washing
    RoomAmenity.FREE_HOURS -> R.drawable.ic_amenity_free_hours
    RoomAmenity.GOVERNMENT_UTILITY_RATES -> R.drawable.ic_amenity_bolt
    RoomAmenity.SECURED_PARKING -> R.drawable.ic_amenity_parking
    RoomAmenity.COOKING -> R.drawable.ic_amenity_cooking
    RoomAmenity.HIGH_SPEED_WIFI -> R.drawable.ic_amenity_wifi
    RoomAmenity.HOT_WATER -> R.drawable.ic_amenity_hot_water
    RoomAmenity.BALCONY -> R.drawable.ic_amenity_balcony
    RoomAmenity.KEY_CARD -> R.drawable.ic_amenity_key_card
}

@StringRes
private fun RoomAmenity.labelRes(): Int = when (this) {
    RoomAmenity.MEZZANINE -> R.string.housing_amenity_mezzanine
    RoomAmenity.AIR_CONDITIONER -> R.string.housing_amenity_air_conditioner
    RoomAmenity.MACHINE_WASH -> R.string.housing_amenity_machine_wash
    RoomAmenity.FREE_HOURS -> R.string.housing_amenity_free_hours
    RoomAmenity.GOVERNMENT_UTILITY_RATES -> R.string.housing_amenity_government_utility_rates
    RoomAmenity.SECURED_PARKING -> R.string.housing_amenity_secured_parking
    RoomAmenity.COOKING -> R.string.housing_amenity_cooking
    RoomAmenity.HIGH_SPEED_WIFI -> R.string.housing_amenity_wifi
    RoomAmenity.HOT_WATER -> R.string.housing_amenity_hot_water
    RoomAmenity.BALCONY -> R.string.housing_amenity_balcony
    RoomAmenity.KEY_CARD -> R.string.housing_amenity_key_card
}

@StringRes
private fun UthCampus.labelRes(): Int = when (this) {
    UthCampus.CS1_QUANG_TRUNG -> R.string.housing_campus_cs1
    UthCampus.CS2_THU_DUC -> R.string.housing_campus_cs2
    UthCampus.CS3_TO_KY -> R.string.housing_campus_cs3
}

/**
 * Nhóm 3 chữ số bằng dấu chấm ngay trong ô nhập (`1500000` → `1.500.000`);
 * UiState vẫn giữ chuỗi chữ số thô nên `parseAmount` bên ViewModel không đổi.
 */
private class MoneyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = buildString {
            digits.forEachIndexed { index, char ->
                append(char)
                val remaining = digits.length - index - 1
                if (remaining > 0 && remaining % GROUP_SIZE == 0) append('.')
            }
        }
        // Ranh giới đặt dấu chấm tính từ phải sang: sau 1, 4, 7… chữ số (với "1500000").
        val boundaries = (digits.length - GROUP_SIZE downTo 1 step GROUP_SIZE).sorted()
        val dotOffsets = boundaries.mapIndexed { index, boundary -> boundary + index }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + boundaries.count { it < offset }
            override fun transformedToOriginal(offset: Int): Int = offset - dotOffsets.count { it < offset }
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }

    private companion object {
        /** Kiểu nhóm chữ số Việt Nam: mỗi 3 chữ số một dấu chấm. */
        const val GROUP_SIZE = 3
    }
}
