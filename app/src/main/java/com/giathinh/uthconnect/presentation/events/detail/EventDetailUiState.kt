package com.giathinh.uthconnect.presentation.events.detail

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.Registration

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val isRegistering: Boolean = false,
    val registration: Registration? = null,
    val failure: EventDetailFailure? = null,
    val registrationFailed: Boolean = false,
) {
    val canRegister: Boolean
        get() = event?.canRegister == true && !isRegistering && registration == null
}

enum class EventDetailFailure { LoadFailed, EventNotFound }
