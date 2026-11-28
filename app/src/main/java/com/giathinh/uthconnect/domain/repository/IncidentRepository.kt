package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport

interface IncidentRepository {
    suspend fun createReport(draft: IncidentDraft): Result<IncidentReport>

    suspend fun getMyReports(): Result<List<IncidentReport>>

    suspend fun getReport(id: String): Result<IncidentReport>
}
