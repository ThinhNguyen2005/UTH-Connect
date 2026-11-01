package com.giathinh.uthconnect.presentation.lostfound

import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind

sealed interface LostFoundAction {
    data class SelectKind(val kind: ListingKind) : LostFoundAction
    data class SelectCategory(val category: ItemCategory) : LostFoundAction
    data object OpenReportDialog : LostFoundAction
    data object CloseReportDialog : LostFoundAction
    data class UpdateReportTitle(val title: String) : LostFoundAction
    data class UpdateReportDescription(val description: String) : LostFoundAction
    data class UpdateReportKind(val kind: ListingKind) : LostFoundAction
    data class UpdateReportCategory(val category: ItemCategory) : LostFoundAction
    data class UpdateReportLocation(val location: String) : LostFoundAction
    data class UpdateReportContactName(val contactName: String) : LostFoundAction
    data class UpdateReportContactPhone(val contactPhone: String) : LostFoundAction
    data object SubmitReport : LostFoundAction
    data object DismissMessage : LostFoundAction
    data object Retry : LostFoundAction
}
