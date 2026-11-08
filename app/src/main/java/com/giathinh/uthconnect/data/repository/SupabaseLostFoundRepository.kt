package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Authenticated, RLS-protected community reports; never seeds fictional postings. */
@Singleton
class SupabaseLostFoundRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : LostFoundRepository {
    override suspend fun getListings(): Result<List<LostFoundListing>> = suspendResult {
        client.table("lost_found?select=*&order=created_at.desc&limit=100")
            .jsonArray.map { it.jsonObject.toLostFoundListing() }
    }

    override suspend fun reportItem(item: LostFoundListing): Result<LostFoundListing> = suspendResult {
        require(item.title.isNotBlank() && item.description.length >= 20 && item.location.isNotBlank())
        require(item.category != ItemCategory.ALL)
        require(item.contactPhone.isNotBlank())
        val payload = buildJsonObject {
            put("owner_user_id", uuid(client.userId))
            put("title", item.title.trim())
            put("description", item.description.trim())
            put("kind", item.kind.name)
            put("category", item.category.name)
            put("location", item.location.trim())
            put("contact_name", item.contactName.trim())
            put("contact_phone", item.contactPhone.trim())
        }
        client.insertTable("lost_found?select=*", payload).jsonArray.single().jsonObject.toLostFoundListing()
    }

    private fun JsonObject.toLostFoundListing(): LostFoundListing = LostFoundListing(
        id = requireNotNull(string("id")),
        title = string("title").orEmpty(),
        description = string("description").orEmpty(),
        kind = ListingKind.valueOf(requireNotNull(string("kind"))),
        category = ItemCategory.valueOf(requireNotNull(string("category"))),
        location = string("location").orEmpty(),
        date = DATE_FORMAT.format(Instant.parse(requireNotNull(string("created_at")))),
        contactName = string("contact_name").orEmpty(),
        contactPhone = string("contact_phone").orEmpty(),
        isResolved = bool("is_resolved"),
    )

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
    }
}
