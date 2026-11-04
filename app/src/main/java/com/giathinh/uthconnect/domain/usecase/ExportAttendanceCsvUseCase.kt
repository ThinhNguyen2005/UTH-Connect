package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.AttendanceRoster
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AttendanceCsvLabels(
    val event: String,
    val exportedAt: String,
    val studentId: String,
    val name: String,
    val email: String,
    val status: String,
    val checkedInAt: String,
    val checkedInBy: String,
    val present: String,
    val missing: String,
)

class ExportAttendanceCsvUseCase @Inject constructor() {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))

    operator fun invoke(roster: AttendanceRoster, labels: AttendanceCsvLabels, exportedAt: Instant): String {
        val lines = listOf(
            listOf(labels.event, roster.eventTitle),
            listOf(labels.exportedAt, "${formatter.format(exportedAt)} Asia/Ho_Chi_Minh"),
            emptyList(),
            listOf(labels.studentId, labels.name, labels.email, labels.status, labels.checkedInAt, labels.checkedInBy),
        ) + roster.entries.map { entry ->
            listOf(
                entry.studentId, entry.displayName, entry.email,
                if (entry.checkedIn) labels.present else labels.missing,
                entry.checkedInAt?.let { formatter.format(Instant.parse(it)) }.orEmpty(),
                entry.checkedInBy.orEmpty(),
            )
        }
        return lines.joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { csvCell(it) } }
    }

    private fun csvCell(value: String): String {
        // Prevent spreadsheet formula evaluation when opening user-provided names or email values.
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
