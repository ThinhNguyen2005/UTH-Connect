package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.repository.EventRepository
import javax.inject.Inject

class GetEventsUseCase @Inject constructor(private val repository: EventRepository) {
    /** null là tất cả cơ sở. Lỗi từ repository được giữ nguyên cho ViewModel. */
    suspend operator fun invoke(campus: Campus? = null): Result<List<Event>> =
        repository.events().map { events ->
            events.filter { campus == null || it.campus == campus }.sortedBy { it.startAt }
        }
}
