package com.giathinh.uthconnect.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.giathinh.uthconnect.domain.model.Deadline
import java.time.LocalDate

@Entity(tableName = "deadlines")
data class DeadlineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val courseName: String,
    val dueDateEpochDay: Long,
    val note: String,
) {
    fun toDomain() = Deadline(id, title, courseName, LocalDate.ofEpochDay(dueDateEpochDay), note)

    companion object {
        fun fromDomain(deadline: Deadline) = DeadlineEntity(
            deadline.id,
            deadline.title,
            deadline.courseName,
            deadline.dueDate.toEpochDay(),
            deadline.note,
        )
    }
}
