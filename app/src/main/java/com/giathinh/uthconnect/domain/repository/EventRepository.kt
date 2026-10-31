package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Event

interface EventRepository {
    suspend fun events(): Result<List<Event>>
}
