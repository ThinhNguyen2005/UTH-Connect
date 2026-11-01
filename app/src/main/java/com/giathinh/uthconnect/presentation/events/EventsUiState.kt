package com.giathinh.uthconnect.presentation.events

import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event

data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val selectedCampus: Campus? = null,
    val failure: EventsFailure? = null,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && events.isEmpty()
}

enum class EventsFailure { LoadFailed }
