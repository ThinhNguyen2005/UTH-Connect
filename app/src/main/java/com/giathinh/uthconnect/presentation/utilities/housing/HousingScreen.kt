package com.giathinh.uthconnect.presentation.utilities.housing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
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

data class HousingPost(
    val id: String,
    val title: String,
    val campusArea: String,
    val rentPrice: String,
    val address: String,
    val amenities: List<String>,
    val posterName: String,
    val contactPhone: String,
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
fun HousingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onCallContact: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var housingList by remember { mutableStateOf(emptyList<HousingPost>()) }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var itemToDelete by remember { mutableStateOf<HousingPost?>(null) }
    var isPostDialogOpen by remember { mutableStateOf(false) }

    val filters = listOf("Tất cả", "Gần CS1 (Bình Thạnh)", "Gần CS2 (Thủ Đức)", "Gần CS3 (Quận 12)")

    val filteredList = housingList.filter {
        if (selectedFilter == "Tất cả") true else it.campusArea == selectedFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ghép Phòng Trọ Sinh Viên", fontWeight = FontWeight.Bold) },
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
                text = { Text("Đăng tin tìm trọ") },
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
            // Bộ lọc cơ sở
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
                    title = "Chưa có tin tìm bạn ở ghép",
                    body = "Hãy bấm nút '+ Đăng tin tìm trọ' bên dưới để kết nối với các bạn sinh viên UTH đang có phòng trống.",
                    icon = Icons.Filled.Home,
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
                        HousingItemCard(
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
            title = { Text("Xác nhận xóa tin") },
            text = { Text("Bạn có chắc chắn muốn xóa tin trọ \"${post.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        housingList = housingList.filterNot { it.id == post.id }
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

    // Dialog đăng tin ghép trọ
    if (isPostDialogOpen) {
        PostHousingDialog(
            onDismiss = { isPostDialogOpen = false },
            onSubmit = { newPost ->
                housingList = listOf(newPost) + housingList
                isPostDialogOpen = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HousingItemCard(
    post: HousingPost,
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
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = post.rentPrice,
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Xóa tin",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = "${post.address} • ${post.campusArea}",
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }

        if (post.amenities.isNotEmpty()) {
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                post.amenities.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Liên hệ: ${post.posterName}",
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
                Text("Gọi điện", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PostHousingDialog(
    onDismiss: () -> Unit,
    onSubmit: (HousingPost) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var campusArea by remember { mutableStateOf("Gần CS1 (Bình Thạnh)") }
    var rentPrice by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var amenities by remember { mutableStateOf("Máy lạnh, Giờ tự do, Wifi") }
    var posterName by remember { mutableStateOf("Sinh viên UTH") }
    var contactPhone by remember { mutableStateOf("0909123456") }

    var titleError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng tin tìm bạn ở ghép") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = it.isBlank()
                    },
                    label = { Text("Tiêu đề tin trọ *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Vui lòng nhập tiêu đề", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = campusArea,
                    onValueChange = { campusArea = it },
                    label = { Text("Khu vực (Gần CS1, CS2 hoặc CS3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rentPrice,
                    onValueChange = { rentPrice = it },
                    label = { Text("Giá phòng (vd: 1.500.000 đ/tháng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ chi tiết") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amenities,
                    onValueChange = { amenities = it },
                    label = { Text("Tiện ích (cách nhau dấu phẩy)") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    if (contactPhone.isBlank()) {
                        phoneError = true
                        return@Button
                    }
                    val amenitiesList = amenities.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val finalPrice = if (rentPrice.isNotBlank()) {
                        if (rentPrice.contains("đ") || rentPrice.contains("VNĐ")) rentPrice.trim() else "${rentPrice.trim()} đ/tháng"
                    } else "Thỏa thuận"

                    val newPost = HousingPost(
                        id = "housing_${System.currentTimeMillis()}",
                        title = title.trim(),
                        campusArea = campusArea.ifBlank { "Gần CS1 (Bình Thạnh)" },
                        rentPrice = finalPrice,
                        address = address.ifBlank { "Gần khuôn viên UTH" },
                        amenities = if (amenitiesList.isEmpty()) listOf("An ninh", "Giờ tự do") else amenitiesList,
                        posterName = posterName.ifBlank { "Sinh viên UTH" },
                        contactPhone = contactPhone.trim(),
                    )
                    onSubmit(newPost)
                },
                enabled = title.isNotBlank() && contactPhone.isNotBlank(),
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
