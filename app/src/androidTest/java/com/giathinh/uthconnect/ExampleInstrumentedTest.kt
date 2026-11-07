package com.giathinh.uthconnect

import android.net.Uri
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.data.repository.FirebaseBookListingRepository
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.giathinh.uthconnect", appContext.packageName)
    }

    @Test
    fun bookListingPhotoUploadIsAcceptedByStorageRules() = runBlocking {
        assumeTrue(BuildConfig.USE_FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val auth = FirebaseAuth.getInstance()
        val suffix = UUID.randomUUID().toString()
        val email = "kan88-$suffix@ut.edu.vn"
        val password = "$suffix-Aa1!"
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user!!.uid
        val photo = File(context.cacheDir, "kan88-$suffix.jpg")
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            photo.outputStream().use { compress(Bitmap.CompressFormat.JPEG, 100, it) }
            recycle()
        }
        val listingId = UUID.randomUUID().toString()
        val firestore = FirebaseFirestore.getInstance()
        val repository = FirebaseBookListingRepository(context, auth, firestore, FirebaseStorage.getInstance())
        val listing = BookListing(
            id = listingId,
            title = "KAN-88 upload regression",
            subject = "Signals & Systems",
            condition = BookCondition.GOOD,
            dealType = BookDealType.SALE,
            priceVnd = 250_000,
            description = "Firebase Storage emulator test",
            sellerName = "KAN-88 QA",
            contact = email,
            photoUri = Uri.fromFile(photo).toString(),
        )

        val document = firestore.collection("book_listings").document(listingId)
        val nonImage = File(context.cacheDir, "kan88-$suffix.txt").apply { writeText("not an image") }
        val invalidListing = listing.copy(
            id = UUID.randomUUID().toString(),
            photoUri = Uri.fromFile(nonImage).toString(),
        )
        assertFalse("Non-image files must be rejected", repository.saveListing(invalidListing).isSuccess)
        assertFalse(firestore.collection("book_listings").document(invalidListing.id).get().await().exists())

        val result = repository.saveListing(listing)
        assertTrue("Book photo upload failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val saved = document.get().await()
        assertTrue(saved.exists())
        val photoUrl = saved.getString("photoUrl").orEmpty()
        assertTrue(photoUrl.isNotBlank())

        val soldListing = listing.copy(
            ownerUid = uid,
            photoUri = null,
            photoUrl = photoUrl,
            status = BookListingStatus.SOLD,
            title = "KAN-88 uploaded and edited",
        )
        assertTrue(repository.saveListing(soldListing).isSuccess)
        val updated = document.get().await()
        assertEquals("KAN-88 uploaded and edited", updated.getString("title"))
        assertEquals("SOLD", updated.getString("status"))

        auth.signOut()
        auth.createUserWithEmailAndPassword("kan88-other-$suffix@ut.edu.vn", "$suffix-Bb2!").await()
        var directUpdateDenied = false
        try {
            document.update("title", "unauthorized").await()
        } catch (failure: FirebaseFirestoreException) {
            directUpdateDenied = failure.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Firestore rules must reject another user's update", directUpdateDenied)
        assertFalse("Repository must reject another user's delete", repository.deleteListing(listingId).isSuccess)
        assertEquals("KAN-88 uploaded and edited", document.get().await().getString("title"))

        auth.signOut()
        auth.signInWithEmailAndPassword(email, password).await()
        assertTrue(repository.deleteListing(listingId).isSuccess)
        assertFalse(document.get().await().exists())
        auth.signOut()
    }
}
