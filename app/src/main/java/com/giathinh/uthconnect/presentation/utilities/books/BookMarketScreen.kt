package com.giathinh.uthconnect.presentation.utilities.books

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
import androidx.compose.material.icons.filled.ShoppingCart
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

data class BookItem(
    val id: String,
    val title: String,
    val subject: String,
    val price: String,
    val condition: String,
    val campus: String,
    val sellerName: String,
    val sellerPhone: String,
    val isFree: Boolean = false,
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
fun BookMarketScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onCallContact: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var books by remember { mutableStateOf(emptyList<BookItem>()) }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var itemToDelete by remember { mutableStateOf<BookItem?>(null) }
    var isPostDialogOpen by remember { mutableStateOf(false) }

    val filters = listOf("Tất cả", "Khoa CNTT", "Đại cương", "Tài liệu miễn phí")

    val filteredBooks = books.filter {
        when (selectedFilter) {
            "Tất cả" -> true
            "Khoa CNTT" -> it.subject.contains("CNTT", ignoreCase = true)
            "Đại cương" -> it.subject.contains("Đại cương", ignoreCase = true)
            "Tài liệu miễn phí" -> it.isFree
            else -> true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chợ Giáo trình UTH", fontWeight = FontWeight.Bold) },
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
                text = { Text("Đăng bán sách") },
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
            // Bộ lọc Chip
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

            if (filteredBooks.isEmpty()) {
                UthEmptyState(
                    title = "Chưa có giáo trình nào",
                    body = "Hãy bấm nút '+ Đăng bán sách' bên dưới để thanh lý sách cũ hoặc tặng tài liệu học tập cho các bạn sinh viên khác.",
                    icon = Icons.Filled.ShoppingCart,
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
                    items(filteredBooks, key = { it.id }) { book ->
                        BookItemCard(
                            book = book,
                            onCall = {
                                onCallContact(book.sellerPhone)
                                dialPhoneNumber(context, book.sellerPhone)
                            },
                            onDelete = { itemToDelete = book },
                        )
                    }
                }
            }
        }
    }

    // Dialog xác nhận xóa
    itemToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Xác nhận xóa bài") },
            text = { Text("Bạn có chắc chắn muốn xóa bài đăng \"${book.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        books = books.filterNot { it.id == book.id }
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

    // Dialog đăng bán sách
    if (isPostDialogOpen) {
        PostBookDialog(
            onDismiss = { isPostDialogOpen = false },
            onSubmit = { newBook ->
                books = listOf(newBook) + books
                isPostDialogOpen = false
            },
        )
    }
}

@Composable
private fun BookItemCard(
    book: BookItem,
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
                color = if (book.isFree) UthTheme.extendedColors.successContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = book.price,
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (book.isFree) UthTheme.extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer,
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
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = "${book.subject} • ${book.condition}",
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Text(
            text = "Khu vực: ${book.campus}",
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )

        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Người bán: ${book.sellerName}",
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
                Text("Liên hệ", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PostBookDialog(
    onDismiss: () -> Unit,
    onSubmit: (BookItem) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var sellerName by remember { mutableStateOf("Nguyễn Minh Anh") }
    var sellerPhone by remember { mutableStateOf("0909123456") }
    var campus by remember { mutableStateOf("Cơ sở 1 (Bình Thạnh)") }

    var titleError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng bán giáo trình") },
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
                    label = { Text("Tên sách / giáo trình *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Vui lòng nhập tên giáo trình", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Môn học / Chuyên ngành") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Giá bán (nhập '0' nếu tặng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = campus,
                    onValueChange = { campus = it },
                    label = { Text("Cơ sở trường (CS1, CS2, CS3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sellerPhone,
                    onValueChange = {
                        sellerPhone = it
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
                    if (sellerPhone.isBlank()) {
                        phoneError = true
                        return@Button
                    }
                    val isFree = price.trim() == "0" || price.lowercase().contains("miễn phí")
                    val finalPrice = if (isFree) "0 VNĐ (Tặng miễn phí)" else if (price.isNotBlank()) "${price.trim()} VNĐ" else "Thỏa thuận"
                    val newBook = BookItem(
                        id = "book_${System.currentTimeMillis()}",
                        title = title.trim(),
                        subject = subject.ifBlank { "Giáo trình UTH" },
                        price = finalPrice,
                        condition = "Đã qua sử dụng",
                        campus = campus.ifBlank { "Cơ sở 1 UTH" },
                        sellerName = sellerName.ifBlank { "Sinh viên UTH" },
                        sellerPhone = sellerPhone.trim(),
                        isFree = isFree,
                    )
                    onSubmit(newBook)
                },
                enabled = title.isNotBlank() && sellerPhone.isNotBlank(),
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
