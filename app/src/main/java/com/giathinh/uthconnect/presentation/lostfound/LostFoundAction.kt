package com.giathinh.uthconnect.presentation.lostfound

import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind

sealed interface LostFoundAction {
    data class SelectKind(val kind: ListingKind) : LostFoundAction
    data class SelectCategory(val category: ItemCategory) : LostFoundAction
    data class UpdateSearchQuery(val query: String) : LostFoundAction
    data class ToggleResolveListing(val id: String) : LostFoundAction
    data object OpenReportDialog : LostFoundAction
    data object CloseReportDialog : LostFoundAction
    data class UpdateReportTitle(val title: String) : LostFoundAction
    data class UpdateReportDescription(val description: String) : LostFoundAction
    data class UpdateReportKind(val kind: ListingKind) : LostFoundAction
    data class UpdateReportCategory(val category: ItemCategory) : LostFoundAction
    data class UpdateReportLocation(val location: String) : LostFoundAction
    data class UpdateReportContactName(val contactName: String) : LostFoundAction
    data class UpdateReportContactPhone(val contactPhone: String) : LostFoundAction
    data class UpdateReportImage(
        val bytes: ByteArray?,
        val mimeType: String?,
        val previewUri: String?,
    ) : LostFoundAction {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UpdateReportImage) return false
            return bytes.contentEquals(other.bytes) &&
                mimeType == other.mimeType &&
                previewUri == other.previewUri
        }

        override fun hashCode(): Int {
            var result = bytes?.contentHashCode() ?: 0
            result = 31 * result + (mimeType?.hashCode() ?: 0)
            result = 31 * result + (previewUri?.hashCode() ?: 0)
            return result
        }
    }
    data object RemoveReportImage : LostFoundAction
    data object SubmitReport : LostFoundAction
    data object DismissMessage : LostFoundAction
    data object Retry : LostFoundAction
}
