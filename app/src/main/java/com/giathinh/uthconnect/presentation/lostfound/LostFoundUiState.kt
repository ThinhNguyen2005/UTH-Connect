package com.giathinh.uthconnect.presentation.lostfound

import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing

data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val kind: ListingKind = ListingKind.LOST,
    val category: ItemCategory = ItemCategory.PERSONAL_ITEMS,
    val location: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val isSubmitting: Boolean = false,
)

data class LostFoundUiState(
    val isLoading: Boolean = false,
    val selectedKind: ListingKind = ListingKind.LOST,
    val selectedCategory: ItemCategory = ItemCategory.ALL,
    val listings: List<LostFoundListing> = emptyList(),
    val isReportDialogOpen: Boolean = false,
    val reportForm: ReportFormState = ReportFormState(),
    val userMessage: String? = null,
) {
    val filteredListings: List<LostFoundListing>
        get() = listings.filter { item ->
            val matchKind = item.kind == selectedKind
            val matchCategory = selectedCategory == ItemCategory.ALL || item.category == selectedCategory
            matchKind && matchCategory
        }

    val lostCount: Int get() = listings.count { it.kind == ListingKind.LOST }
    val foundCount: Int get() = listings.count { it.kind == ListingKind.FOUND }
}
