package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.LostFoundListing

interface LostFoundRepository {
    suspend fun getListings(): Result<List<LostFoundListing>>
    suspend fun reportItem(item: LostFoundListing): Result<LostFoundListing>
}
