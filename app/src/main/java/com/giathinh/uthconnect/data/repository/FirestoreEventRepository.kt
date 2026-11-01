package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreEventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EventRepository {

    override fun getEvents(campus: String?): Flow<List<Event>> = callbackFlow {
        var query: Query = firestore.collection(EVENTS_COLLECTION)
            .orderBy(FIELD_START_AT, Query.Direction.ASCENDING)

        if (!campus.isNullOrBlank() && campus != "Tất cả") {
            query = query.whereEqualTo(FIELD_CAMPUS, campus)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val events = snapshot?.documents?.mapNotNull { it.toEvent() }.orEmpty()
            trySend(events)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getEventDetail(eventId: String): Result<Event?> = runCatching {
        val doc = firestore.collection(EVENTS_COLLECTION).document(eventId).get().await()
        if (doc.exists()) doc.toEvent() else null
    }

    override suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit> = runCatching {
        val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
        val registrationRef = eventRef.collection(REGISTRATIONS_COLLECTION).document(uid)

        firestore.runTransaction { transaction ->
            val eventDoc = transaction.get(eventRef)
            if (!eventDoc.exists()) {
                throw IllegalStateException("Sự kiện không tồn tại")
            }

            val currentRegistered = eventDoc.getLong(FIELD_REGISTERED_COUNT)?.toInt() ?: 0
            val capacity = eventDoc.getLong(FIELD_CAPACITY)?.toInt() ?: 0

            val regDoc = transaction.get(registrationRef)
            val alreadyRegistered = regDoc.exists() && regDoc.getString(FIELD_STATUS) == STATUS_REGISTERED

            if (alreadyRegistered) {
                return@runTransaction
            }

            if (capacity > 0 && currentRegistered >= capacity) {
                throw IllegalStateException("Sự kiện đã hết số lượng đăng ký")
            }

            transaction.update(eventRef, FIELD_REGISTERED_COUNT, currentRegistered + 1)
            val registrationData = mapOf(
                "uid" to uid,
                "studentId" to studentId,
                "studentName" to studentName,
                "registeredAt" to FieldValue.serverTimestamp(),
                FIELD_STATUS to STATUS_REGISTERED,
            )
            transaction.set(registrationRef, registrationData)
        }.await()
    }

    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = runCatching {
        val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
        val registrationRef = eventRef.collection(REGISTRATIONS_COLLECTION).document(uid)

        firestore.runTransaction { transaction ->
            val regDoc = transaction.get(registrationRef)
            if (!regDoc.exists() || regDoc.getString(FIELD_STATUS) != STATUS_REGISTERED) {
                return@runTransaction
            }

            val eventDoc = transaction.get(eventRef)
            val currentRegistered = eventDoc.getLong(FIELD_REGISTERED_COUNT)?.toInt() ?: 1

            val newCount = (currentRegistered - 1).coerceAtLeast(0)
            transaction.update(eventRef, FIELD_REGISTERED_COUNT, newCount)
            transaction.update(registrationRef, FIELD_STATUS, STATUS_CANCELLED)
        }.await()
    }

    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = callbackFlow {
        val registrationRef = firestore.collection(EVENTS_COLLECTION)
            .document(eventId)
            .collection(REGISTRATIONS_COLLECTION)
            .document(uid)

        val listener = registrationRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            val registered = snapshot?.exists() == true && snapshot.getString(FIELD_STATUS) == STATUS_REGISTERED
            trySend(registered)
        }

        awaitClose { listener.remove() }
    }

    private fun DocumentSnapshot.toEvent(): Event? {
        val title = getString(FIELD_TITLE) ?: return null
        val startAtLong = when (val value = get(FIELD_START_AT)) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            else -> 0L
        }
        val endAtLong = when (val value = get(FIELD_END_AT)) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            else -> 0L
        }

        return Event(
            id = id,
            title = title,
            description = getString(FIELD_DESCRIPTION) ?: "",
            orgId = getString(FIELD_ORG_ID) ?: "",
            orgName = getString(FIELD_ORG_NAME) ?: "",
            location = getString(FIELD_LOCATION) ?: "",
            campus = getString(FIELD_CAMPUS) ?: "Quang Trung",
            startAt = startAtLong,
            endAt = endAtLong,
            capacity = getLong(FIELD_CAPACITY)?.toInt() ?: 100,
            registeredCount = getLong(FIELD_REGISTERED_COUNT)?.toInt() ?: 0,
            status = EventStatus.fromString(getString(FIELD_STATUS)),
            coverUrl = getString(FIELD_COVER_URL),
        )
    }

    companion object {
        private const val EVENTS_COLLECTION = "events"
        private const val REGISTRATIONS_COLLECTION = "registrations"
        private const val FIELD_TITLE = "title"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_ORG_ID = "orgId"
        private const val FIELD_ORG_NAME = "orgName"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_CAMPUS = "campus"
        private const val FIELD_START_AT = "startAt"
        private const val FIELD_END_AT = "endAt"
        private const val FIELD_CAPACITY = "capacity"
        private const val FIELD_REGISTERED_COUNT = "registeredCount"
        private const val FIELD_STATUS = "status"
        private const val FIELD_COVER_URL = "coverUrl"

        private const val STATUS_REGISTERED = "registered"
        private const val STATUS_CANCELLED = "cancelled"
    }
}
