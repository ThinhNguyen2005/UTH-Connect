package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.model.IncidentSummary

interface IncidentRepository {
    suspend fun createReport(draft: IncidentDraft): Result<IncidentReport>

    suspend fun getMyReports(): Result<List<IncidentReport>>

    suspend fun getReport(id: String): Result<IncidentReport>

    suspend fun getOpenReports(campus: String?): Result<List<IncidentSummary>>

    suspend fun getStatusHistory(id: String): Result<List<IncidentStatusLog>>

    suspend fun confirmReport(id: String): Result<Long>

    suspend fun transitionReport(
        id: String,
        expectedVersion: Int,
        targetStatus: IncidentStatus,
        note: String? = null,
        duplicateOfId: String? = null,
    ): Result<Int>
}
