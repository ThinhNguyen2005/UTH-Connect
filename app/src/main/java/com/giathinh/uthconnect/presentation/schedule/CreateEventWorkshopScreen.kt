package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Giao diện Tạo sự kiện Workshop dành cho Giảng viên & Ban tổ chức.
 * Thiết kế chuẩn theo bản thiết kế mobile (Design System UTH Connect).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventWorkshopScreen(
    onBack: () -> Unit,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var eventTitle by remember {
        mutableStateOf("Workshop: Ứng dụng AI & IoT trong Giao thông Đô thị 2026")
    }

    val categories = remember {
        listOf(
            "Hội thảo / Workshop",
            "Học thuật / NCKH",
            "Tọa đàm Doanh nghiệp",
            "Cuộc thi Sinh viên",
        )
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val coOrganizers = remember {
        mutableStateListOf("Đoàn - Hội CNTT", "CLB Sáng tạo Trẻ")
    }
    var showAddOrganizerDialog by remember { mutableStateOf(false) }
    var newOrganizerName by remember { mutableStateOf("") }

    var summaryDescription by remember {
        mutableStateOf("Cung cấp kiến thức chuyên sâu về tích hợp mạng cảm biến thông minh và thị giác máy tính trong hệ thống điều phối xe buýt đô thị và luồng cảng biển thông minh tại TP.HCM. Cơ hội giao lưu trực tiếp cùng chuyên gia hàng đầu từ Sở GTVT.")
    }

    var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 11, 15)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedTimeSlot by remember { mutableStateOf("08:30 - 11:30") }

    val venues = remember {
        listOf(
            VenueOption(
                name = "Hội trường A.101",
                location = "Cơ sở 1: Số 2 Võ Oanh, P.25, Q. Bình Thạnh",
                status = "Đang chọn",
            ),
            VenueOption(
                name = "Hội trường B.202",
                location = "Cơ sở 2: Nguyễn Xí • Sức chứa 250 chỗ",
                status = "Trống",
            ),
            VenueOption(
                name = "Hội trường C",
                location = "Cơ sở 3: Tô Ký, Q.12 • Sức chứa 600 chỗ",
                status = "Trống",
            ),
        )
    }
    var selectedVenueIndex by remember { mutableIntStateOf(0) }

    val managers = remember {
        mutableStateListOf(
            ManagerOption("ThS. Trần Thu Trang", "TT", true),
            ManagerOption("SV. Lê Tuấn Anh (K21)", "TA", false),
        )
    }
    var showAddManagerDialog by remember { mutableStateOf(false) }
    var newManagerName by remember { mutableStateOf("") }

    val ext = UthTheme.extendedColors
    val colors = MaterialTheme.colorScheme
    val navyColor = colors.primary

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddOrganizerDialog) {
        AlertDialog(
            onDismissRequest = { showAddOrganizerDialog = false },
            title = { Text("Thêm đơn vị phối hợp") },
            text = {
                OutlinedTextField(
                    value = newOrganizerName,
                    onValueChange = { newOrganizerName = it },
                    label = { Text("Tên đơn vị") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newOrganizerName.isNotBlank()) {
                        coOrganizers.add(newOrganizerName.trim())
                        newOrganizerName = ""
                    }
                    showAddOrganizerDialog = false
                }) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrganizerDialog = false }) {
                    Text("Hủy")
                }
            },
        )
    }

    if (showAddManagerDialog) {
        AlertDialog(
            onDismissRequest = { showAddManagerDialog = false },
            title = { Text("Thêm thành viên Ban quản lý") },
            text = {
                OutlinedTextField(
                    value = newManagerName,
                    onValueChange = { newManagerName = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newManagerName.isNotBlank()) {
                        val trimmed = newManagerName.trim()
                        val initials = trimmed.split(" ")
                            .filter { it.isNotBlank() }
                            .takeLast(2)
                            .map { it.take(1).uppercase() }
                            .joinToString("")
                        managers.add(ManagerOption(trimmed, initials.ifBlank { "QL" }, false))
                        newManagerName = ""
                    }
                    showAddManagerDialog = false
                }) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManagerDialog = false }) {
                    Text("Hủy")
                }
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                shadowElevation = UthDimens.Elevation.Card,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = navyColor,
                        )
                    }
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    Column {
                        Text(
                            text = "Create Event Workshop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = navyColor,
                        )
                        Text(
                            text = "Dành cho Giảng viên & Ban tổ chức",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md)
                    .padding(bottom = UthDimens.Size.ButtonHeight + UthDimens.Spacing.Xxl),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
            ) {
                // Section 1: Basic Information Form
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surface,
                    border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = UthDimens.Elevation.Card,
                ) {
                    Column(
                        modifier = Modifier.padding(UthDimens.Spacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                    ) {
                        // Tên sự kiện
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tên sự kiện ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = "*",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.error,
                                )
                            }
                            OutlinedTextField(
                                value = eventTitle,
                                onValueChange = { eventTitle = it },
                                placeholder = {
                                    Text(
                                        text = "VD: Workshop: Ứng dụng AI & IoT trong Giao thông Đô thị...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ext.textTertiary,
                                    )
                                },
                                singleLine = false,
                                maxLines = 3,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ext.surfaceSubtle,
                                    unfocusedContainerColor = ext.surfaceSubtle,
                                    focusedBorderColor = navyColor,
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Phân loại học thuật & phong trào
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                            Text(
                                text = "Phân loại học thuật & phong trào",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                            ) {
                                categories.take(2).forEachIndexed { index, cat ->
                                    val isSelected = selectedCategoryIndex == index
                                    Surface(
                                        onClick = { selectedCategoryIndex = index },
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = if (isSelected) navyColor else ext.surfaceSubtle,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            color = if (isSelected) colors.onPrimary else ext.textSecondary,
                                            modifier = Modifier.padding(vertical = UthDimens.Spacing.Sm, horizontal = UthDimens.Spacing.Xs),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                            ) {
                                categories.drop(2).forEachIndexed { idx, cat ->
                                    val actualIndex = idx + 2
                                    val isSelected = selectedCategoryIndex == actualIndex
                                    Surface(
                                        onClick = { selectedCategoryIndex = actualIndex },
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = if (isSelected) navyColor else ext.surfaceSubtle,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            color = if (isSelected) colors.onPrimary else ext.textSecondary,
                                            modifier = Modifier.padding(vertical = UthDimens.Spacing.Sm, horizontal = UthDimens.Spacing.Xs),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }

                        // Đơn vị phối hợp & đồng tổ chức
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                            Text(
                                text = "Đơn vị phối hợp & đồng tổ chức",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = ext.surfaceSubtle,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(UthDimens.Spacing.Sm),
                                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    coOrganizers.forEach { org ->
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = colors.surface,
                                            shadowElevation = UthDimens.Elevation.Card,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                                            ) {
                                                Text(
                                                    text = org,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                    color = navyColor,
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Xóa $org",
                                                    modifier = Modifier
                                                        .size(UthDimens.Size.IconInline)
                                                        .clickable { coOrganizers.remove(org) },
                                                    tint = ext.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        onClick = { showAddOrganizerDialog = true },
                                        shape = MaterialTheme.shapes.small,
                                        color = colors.surface.copy(alpha = 0.8f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(UthDimens.Size.IconInline),
                                                tint = ext.textSecondary,
                                            )
                                            Text(
                                                text = "Thêm đơn vị",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Nội dung tóm tắt & Lịch trình
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Nội dung tóm tắt & Lịch trình",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = "${summaryDescription.length}/500 từ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary,
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = ext.surfaceSubtle,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(UthDimens.Spacing.Sm)) {
                                    // Formatting mini toolbar
                                    Row(
                                        modifier = Modifier.padding(bottom = UthDimens.Spacing.Xs),
                                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        FormatButton("B", bold = true)
                                        FormatButton("I", italic = true)
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = colors.surface,
                                            modifier = Modifier.size(UthDimens.Size.Badge),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.List,
                                                    contentDescription = "Danh sách",
                                                    modifier = Modifier.size(UthDimens.Size.IconInline),
                                                    tint = ext.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = summaryDescription,
                                        onValueChange = { summaryDescription = it },
                                        placeholder = {
                                            Text(
                                                text = "Mô tả mục tiêu buổi hội thảo, diễn giả khách mời, tài liệu cần chuẩn bị...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ext.textTertiary,
                                            )
                                        },
                                        singleLine = false,
                                        minLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Time & Venue Module
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surface,
                    border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = UthDimens.Elevation.Card,
                ) {
                    Column(
                        modifier = Modifier.padding(UthDimens.Spacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                    ) {
                        // Date & Time pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                        ) {
                            // Ngày tổ chức
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
                            ) {
                                Text(
                                    text = "Ngày tổ chức",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary,
                                )
                                Surface(
                                    onClick = { showDatePicker = true },
                                    shape = MaterialTheme.shapes.medium,
                                    color = ext.surfaceSubtle,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = navyColor,
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = "Chọn ngày",
                                            tint = navyColor,
                                            modifier = Modifier.size(UthDimens.Size.IconInline),
                                        )
                                    }
                                }
                            }

                            // Khung giờ
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
                            ) {
                                Text(
                                    text = "Khung giờ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary,
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = ext.surfaceSubtle,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = selectedTimeSlot,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = navyColor,
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = null,
                                            tint = navyColor,
                                            modifier = Modifier.size(UthDimens.Size.IconInline),
                                        )
                                    }
                                }
                            }
                        }

                        // Lựa chọn hội trường đăng ký
                        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                            Text(
                                text = "Lựa chọn hội trường đăng ký",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            venues.forEachIndexed { index, venue ->
                                val isSelected = selectedVenueIndex == index
                                Surface(
                                    onClick = { selectedVenueIndex = index },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) colors.primaryContainer.copy(alpha = 0.5f) else ext.surfaceSubtle,
                                    border = if (isSelected) BorderStroke(UthDimens.Size.Stroke, navyColor.copy(alpha = 0.3f)) else null,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(UthDimens.Spacing.Md),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedVenueIndex = index },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = navyColor,
                                                unselectedColor = colors.outline,
                                            ),
                                        )
                                        Spacer(Modifier.width(UthDimens.Spacing.Xs))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = venue.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) navyColor else colors.onSurface,
                                            )
                                            Text(
                                                text = venue.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ext.textSecondary,
                                            )
                                        }
                                        if (!isSelected && venue.status.isNotBlank()) {
                                            Text(
                                                text = venue.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Điểm rèn luyện cộng thưởng
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surface,
                    border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = UthDimens.Elevation.Card,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(UthDimens.Spacing.Lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Điểm rèn luyện cộng thưởng",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            )
                            Spacer(Modifier.height(UthDimens.Spacing.Xxs))
                            Text(
                                text = "Áp dụng Điều 3: Hoạt động học thuật/phong trào",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.textSecondary,
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ext.accentContainer,
                        ) {
                            Text(
                                text = "+5 ĐRL",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = navyColor,
                                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                            )
                        }
                    }
                }

                // Section 4: Ban quản lý
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surface,
                    border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = UthDimens.Elevation.Card,
                ) {
                    Column(
                        modifier = Modifier.padding(UthDimens.Spacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = navyColor,
                                    modifier = Modifier.size(UthDimens.Size.IconInline),
                                )
                                Text(
                                    text = "Ban quản lý",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = navyColor,
                                )
                            }
                            Text(
                                text = "+ Thêm người",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = navyColor,
                                modifier = Modifier.clickable { showAddManagerDialog = true },
                            )
                        }

                        managers.forEach { mgr ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = ext.surfaceSubtle,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(UthDimens.Size.HeaderAction)
                                            .clip(CircleShape)
                                            .background(
                                                if (mgr.isFaculty) colors.primaryContainer else ext.accentContainer,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = mgr.initials,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mgr.isFaculty) colors.onPrimaryContainer else ext.onAccentContainer,
                                        )
                                    }
                                    Text(
                                        text = mgr.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar "TẠO"
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = colors.surface.copy(alpha = 0.96f),
            shadowElevation = UthDimens.Elevation.Card,
            border = BorderStroke(UthDimens.Size.Stroke, colors.outlineVariant.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
            ) {
                Button(
                    onClick = {
                        val times = selectedTimeSlot.split("-").map { it.trim() }
                        val startTime = times.getOrNull(0) ?: "08:30"
                        val endTime = times.getOrNull(1) ?: "11:30"
                        val selectedVenue = venues.getOrNull(selectedVenueIndex)?.name ?: "Hội trường A.101"

                        onAction(
                            ScheduleAction.CreateWorkshopEvent(
                                title = eventTitle,
                                category = categories.getOrNull(selectedCategoryIndex) ?: "Hội thảo / Workshop",
                                coOrganizers = coOrganizers.toList(),
                                summary = summaryDescription,
                                date = selectedDate,
                                startTime = startTime,
                                endTime = endTime,
                                venue = selectedVenue,
                                trainingPoints = "+5 ĐRL",
                                managers = managers.map { it.name },
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UthDimens.Size.ButtonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "TẠO",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary,
                        )
                        Spacer(Modifier.width(UthDimens.Spacing.Sm))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                            tint = colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatButton(text: String, bold: Boolean = false, italic: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(UthDimens.Size.Badge),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class VenueOption(
    val name: String,
    val location: String,
    val status: String,
)

private data class ManagerOption(
    val name: String,
    val initials: String,
    val isFaculty: Boolean,
)
