package com.giathinh.uthconnect.presentation.tickets

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus

/**
 * Tab Vé: thanh nhận diện rút gọn kiểu Trang chủ, bộ lọc hai nhóm và danh sách vé dạng
 * boarding pass (dải nhận diện · nội dung · đường xé · cuống vé có QR mini).
 *
 * Screen **stateless**: chỉ render [TicketsUiState] và phát [TicketsAction] — cùng ràng buộc
 * tầng `presentation` như các module khác.
 */
@Composable
fun TicketsScreen(
    uiState: TicketsUiState,
    onAction: (TicketsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TicketsTopBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Lg,
                bottom = UthDimens.Spacing.Xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            val hasTickets = !uiState.isLoading && uiState.failure == null && uiState.tickets.isNotEmpty()
            val ticketCount = if (hasTickets) uiState.visibleTickets.size else null

            item(key = TicketsKey.Intro) {
                TicketsIntro(ticketCount = ticketCount)
            }

            if (hasTickets) {
                item(key = TicketsKey.Filters) {
                    TicketsFilterTabs(
                        selected = uiState.filter,
                        upcomingCount = uiState.upcomingTickets.size,
                        pastCount = uiState.pastTickets.size,
                        onSelect = { onAction(TicketsAction.SelectFilter(it)) },
                    )
                }
            }

            when {
                uiState.isLoading -> item(key = TicketsKey.Loading) { TicketsLoading() }

                uiState.failure != null -> item(key = TicketsKey.Error) {
                    UthEmptyState(
                        title = stringResource(R.string.tickets_error_title),
                        body = stringResource(R.string.tickets_error_body),
                        icon = Icons.Filled.Warning,
                        actionLabel = stringResource(R.string.common_retry),
                        onAction = { onAction(TicketsAction.Retry) },
                        modifier = Modifier.padding(top = UthDimens.Spacing.Xl),
                    )
                }

                uiState.tickets.isEmpty() -> item(key = TicketsKey.Empty) {
                    UthEmptyState(
                        title = stringResource(R.string.tickets_empty_title),
                        body = stringResource(R.string.tickets_empty_body),
                        icon = Icons.Filled.DateRange,
                        modifier = Modifier.padding(top = UthDimens.Spacing.Xl),
                    )
                }

                uiState.visibleTickets.isEmpty() -> item(key = TicketsKey.FilterEmpty) {
                    // Tab rỗng khác trạng thái rỗng toàn màn: người dùng còn vé ở tab kia.
                    val past = uiState.filter == TicketFilter.Past
                    UthEmptyState(
                        title = stringResource(
                            if (past) R.string.tickets_past_empty_title else R.string.tickets_upcoming_empty_title,
                        ),
                        body = stringResource(
                            if (past) R.string.tickets_past_empty_body else R.string.tickets_upcoming_empty_body,
                        ),
                        icon = Icons.Filled.DateRange,
                        modifier = Modifier.padding(top = UthDimens.Spacing.Xl),
                    )
                }

                else -> items(uiState.visibleTickets, key = Ticket::id) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        onOpenTicket = { onAction(TicketsAction.OpenTicket(ticket.id)) },
                    )
                }
            }
        }
    }
}

/**
 * Bản rút gọn của thanh nhận diện Trang chủ: giữ dải nền `primary`, ô logo chữ tắt trường và
 * tên tab; bỏ lời chào và chuông thông báo vì tab Vé không có luồng thông báo riêng.
 *
 * Hai vòng tròn trang trí vẽ bằng [drawBehind] ở alpha thấp, không mang thông tin nên không
 * ảnh hưởng TalkBack.
 */
@Composable
private fun TicketsTopBar(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primary)
            .drawBehind {
                val radius = size.width * TopBarRingRadiusFraction
                drawCircle(
                    color = colors.onPrimary.copy(alpha = TopBarRingAlpha),
                    radius = radius,
                    center = Offset(size.width * TopBarRingCenterFraction, 0f),
                    style = Stroke(width = size.width * TopBarRingStrokeFraction),
                )
            }
            .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .clip(MaterialTheme.shapes.small)
                .background(colors.onPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tickets_logo_abbr),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tickets_brand).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.tickets_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPrimary,
            )
        }
    }
}

/** Dòng mô tả ngắn + số vé của tab đang xem; không lặp lại tiêu đề đã có trên top bar. */
@Composable
private fun TicketsIntro(ticketCount: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tickets_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (ticketCount != null && ticketCount > 0) {
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Text(
                text = pluralStringResource(R.plurals.tickets_count, ticketCount, ticketCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
            )
        }
    }
}

/** Segmented control hai nhóm vé; nhóm suy ra từ trạng thái vé nên không cần nút "Lịch sử" riêng. */
@Composable
private fun TicketsFilterTabs(
    selected: TicketFilter,
    upcomingCount: Int,
    pastCount: Int,
    onSelect: (TicketFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(UthDimens.Spacing.Xs)
            .selectableGroup(),
    ) {
        TicketsFilterTab(
            label = stringResource(R.string.tickets_tab_upcoming),
            icon = Icons.AutoMirrored.Filled.List,
            count = upcomingCount,
            selected = selected == TicketFilter.Upcoming,
            onClick = { onSelect(TicketFilter.Upcoming) },
            modifier = Modifier.weight(1f),
        )
        TicketsFilterTab(
            label = stringResource(R.string.tickets_tab_past),
            icon = Icons.Filled.DateRange,
            count = pastCount,
            selected = selected == TicketFilter.Past,
            onClick = { onSelect(TicketFilter.Past) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TicketsFilterTab(
    label: String,
    icon: ImageVector,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant

    Row(
        modifier = modifier
            .height(UthDimens.Size.MinTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colors.primary else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = UthDimens.Spacing.Sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(UthDimens.Size.IconInline),
        )
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (selected) colors.onPrimary.copy(alpha = SelectedCountAlpha) else colors.surface)
                .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs),
        )
    }
}

/** Indicator tải danh sách: không phủ overlay, chỉ đặt giữa vùng nội dung. */
@Composable
private fun TicketsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = UthDimens.Spacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = UthDimens.Size.ProgressStroke,
            modifier = Modifier.size(UthDimens.Size.Progress),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Text(
            text = stringResource(R.string.tickets_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Thẻ vé dạng boarding pass: dải màu trạng thái bên trái, khối nội dung ở giữa, đường xé nét
 * đứt và cuống vé bên phải. Toàn bộ cuống vé là một vùng chạm duy nhất (≥ [UthDimens.Size.MinTouchTarget])
 * nên không cần thêm nút phụ trong khối nội dung.
 */
@Composable
private fun TicketCard(
    ticket: Ticket,
    onOpenTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth(), contentPadding = false) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(UthDimens.Size.TicketStripe)
                    .fillMaxHeight()
                    .background(ticketStripeColor(ticket.status)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(UthDimens.Spacing.Md),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                TicketHeadline(ticket)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = UthDimens.Spacing.Xs),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                TicketMetaRow(
                    icon = Icons.Filled.DateRange,
                    text = ticket.eventStartsAt,
                    emphasized = ticket.status == TicketStatus.VALID,
                )
                TicketMetaRow(
                    icon = Icons.Filled.LocationOn,
                    text = ticket.eventLocation,
                    emphasized = false,
                )
                TicketFooter(ticket)
            }
            TicketPerforation(modifier = Modifier.fillMaxHeight())
            TicketStub(ticket = ticket, onOpenTicket = onOpenTicket, modifier = Modifier.fillMaxHeight())
        }
    }
}

/** Badge trạng thái, tên sự kiện và đơn vị tổ chức — một badge nhấn mỗi thẻ. */
@Composable
private fun TicketHeadline(ticket: Ticket, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        UthStatusBadge(text = ticketStatusLabel(ticket.status), tone = ticketStatusTone(ticket.status))
        Text(
            text = ticket.eventTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ticket.organizerName?.let { organizer ->
            Text(
                text = organizer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Một dòng thông tin sự kiện: icon + chữ, tối đa một dòng để giữ nhịp thẻ vé. */
@Composable
private fun TicketMetaRow(
    icon: ImageVector,
    text: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(UthDimens.Size.IconInline),
        )
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Text(
            text = text,
            style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = if (emphasized) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mã vé (monospace) bên trái, chỗ ngồi bên phải khi sự kiện có chia chỗ. */
@Composable
private fun TicketFooter(ticket: Ticket, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val active = ticket.status == TicketStatus.VALID

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UthDimens.Spacing.Xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tickets_card_code, ticket.fallbackCode),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
            textDecoration = if (active) null else TextDecoration.LineThrough,
            maxLines = 1,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (active) colors.primaryContainer else colors.surfaceVariant)
                .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
        )
        ticket.seatLabel?.let { seat ->
            Text(
                text = seat,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(colors.surfaceVariant)
                    .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
            )
        }
    }
}

/** Đường xé dọc nét đứt — dấu hiệu "vé" duy nhất, không cần notch để tránh cắt nền phức tạp. */
@Composable
private fun TicketPerforation(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(
        modifier = modifier
            .width(UthDimens.Spacing.Sm)
            .padding(vertical = UthDimens.Spacing.Md),
    ) {
        val x = size.width / 2f
        drawLine(
            color = colors.outlineVariant,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = UthDimens.Size.Stroke.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(size.width * DashedLineDashFactor, size.width * DashedLineGapFactor),
            ),
        )
    }
}

/** Cuống vé: nhãn vào cổng, QR mini mô phỏng và nhãn "Mở vé"; cả cột là một nút. */
@Composable
private fun TicketStub(
    ticket: Ticket,
    onOpenTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(UthDimens.Size.TicketStub)
            .background(colors.surfaceVariant.copy(alpha = StubFillAlpha))
            .clickable(role = Role.Button, onClick = onOpenTicket)
            .padding(UthDimens.Spacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.tickets_stub_label),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        TicketQrPreview(seed = ticket.id.hashCode(), modifier = Modifier.size(UthDimens.Size.TicketStubQr))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(colors.primary)
                .padding(vertical = UthDimens.Spacing.Xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tickets_stub_open),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                maxLines = 1,
            )
        }
    }
}

/**
 * QR mini mô phỏng (chưa có token thật): 21×21 module sinh tất định từ id vé cộng ba ô định vị
 * góc — chỉ là hình in trên cuống vé nên không có `contentDescription`.
 */
@Composable
private fun TicketQrPreview(seed: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(colors.surface)
            .border(UthDimens.Size.Stroke, colors.outlineVariant, MaterialTheme.shapes.extraSmall)
            .padding(UthDimens.Spacing.Xxs),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = size.minDimension / QrModuleCount
            drawQrModules(seed = seed, cell = cell, ink = colors.onSurface, paper = colors.surface)
        }
    }
}

private fun DrawScope.drawQrModules(seed: Int, cell: Float, ink: Color, paper: Color) {
    var state = (seed.toLong() and QrSeedMask) or 1L
    for (row in 0 until QrModuleCount) {
        for (column in 0 until QrModuleCount) {
            if (isInsideFinderModule(column, row)) continue
            state = (state * QrSeedMultiplier + QrSeedIncrement) and QrSeedMask
            if ((state ushr QrSeedShift) and 1L == 1L) {
                drawRect(ink, Offset(column * cell, row * cell), Size(cell, cell))
            }
        }
    }
    val far = QrModuleCount - QrFinderModules
    listOf(0 to 0, far to 0, 0 to far).forEach { (column, row) ->
        drawFinderModule(column, row, cell, ink, paper)
    }
}

private fun isInsideFinderModule(column: Int, row: Int): Boolean {
    val far = QrModuleCount - QrFinderModules
    val nearLeft = column < QrFinderModules
    val nearTop = row < QrFinderModules
    val nearRight = column >= far
    val nearBottom = row >= far
    return (nearLeft && nearTop) || (nearRight && nearTop) || (nearLeft && nearBottom)
}

/** Ba lớp vuông lồng nhau của ô định vị QR. */
private fun DrawScope.drawFinderModule(column: Int, row: Int, cell: Float, ink: Color, paper: Color) {
    drawRect(ink, Offset(column * cell, row * cell), Size(cell * QrFinderModules, cell * QrFinderModules))
    drawRect(
        paper,
        Offset((column + 1) * cell, (row + 1) * cell),
        Size(cell * (QrFinderModules - 2), cell * (QrFinderModules - 2)),
    )
    drawRect(
        ink,
        Offset((column + 2) * cell, (row + 2) * cell),
        Size(cell * (QrFinderModules - 4), cell * (QrFinderModules - 4)),
    )
}

/** Màu dải nhận diện: Navy khi vé còn dùng được, xám khi đã qua, đỏ khi bị hủy. */
@Composable
private fun ticketStripeColor(status: TicketStatus): Color = when (status) {
    TicketStatus.VALID -> MaterialTheme.colorScheme.primary
    TicketStatus.CHECKED_IN -> MaterialTheme.colorScheme.outlineVariant
    TicketStatus.CANCELLED -> MaterialTheme.colorScheme.error
}

@Composable
private fun ticketStatusLabel(status: TicketStatus): String = when (status) {
    TicketStatus.VALID -> stringResource(R.string.tickets_status_valid)
    TicketStatus.CHECKED_IN -> stringResource(R.string.tickets_status_checked_in)
    TicketStatus.CANCELLED -> stringResource(R.string.tickets_status_cancelled)
}

private fun ticketStatusTone(status: TicketStatus): BadgeTone = when (status) {
    TicketStatus.VALID -> BadgeTone.Success
    TicketStatus.CHECKED_IN -> BadgeTone.Info
    TicketStatus.CANCELLED -> BadgeTone.Danger
}

/** Khoá item của danh sách vé — một chỗ duy nhất thay vì chuỗi rải trong Composable. */
private object TicketsKey {
    const val Intro = "tickets-intro"
    const val Filters = "tickets-filters"
    const val Loading = "tickets-loading"
    const val Error = "tickets-error"
    const val Empty = "tickets-empty"
    const val FilterEmpty = "tickets-filter-empty"
}

private const val TopBarRingRadiusFraction = 0.62f
private const val TopBarRingStrokeFraction = 0.008f
private const val TopBarRingCenterFraction = 0.95f
private const val TopBarRingAlpha = 0.14f
private const val SelectedCountAlpha = 0.24f
private const val StubFillAlpha = 0.5f
private const val DashedLineDashFactor = 1f
private const val DashedLineGapFactor = 1.2f
private const val QrModuleCount = 21
private const val QrFinderModules = 7
private const val QrSeedMask = 0x7FFFFFFFL
private const val QrSeedMultiplier = 1103515245L
private const val QrSeedIncrement = 12345L
private const val QrSeedShift = 16

/**
 * Màn chi tiết vé dạng boarding pass: nửa trên là thông tin sự kiện + sinh viên, đường xé có
 * khuyết tròn, nửa dưới là khung QR lớn kèm mã vé sao chép được.
 *
 * Screen **stateless**: chỉ render [Ticket] đang mở trong state, không tự gọi repository. QR vẫn
 * là hình mô phỏng tất định từ id vé — chưa có token thật từ backend.
 */
@Composable
fun TicketQrScreen(
    ticket: Ticket?,
    studentName: String?,
    secondsRemaining: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TicketDetailTopBar(onBack = onBack)

        if (ticket == null) {
            TicketMissing(onBack = onBack)
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Lg),
        ) {
            TicketPassCard(
                ticket = ticket,
                studentName = studentName,
                secondsRemaining = secondsRemaining,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Xl))
        }
    }
}

/** Top bar tối giản: chỉ nút quay lại và tiêu đề — màn chi tiết không có hành động phụ nào. */
@Composable
private fun TicketDetailTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UthDimens.Size.TopBar)
                .padding(horizontal = UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.tickets_qr_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

/** Vé không còn trong state (mở lại route sau khi state bị dọn) — vẫn phải có lối thoát. */
@Composable
private fun TicketMissing(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UthDimens.Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.tickets_qr_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = UthTheme.extendedColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        UthPrimaryButton(text = stringResource(R.string.common_back), onClick = onBack)
    }
}

/**
 * Thẻ vé hoàn chỉnh: khối thông tin ở trên, đường xé ở giữa, khối QR ở dưới — cùng một `UthCard`
 * nên hai nửa không thể lệch viền.
 */
@Composable
private fun TicketPassCard(
    ticket: Ticket,
    studentName: String?,
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val copyLabel = stringResource(R.string.tickets_detail_copy)
    val copiedLabel = stringResource(R.string.tickets_detail_copied, ticket.fallbackCode)
    val onCopy: () -> Unit = {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(copyLabel, ticket.fallbackCode))
        // Android 13+ tự hiện xác nhận sao chép; máy cũ cần toast để vẫn có phản hồi.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, copiedLabel, Toast.LENGTH_SHORT).show()
        }
    }

    UthCard(modifier = modifier.fillMaxWidth(), contentPadding = false) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(UthDimens.Spacing.Xl),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
            ) {
                TicketDetailHeadline(ticket)
                TicketDetailGrid(ticket = ticket, studentName = studentName)
            }
            TicketNotchDivider()
            Column(
                modifier = Modifier.padding(
                    start = UthDimens.Spacing.Xl,
                    end = UthDimens.Spacing.Xl,
                    top = UthDimens.Spacing.Xs,
                    bottom = UthDimens.Spacing.Xl,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                TicketSecurityRibbon(secondsRemaining = secondsRemaining)
                TicketQrPanel(seed = ticket.id.hashCode())
                TicketCodeRow(code = ticket.fallbackCode, copyLabel = copyLabel, onCopy = onCopy)
                Text(
                    text = stringResource(R.string.tickets_qr_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Badge trạng thái, tên sự kiện và đơn vị tổ chức (dấu tick = đơn vị đã xác thực). */
@Composable
private fun TicketDetailHeadline(ticket: Ticket, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        UthStatusBadge(text = ticketStatusLabel(ticket.status), tone = ticketStatusTone(ticket.status))
        Text(
            text = ticket.eventTitle,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        ticket.organizerName?.let { organizer ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
                Text(
                    text = organizer,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Lưới thông tin 2 cột: thời gian · chỗ ngồi, rồi địa điểm và sinh viên chiếm trọn hàng —
 * đúng thứ tự người soát vé đọc trên vé giấy.
 */
@Composable
private fun TicketDetailGrid(
    ticket: Ticket,
    studentName: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(UthTheme.extendedColors.surfaceSubtle)
            .padding(UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
            TicketDetailCell(
                icon = Icons.Filled.DateRange,
                label = stringResource(R.string.tickets_detail_time),
                value = ticket.eventStartsAt,
                modifier = Modifier.weight(1f),
            )
            TicketDetailCell(
                icon = Icons.Filled.Place,
                label = stringResource(R.string.tickets_detail_seat),
                value = ticket.seatLabel ?: stringResource(R.string.tickets_detail_seat_free),
                emphasize = true,
                modifier = Modifier.weight(1f),
            )
        }
        TicketDetailCell(
            icon = Icons.Filled.LocationOn,
            label = stringResource(R.string.tickets_detail_location),
            value = ticket.eventLocation,
        )
        HorizontalDivider(color = colors.outlineVariant)
        TicketDetailCell(
            icon = Icons.Filled.Person,
            label = stringResource(R.string.tickets_detail_student),
            value = studentName,
            trailing = stringResource(R.string.tickets_detail_student_id, ticket.studentId),
        )
    }
}

/** Một ô thông tin: nhãn nhỏ có icon, giá trị đậm và chip phụ tuỳ chọn bên phải. */
@Composable
private fun TicketDetailCell(
    icon: ImageVector,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    trailing: String? = null,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(UthDimens.Size.IconBadge),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (value != null || trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (emphasize) colors.primary else colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(colors.primaryContainer)
                            .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
                    )
                }
            }
        }
    }
}

/**
 * Đường xé giữa hai nửa vé: hai khuyết tròn khoét đúng màu nền màn hình và một nét đứt ở giữa —
 * vẽ bằng [Canvas] nên không cần shape riêng cho khuyết.
 */
@Composable
private fun TicketNotchDivider(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(UthDimens.Size.TicketNotchHeight),
    ) {
        val notch = UthDimens.Size.TicketNotchWidth.toPx()
        val middle = size.height / 2f
        drawCircle(color = colors.background, radius = notch, center = Offset(0f, middle))
        drawCircle(color = colors.background, radius = notch, center = Offset(size.width, middle))
        val gap = notch + UthDimens.Spacing.Sm.toPx()
        drawLine(
            color = colors.outlineVariant,
            start = Offset(gap, middle),
            end = Offset(size.width - gap, middle),
            strokeWidth = UthDimens.Size.Stroke.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(UthDimens.Spacing.Sm.toPx(), UthDimens.Spacing.Sm.toPx()),
            ),
        )
    }
}

/** Nhắc thời hạn của mã: chấm xanh + câu đếm ngược, thay cho đồng hồ "bảo mật" của bản mockup. */
@Composable
private fun TicketSecurityRibbon(secondsRemaining: Int, modifier: Modifier = Modifier) {
    val extended = UthTheme.extendedColors

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(extended.surfaceSubtle)
            .padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(UthDimens.Size.IndicatorDot)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(extended.success),
        )
        Text(
            text = stringResource(R.string.tickets_countdown, secondsRemaining),
            style = MaterialTheme.typography.labelSmall,
            color = extended.textSecondary,
            maxLines = 1,
        )
    }
}

/** Khung QR cỡ máy quét: bốn góc ngắm, mã mô phỏng và ô chữ UTH ở giữa. */
@Composable
private fun TicketQrPanel(seed: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(UthDimens.Size.QrMin)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surface)
            .border(UthDimens.Size.Stroke, colors.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawQrDesignators(
                color = colors.primary,
                inset = UthDimens.Spacing.Sm.toPx(),
                arm = UthDimens.Size.QrDesignator.toPx(),
                stroke = UthDimens.Size.StrokeFocused.toPx(),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(UthDimens.Spacing.Xl),
        ) {
            val cell = size.minDimension / QrModuleCount
            drawQrModules(seed = seed, cell = cell, ink = colors.onSurface, paper = colors.surface)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(UthDimens.Size.QrMonogram)
                .clip(MaterialTheme.shapes.small)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tickets_logo_abbr),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
            )
        }
    }
}

/** Mã vé dạng monospace + nút sao chép cùng kích thước vùng chạm tối thiểu. */
@Composable
private fun TicketCodeRow(
    code: String,
    copyLabel: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(extended.surfaceSubtle)
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md),
        )
        Box(
            modifier = Modifier
                .size(UthDimens.Size.MinTouchTarget)
                .clip(MaterialTheme.shapes.small)
                .background(extended.surfaceSubtle)
                .clickable(role = Role.Button, onClick = onCopy),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ticket_copy),
                contentDescription = copyLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
        }
    }
}

/** Bốn góc ngắm của khung QR — chỉ để mắt bám khung, không mang thông tin nên không có mô tả. */
private fun DrawScope.drawQrDesignators(color: Color, inset: Float, arm: Float, stroke: Float) {
    val corners = listOf(
        Triple(Offset(inset, inset), 1f, 1f),
        Triple(Offset(size.width - inset, inset), -1f, 1f),
        Triple(Offset(inset, size.height - inset), 1f, -1f),
        Triple(Offset(size.width - inset, size.height - inset), -1f, -1f),
    )
    corners.forEach { (origin, stepX, stepY) ->
        drawLine(color, origin, Offset(origin.x + stepX * arm, origin.y), strokeWidth = stroke)
        drawLine(color, origin, Offset(origin.x, origin.y + stepY * arm), strokeWidth = stroke)
    }
}

private val PreviewTicket = Ticket(
    id = "preview",
    studentId = "2023040123",
    eventId = "event",
    eventTitle = "Hội thao Sinh viên UTH 2026",
    eventLocation = "Sân vận động Cơ sở 2",
    eventStartsAt = "12/11/2026 · 07:00",
    status = TicketStatus.VALID,
    fallbackCode = "482 917",
    organizerName = "CLB Thể Thao UTH",
    seatLabel = "Ghế A-14",
)

private val PreviewPastTicket = Ticket(
    id = "preview-past",
    studentId = "2023040123",
    eventId = "event-past",
    eventTitle = "Giao lưu CLB Ánh Sáng",
    eventLocation = "Hội trường A.102",
    eventStartsAt = "25/10/2026 · 18:30",
    status = TicketStatus.CHECKED_IN,
    fallbackCode = "105 638",
    organizerName = "Đội CTXH Ánh Sáng",
)

private val PreviewTickets = listOf(PreviewTicket, PreviewPastTicket)

private const val PreviewStudentName = "Nguyễn Minh Anh"

@Preview(name = "Tickets upcoming light", showBackground = true)
@Composable
private fun TicketsPreview() = TicketsStatePreview(TicketsUiState(isLoading = false, tickets = PreviewTickets))

@Preview(name = "Tickets upcoming dark", showBackground = true)
@Composable
private fun TicketsDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, tickets = PreviewTickets), darkTheme = true,
)

@Preview(name = "Tickets past light", showBackground = true)
@Composable
private fun TicketsPastPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, tickets = PreviewTickets, filter = TicketFilter.Past),
)

@Preview(name = "Tickets past dark", showBackground = true)
@Composable
private fun TicketsPastDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, tickets = PreviewTickets, filter = TicketFilter.Past), darkTheme = true,
)

@Preview(name = "Tickets font scale 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun TicketsFontScalePreview() = UthPreview {
    TicketsScreen(TicketsUiState(isLoading = false, tickets = PreviewTickets), onAction = {})
}

@Preview(name = "Tickets loading light", showBackground = true)
@Composable
private fun TicketsLoadingPreview() = TicketsStatePreview(TicketsUiState())

@Preview(name = "Tickets loading dark", showBackground = true)
@Composable
private fun TicketsLoadingDarkPreview() = TicketsStatePreview(TicketsUiState(), darkTheme = true)

@Preview(name = "Tickets error light", showBackground = true)
@Composable
private fun TicketsErrorPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, failure = TicketsFailure.LoadFailed),
)

@Preview(name = "Tickets error dark", showBackground = true)
@Composable
private fun TicketsErrorDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, failure = TicketsFailure.LoadFailed), darkTheme = true,
)

@Preview(name = "Tickets empty light", showBackground = true)
@Composable
private fun TicketsEmptyPreview() = TicketsStatePreview(TicketsUiState(isLoading = false))

@Preview(name = "Tickets empty dark", showBackground = true)
@Composable
private fun TicketsEmptyDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false), darkTheme = true,
)

@Preview(name = "QR light", showBackground = true)
@Composable
private fun TicketQrPreview() = TicketQrStatePreview(PreviewTicket, 60)

@Preview(name = "QR dark", showBackground = true)
@Composable
private fun TicketQrDarkPreview() = TicketQrStatePreview(PreviewTicket, 60, darkTheme = true)

@Preview(name = "QR checked-in light", showBackground = true)
@Composable
private fun TicketQrCheckedInPreview() = TicketQrStatePreview(
    PreviewTicket.copy(status = TicketStatus.CHECKED_IN), 60,
)

@Preview(name = "QR checked-in dark", showBackground = true)
@Composable
private fun TicketQrCheckedInDarkPreview() = TicketQrStatePreview(
    PreviewTicket.copy(status = TicketStatus.CHECKED_IN), 60, darkTheme = true,
)

@Preview(name = "QR expired light", showBackground = true)
@Composable
private fun TicketQrExpiredPreview() = TicketQrStatePreview(PreviewTicket, 0)

@Preview(name = "QR expired dark", showBackground = true)
@Composable
private fun TicketQrExpiredDarkPreview() = TicketQrStatePreview(PreviewTicket, 0, darkTheme = true)

@Preview(name = "QR font scale 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun TicketQrFontScalePreview() = UthPreview {
    TicketQrScreen(PreviewTicket, PreviewStudentName, 60, onBack = {})
}

@Preview(name = "QR missing light", showBackground = true)
@Composable
private fun TicketQrMissingPreview() = TicketQrStatePreview(null, 0)

@Preview(name = "QR missing dark", showBackground = true)
@Composable
private fun TicketQrMissingDarkPreview() = TicketQrStatePreview(null, 0, darkTheme = true)

@Composable
private fun TicketsStatePreview(state: TicketsUiState, darkTheme: Boolean = false) =
    UthPreview(darkTheme = darkTheme) { TicketsScreen(state, onAction = {}) }

@Composable
private fun TicketQrStatePreview(
    ticket: Ticket?,
    seconds: Int,
    darkTheme: Boolean = false,
) = UthPreview(darkTheme = darkTheme) {
    TicketQrScreen(
        ticket = ticket,
        studentName = PreviewStudentName,
        secondsRemaining = seconds,
        onBack = {},
    )
}
