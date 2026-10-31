package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.TuitionInfo

interface TuitionRepository {
    suspend fun getTuition(): Result<TuitionInfo>
}
