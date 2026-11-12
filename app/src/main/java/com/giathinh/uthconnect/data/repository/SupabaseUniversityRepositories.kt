package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementBlock
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.HomeBanner
import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import com.giathinh.uthconnect.domain.model.SemesterGpa
import com.giathinh.uthconnect.domain.model.StudentGrade
import com.giathinh.uthconnect.domain.model.User
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import com.giathinh.uthconnect.domain.repository.ScheduleRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentGradeRepository
import com.giathinh.uthconnect.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val universityZone = ZoneId.of("Asia/Ho_Chi_Minh")

private fun JsonObject.decimal(key: String): Float? = get(key)?.jsonPrimitive?.floatOrNull

/** An explicit authenticated user filter complements (never replaces) backend RLS. */
private fun SupabaseRestClient.ownRowsPath(table: String): String = "$table?select=*&user_id=eq.${urlEncode(uuid(userId))}"

@Singleton
class SupabaseStudentGradeRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : StudentGradeRepository {
    private suspend fun loadGrades(): List<StudentGrade> = client.table(client.ownRowsPath("grades"))
        .jsonArray.map { element ->
            val row = element.jsonObject
            StudentGrade(
                id = requireNotNull(row.string("id")),
                courseCode = row.string("course_code").orEmpty(),
                courseName = row.string("course_name").orEmpty(),
                credits = row.int("credits"),
                semester = row.string("semester").orEmpty(),
                attendanceScore = row.decimal("attendance_score"),
                midtermScore = row.decimal("midterm_score"),
                finalScore = row.decimal("final_score"),
                totalScore10 = row.decimal("total_score_10") ?: 0f,
                totalScore4 = row.decimal("total_score_4") ?: 0f,
                letterGrade = row.string("letter_grade").orEmpty(),
            )
        }

    override fun getGrades(semester: String?): Flow<List<StudentGrade>> = flow {
        emit(loadGrades().filter { semester == null || it.semester == semester })
    }

    override fun getSemesterGpaList(): Flow<List<SemesterGpa>> = flow {
        val records = loadGrades().filter { it.credits > 0 && it.letterGrade.isNotBlank() }
        emit(
            records.groupBy { it.semester }.map { (semester, grades) ->
                val credits = grades.sumOf { it.credits }
                SemesterGpa(
                    semester = semester,
                    gpa10 = grades.sumOf { it.totalScore10.toDouble() * it.credits }.div(credits).toFloat(),
                    gpa4 = grades.sumOf { it.totalScore4.toDouble() * it.credits }.div(credits).toFloat(),
                    totalCredits = credits,
                )
            }.sortedBy { it.semester },
        )
    }

    override fun getCumulativeGpa(): Flow<Float> = flow {
        val grades = loadGrades().filter { it.credits > 0 && it.letterGrade.isNotBlank() }
        val totalCredits = grades.sumOf { it.credits }
        emit(if (totalCredits == 0) 0f else grades.sumOf { it.totalScore4.toDouble() * it.credits }.div(totalCredits).toFloat())
    }
}

@Singleton
class SupabaseScheduleRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : ScheduleRepository {
    private suspend fun load(): List<CourseSchedule> = client.table(client.ownRowsPath("schedules"))
        .jsonArray.map { element ->
            val row = element.jsonObject
            CourseSchedule(
                id = requireNotNull(row.string("id")),
                courseCode = row.string("course_code").orEmpty(),
                courseName = row.string("course_name").orEmpty(),
                lecturerName = row.string("lecturer_name").orEmpty(),
                room = row.string("room").orEmpty(),
                campus = row.string("campus").orEmpty(),
                dayOfWeek = row.int("day_of_week"),
                startPeriod = row.int("start_period"),
                endPeriod = row.int("end_period"),
                startTime = row.string("start_time").orEmpty(),
                endTime = row.string("end_time").orEmpty(),
                // Schema has weekday and semester, but no actual calendar date.
                date = "",
                credits = row.int("credits"),
            )
        }

    override fun getSemesterSchedule(): Flow<List<CourseSchedule>> = flow { emit(load()) }

    override fun getScheduleByDayOfWeek(dayOfWeek: Int): Flow<List<CourseSchedule>> = flow {
        emit(load().filter { it.dayOfWeek == dayOfWeek })
    }

    override fun getScheduleByDate(date: String): Flow<List<CourseSchedule>> = flow {
        LocalDate.parse(date) // Validate the caller's date.
        // Without an academic calendar, a weekday cannot prove the class meets on this date.
        emit(emptyList())
    }
}

internal fun JsonObject.toUniversityNotification(): PostNotification = PostNotification(
    id = requireNotNull(string("id")),
    title = string("title").orEmpty(),
    summary = string("summary").orEmpty(),
    content = string("content").orEmpty(),
    category = NotificationCategory.fromString(string("category")),
    author = string("author").orEmpty(),
    publishedAt = Instant.parse(requireNotNull(string("published_at"))).toEpochMilli(),
    isPinned = bool("is_pinned"),
    attachmentUrl = string("attachment_url"),
    imageUrl = string("image_url"),
)

private fun JsonObject.toInboxNotification(): PostNotification = PostNotification(
    id = requireNotNull(string("id")),
    title = string("title").orEmpty(),
    summary = string("summary").orEmpty(),
    content = string("content").orEmpty(),
    category = NotificationCategory.fromString(string("category")),
    author = string("author").orEmpty(),
    publishedAt = Instant.parse(requireNotNull(string("published_at"))).toEpochMilli(),
    targetType = string("target_type"),
    targetId = string("target_id"),
    readAt = string("read_at")?.let { Instant.parse(it).toEpochMilli() },
)

@Singleton
class SupabaseNotificationRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : NotificationRepository {
    override fun getNewsfeed(category: NotificationCategory): Flow<List<PostNotification>> = flow {
        val all = client.table("notifications?select=*&order=is_pinned.desc,published_at.desc")
            .jsonArray.map { it.jsonObject.toUniversityNotification() }
        emit(all.filter { category == NotificationCategory.ALL || it.category == category })
    }

    override suspend fun getNotificationDetail(notificationId: String): Result<PostNotification?> = suspendResult {
        client.table("notifications?select=*&id=eq.${urlEncode(uuid(notificationId))}&limit=1")
            .jsonArray.singleOrNull()?.jsonObject?.toUniversityNotification()
    }

    override fun getInbox(): Flow<List<PostNotification>> = flow {
        val userId = urlEncode(uuid(client.userId))
        val rows = client.table(
            "notification_inbox?select=*&recipient_id=eq.$userId&order=published_at.desc&limit=100",
        ).jsonArray.map { it.jsonObject.toInboxNotification() }
        emit(rows)
    }

    override suspend fun markInboxNotificationRead(notificationId: String): Result<Unit> = suspendResult {
        val userId = urlEncode(uuid(client.userId))
        val id = urlEncode(uuid(notificationId))
        client.patchTable(
            "notification_inbox?id=eq.$id&recipient_id=eq.$userId&read_at=is.null",
            buildJsonObject { put("read_at", Instant.now().toString()) },
        )
        Unit
    }

    override suspend fun markAllInboxNotificationsRead(): Result<Unit> = suspendResult {
        val userId = urlEncode(uuid(client.userId))
        client.patchTable(
            "notification_inbox?recipient_id=eq.$userId&read_at=is.null",
            buildJsonObject { put("read_at", Instant.now().toString()) },
        )
        Unit
    }

    override suspend fun getInboxNotification(notificationId: String): Result<PostNotification?> = suspendResult {
        val userId = urlEncode(uuid(client.userId))
        val id = urlEncode(uuid(notificationId))
        client.table(
            "notification_inbox?select=*&id=eq.$id&recipient_id=eq.$userId&limit=1",
        ).jsonArray.singleOrNull()?.jsonObject?.toInboxNotification()
    }
}

@Singleton
class SupabaseAnnouncementRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : AnnouncementRepository {
    private fun PostNotification.asAnnouncement() = Announcement(
        id = id,
        category = when (category) {
            NotificationCategory.ACADEMIC, NotificationCategory.EXAMINATION -> AnnouncementCategory.TRAINING
            NotificationCategory.TUITION -> AnnouncementCategory.TUITION
            NotificationCategory.ACTIVITIES -> AnnouncementCategory.YOUTH_UNION
            else -> AnnouncementCategory.STUDENT_AFFAIRS
        },
        title = title,
        summary = summary.ifBlank { null },
        publisher = author,
        publishedAt = Instant.ofEpochMilli(publishedAt).atZone(universityZone).toLocalDateTime(),
    )

    override suspend fun homeFeed(): Result<HomeFeed> = suspendResult {
        val notices = client.table("notifications?select=*&order=is_pinned.desc,published_at.desc&limit=50")
            .jsonArray.map { it.jsonObject.toUniversityNotification() }
        HomeFeed(
            banners = notices.filter { it.isPinned }.take(5).map { HomeBanner(it.asAnnouncement()) },
            latest = notices.map { it.asAnnouncement() },
        )
    }

    override suspend fun detail(id: String): Result<AnnouncementDetail?> = suspendResult {
        val notice = client.table("notifications?select=*&id=eq.${urlEncode(uuid(id))}&limit=1")
            .jsonArray.singleOrNull()?.jsonObject?.toUniversityNotification() ?: return@suspendResult null
        val announcement = notice.asAnnouncement()
        AnnouncementDetail(
            id = announcement.id,
            category = announcement.category,
            title = announcement.title,
            publisher = announcement.publisher,
            publishedAt = announcement.publishedAt,
            lead = notice.summary.ifBlank { null },
            blocks = notice.content.takeIf { it.isNotBlank() }?.let { listOf(AnnouncementBlock.Paragraph(it)) }.orEmpty(),
        )
    }
}

@Singleton
class SupabaseUserRepository @Inject constructor(
    private val client: SupabaseRestClient,
    private val session: SessionRepository,
) : UserRepository {
    private suspend fun currentProfile(): User? = client.table(
        "profiles?select=*&id=eq.${urlEncode(uuid(client.userId))}&limit=1",
    ).jsonArray.singleOrNull()?.jsonObject?.let { row ->
        User(
            uid = client.userId,
            studentId = row.string("student_id").orEmpty(),
            email = row.string("email").orEmpty(),
            displayName = row.string("display_name").orEmpty(),
            className = row.string("class_name").orEmpty(),
            major = row.string("major").orEmpty(),
            faculty = row.string("faculty").orEmpty(),
            avatarUrl = row.string("avatar_url"),
            phoneNumber = row.string("phone_number"),
            role = row.string("role") ?: "student",
        )
    }

    override fun getUserProfile(uid: String): Flow<User?> = flow {
        emit(if (uid == client.userId) currentProfile() else null)
    }

    override fun getCurrentUser(): Flow<User?> = session.session.map { value ->
        if (value.profile == null) null else currentProfile()
    }

    override suspend fun updateProfile(user: User): Result<Unit> = suspendResult {
        check(user.uid == client.userId) { "Cannot update another user's profile" }
        val body = buildJsonObject {
            put("display_name", user.displayName)
            put("phone_number", user.phoneNumber ?: "")
            user.avatarUrl?.let { put("avatar_url", it) }
        }
        client.patchTable("profiles?id=eq.${urlEncode(uuid(client.userId))}", body)
        Unit
    }
}
