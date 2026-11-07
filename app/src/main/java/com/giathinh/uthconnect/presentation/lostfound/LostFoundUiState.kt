package com.giathinh.uthconnect.presentation.lostfound

import androidx.annotation.StringRes
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing

/**
 * Validation errors associated with reporting a lost or found item.
 */
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

/**
 * One-off notification messages surfaced via snackbars.
 */
sealed interface UserMessage {
    @get:StringRes
    val messageRes: Int

    data object PostSuccess : UserMessage {
        override val messageRes: Int = R.string.lostfound_msg_post_success
    }

    data object PostFailed : UserMessage {
        override val messageRes: Int = R.string.lostfound_msg_post_failed
    }

    data object AuthRequired : UserMessage {
        override val messageRes: Int = R.string.common_error_auth_required
    }
}

/**
 * State representing fields and validation of the report item form.
 */
data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val kind: ListingKind = ListingKind.LOST,
    val category: ItemCategory = ItemCategory.PERSONAL_ITEMS,
    val location: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val imageBytes: ByteArray? = null,
    val imageMimeType: String? = null,
    val imagePreviewUri: String? = null,
    val titleError: FormValidationError? = null,
    val descriptionError: FormValidationError? = null,
    val locationError: FormValidationError? = null,
    val phoneError: FormValidationError? = null,
    val isSubmitting: Boolean = false,
) {
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportFormState) return false
        return title == other.title &&
            description == other.description &&
            kind == other.kind &&
            category == other.category &&
            location == other.location &&
            contactName == other.contactName &&
            contactPhone == other.contactPhone &&
            imageBytes.contentEquals(other.imageBytes) &&
            imageMimeType == other.imageMimeType &&
            imagePreviewUri == other.imagePreviewUri &&
            titleError == other.titleError &&
            descriptionError == other.descriptionError &&
            locationError == other.locationError &&
            phoneError == other.phoneError &&
            isSubmitting == other.isSubmitting
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + contactPhone.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (imageMimeType?.hashCode() ?: 0)
        result = 31 * result + (imagePreviewUri?.hashCode() ?: 0)
        result = 31 * result + (titleError?.hashCode() ?: 0)
        result = 31 * result + (descriptionError?.hashCode() ?: 0)
        result = 31 * result + (locationError?.hashCode() ?: 0)
        result = 31 * result + (phoneError?.hashCode() ?: 0)
        result = 31 * result + isSubmitting.hashCode()
        return result
    }
}

/**
 * UI state for the Lost & Found screen.
 */
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
