package com.giathinh.uthconnect.presentation.lostfound

import androidx.annotation.StringRes
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing

sealed interface FormValidationError {
    @get:StringRes
    val messageRes: Int

    data object TitleRequired : FormValidationError {
        override val messageRes: Int = R.string.lostfound_err_title_required
    }

    data class DescriptionTooShort(val currentLength: Int) : FormValidationError {
        override val messageRes: Int = R.string.lostfound_err_desc_min_length
    }

    data object LocationRequired : FormValidationError {
        override val messageRes: Int = R.string.lostfound_err_location_required
    }

    data object PhoneRequired : FormValidationError {
        override val messageRes: Int = R.string.lostfound_err_phone_required
    }

    data object PhoneInvalid : FormValidationError {
        override val messageRes: Int = R.string.lostfound_err_phone_invalid
    }
}

sealed interface UserMessage {
    @get:StringRes
    val messageRes: Int

    data object PostSuccess : UserMessage {
        override val messageRes: Int = R.string.lostfound_msg_post_success
    }

    data object PostFailed : UserMessage {
        override val messageRes: Int = R.string.lostfound_msg_post_failed
    }
}

data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val kind: ListingKind = ListingKind.LOST,
    val category: ItemCategory = ItemCategory.PERSONAL_ITEMS,
    val location: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val titleError: FormValidationError? = null,
    val descriptionError: FormValidationError? = null,
    val locationError: FormValidationError? = null,
    val phoneError: FormValidationError? = null,
    val isSubmitting: Boolean = false,
)

data class LostFoundUiState(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val selectedKind: ListingKind = ListingKind.LOST,
    val selectedCategory: ItemCategory = ItemCategory.ALL,
    val searchQuery: String = "",
    val listings: List<LostFoundListing> = emptyList(),
    val isReportDialogOpen: Boolean = false,
    val reportForm: ReportFormState = ReportFormState(),
    val userMessage: UserMessage? = null,
) {
    val filteredListings: List<LostFoundListing>
        get() = listings.filter { item ->
            val matchKind = item.kind == selectedKind
            val matchCategory = selectedCategory == ItemCategory.ALL || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true)
            matchKind && matchCategory && matchQuery
        }

    val lostCount: Int get() = listings.count { it.kind == ListingKind.LOST }
    val foundCount: Int get() = listings.count { it.kind == ListingKind.FOUND }
    val isEmpty: Boolean get() = !isLoading && !isError && filteredListings.isEmpty()
}
