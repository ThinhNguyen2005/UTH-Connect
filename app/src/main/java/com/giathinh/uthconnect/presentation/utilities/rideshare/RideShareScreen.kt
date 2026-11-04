package com.giathinh.uthconnect.presentation.utilities.rideshare

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

data class RideSharePost(
    val id: String,
    val route: String,
    val routeCategory: String,
    val departureTime: String,
    val vehicle: String,
    val costShare: String,
    val availableSeats: String,
    val driverName: String,
    val contactPhone: String,
    val note: String,
)

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
fun RideShareScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onCallContact: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var ridesList by remember { mutableStateOf(emptyList<RideSharePost>()) }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var itemToDelete by remember { mutableStateOf<RideSharePost?>(null) }
    var isPostDialogOpen by remember { mutableStateOf(false) }

    val filters = listOf("Tất cả", "CS1 ↔ CS3", "CS1 ↔ CS2", "Về quê cuối tuần")

    val filteredList = ridesList.filter {
        if (selectedFilter == "Tất cả") true else it.routeCategory == selectedFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ghép Xe Đi Chung UTH", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isPostDialogOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Đăng chuyến đi") },
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
            // Bộ lọc tuyến
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            if (filteredList.isEmpty()) {
                UthEmptyState(
                    title = "Chưa có chuyến đi chung nào",
                    body = "Hãy bấm nút '+ Đăng chuyến đi' bên dưới để chia sẻ chỗ trống hoặc tìm bạn đi cùng tuyến đường.",
                    icon = Icons.Filled.Share,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = UthDimens.Spacing.Lg,
                        end = UthDimens.Spacing.Lg,
                        bottom = 80.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                ) {
                    items(filteredList, key = { it.id }) { post ->
                        RideItemCard(
                            post = post,
                            onCall = {
                                onCallContact(post.contactPhone)
                                dialPhoneNumber(context, post.contactPhone)
                            },
                            onDelete = { itemToDelete = post },
                        )
                    }
                }
            }
        }
    }

    // Dialog xác nhận xóa
    itemToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Xác nhận xóa chuyến đi") },
            text = { Text("Bạn có chắc chắn muốn xóa chuyến đi \"${post.route}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ridesList = ridesList.filterNot { it.id == post.id }
                        itemToDelete = null
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Hủy")
                }
            },
        )
    }

    // Dialog đăng chuyến đi
    if (isPostDialogOpen) {
        PostRideDialog(
            onDismiss = { isPostDialogOpen = false },
            onSubmit = { newPost ->
                ridesList = listOf(newPost) + ridesList
                isPostDialogOpen = false
            },
        )
    }
}

@Composable
private fun RideItemCard(
    post: RideSharePost,
    onCall: () -> Unit,
    onDelete: () -> Unit,
) {
    UthCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (post.costShare.contains("Miễn phí")) UthTheme.extendedColors.successContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = post.costShare,
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (post.costShare.contains("Miễn phí")) UthTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Xóa bài",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = post.route,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = "${post.departureTime} • ${post.availableSeats}",
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = "Phương tiện: ${post.vehicle}",
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )

        if (post.note.isNotBlank()) {
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Text(
                text = "Ghi chú: ${post.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tài xế: ${post.driverName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(UthDimens.Spacing.Xs))
                Text("Gọi tài xế", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PostRideDialog(
    onDismiss: () -> Unit,
    onSubmit: (RideSharePost) -> Unit,
) {
    var route by remember { mutableStateOf("") }
    var routeCategory by remember { mutableStateOf("CS1 ↔ CS3") }
    var departureTime by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("Xe máy") }
    var costShare by remember { mutableStateOf("15.000 đ (Share xăng)") }
    var availableSeats by remember { mutableStateOf("Còn 1 chỗ") }
    var driverName by remember { mutableStateOf("Sinh viên UTH") }
    var contactPhone by remember { mutableStateOf("0909123456") }
    var note by remember { mutableStateOf("") }

    var routeError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng chuyến đi chung") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                OutlinedTextField(
                    value = route,
                    onValueChange = {
                        route = it
                        routeError = it.isBlank()
                    },
                    label = { Text("Tuyến đường di chuyển *") },
                    isError = routeError,
                    supportingText = if (routeError) {
                        { Text("Vui lòng nhập tuyến đường", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = routeCategory,
                    onValueChange = { routeCategory = it },
                    label = { Text("Phân loại (CS1 ↔ CS3, CS1 ↔ CS2, Về quê...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = departureTime,
                    onValueChange = { departureTime = it },
                    label = { Text("Thời gian xuất phát (vd: 06:45 sáng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Phương tiện (Xe máy Honda, Ô tô...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = costShare,
                    onValueChange = { costShare = it },
                    label = { Text("Chi phí share (hoặc nhập 'Miễn phí')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = availableSeats,
                    onValueChange = { availableSeats = it },
                    label = { Text("Số chỗ trống (vd: Còn 1 chỗ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = {
                        contactPhone = it
                        phoneError = it.isBlank()
                    },
                    label = { Text("Số điện thoại liên hệ *") },
                    isError = phoneError,
                    supportingText = if (phoneError) {
                        { Text("Vui lòng nhập số điện thoại", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú thêm (điểm đón, mũ bảo hiểm...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (route.isBlank()) {
                        routeError = true
                        return@Button
                    }
                    if (contactPhone.isBlank()) {
                        phoneError = true
                        return@Button
                    }
                    val newPost = RideSharePost(
                        id = "ride_${System.currentTimeMillis()}",
                        route = route.trim(),
                        routeCategory = routeCategory.ifBlank { "CS1 ↔ CS3" },
                        departureTime = departureTime.ifBlank { "Linh hoạt" },
                        vehicle = vehicle.ifBlank { "Xe máy" },
                        costShare = costShare.ifBlank { "Miễn phí" },
                        availableSeats = availableSeats.ifBlank { "Còn 1 chỗ" },
                        driverName = driverName.ifBlank { "Sinh viên UTH" },
                        contactPhone = contactPhone.trim(),
                        note = note.trim(),
                    )
                    onSubmit(newPost)
                },
                enabled = route.isNotBlank() && contactPhone.isNotBlank(),
            ) {
                Text("Đăng ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}
