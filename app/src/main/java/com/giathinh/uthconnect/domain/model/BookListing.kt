package com.giathinh.uthconnect.domain.model

enum class BookCondition { NEW, GOOD, USED }
enum class BookDealType { SALE, EXCHANGE }
enum class BookListingStatus { AVAILABLE, SOLD }

data class BookListing(
    val id: String,
    val title: String,
    val subject: String,
    val condition: BookCondition,
    val dealType: BookDealType,
    val priceVnd: Long?,
    val description: String,
    val sellerName: String,
    val contact: String,
    val ownerUid: String = "",
    val photoUrl: String? = null,
    val photoUri: String? = null,
    val status: BookListingStatus = BookListingStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
)
