package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Singleton
class FirebaseBookListingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : BookListingRepository {
    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun observeListings(): Flow<List<BookListing>> = callbackFlow {
        if (auth.currentUser == null) {
            close(IllegalStateException("Sign in to browse book listings"))
            return@callbackFlow
        }
        val registration = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) runCatching { snapshot.documents.mapNotNull { it.toListing() } }
                    .onSuccess { trySend(it) }
                    .onFailure { close(it) }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun saveListing(listing: BookListing): Result<Unit> = persist {
        val uid = auth.currentUser?.uid ?: error("Sign in to post a book")
        val document = firestore.collection(COLLECTION).document(listing.id)
        val existing = document.get().await()
        if (existing.exists() && existing.getString("ownerUid") != uid) {
            throw SecurityException("Only the listing owner can edit it")
        }
        val uploadedPhoto = listing.photoUri?.let { uri ->
            val photoUri = Uri.parse(uri)
            val contentType = context.contentResolver.getType(photoUri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(photoUri.toString()),
                )
                ?: error("Could not determine the selected image type")
            require(contentType.startsWith("image/", ignoreCase = true)) { "Only image files can be uploaded" }
            val reference = storage.reference.child("book_listings/$uid/${listing.id}/${UUID.randomUUID()}")
            val metadata = StorageMetadata.Builder().setContentType(contentType).build()
            reference.putFile(photoUri, metadata).await()
            reference.downloadUrl.await().toString()
        }
        val saved = listing.copy(ownerUid = uid, photoUrl = uploadedPhoto ?: listing.photoUrl, photoUri = null)
        document.set(saved.toMap()).await()
        if (uploadedPhoto != null) existing.getString("photoUrl")?.let { oldUrl ->
            runCatching { storage.getReferenceFromUrl(oldUrl).delete().await() }
        }
    }

    override suspend fun deleteListing(id: String): Result<Unit> = persist {
        val uid = auth.currentUser?.uid ?: error("Sign in to delete a listing")
        val document = firestore.collection(COLLECTION).document(id)
        val existing = document.get().await()
        if (!existing.exists()) return@persist
        if (existing.getString("ownerUid") != uid) throw SecurityException("Only the listing owner can delete it")
        val photoUrl = existing.getString("photoUrl")
        document.delete().await()
        photoUrl?.let { runCatching { storage.getReferenceFromUrl(it).delete().await() } }
    }

    private fun BookListing.toMap() = mapOf(
        "title" to title,
        "subject" to subject,
        "condition" to condition.name,
        "dealType" to dealType.name,
        "priceVnd" to priceVnd,
        "description" to description,
        "sellerName" to sellerName,
        "contact" to contact,
        "ownerUid" to ownerUid,
        "photoUrl" to photoUrl,
        "status" to status.name,
        "createdAt" to createdAt,
    )

    private fun DocumentSnapshot.toListing(): BookListing? = runCatching {
        BookListing(
            id = id,
            title = getString("title") ?: return null,
            subject = getString("subject") ?: return null,
            condition = BookCondition.valueOf(getString("condition") ?: return null),
            dealType = BookDealType.valueOf(getString("dealType") ?: return null),
            priceVnd = getLong("priceVnd"),
            description = getString("description").orEmpty(),
            sellerName = getString("sellerName") ?: return null,
            contact = getString("contact") ?: return null,
            ownerUid = getString("ownerUid") ?: return null,
            photoUrl = getString("photoUrl"),
            status = BookListingStatus.valueOf(getString("status") ?: return null),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }.getOrNull()

    private suspend fun persist(action: suspend () -> Unit): Result<Unit> = try {
        action()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private companion object { const val COLLECTION = "book_listings" }
}
