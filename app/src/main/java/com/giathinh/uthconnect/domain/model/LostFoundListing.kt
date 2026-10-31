package com.giathinh.uthconnect.domain.model

enum class ListingKind {
    LOST,
    FOUND,
}

enum class ItemCategory {
    ALL,
    STUDENT_CARD,
    ELECTRONICS,
    WALLET_PAPERS,
    PERSONAL_ITEMS,
    OTHER,
}

data class LostFoundListing(
    val id: String = "",
    val title: String,
    val description: String,
    val kind: ListingKind,
    val category: ItemCategory,
    val location: String,
    val date: String,
    val contactName: String,
    val contactPhone: String,
    val isResolved: Boolean = false,
)
