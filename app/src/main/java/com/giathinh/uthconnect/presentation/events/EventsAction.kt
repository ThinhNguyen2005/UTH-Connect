package com.giathinh.uthconnect.presentation.events

import com.giathinh.uthconnect.domain.model.Campus

sealed interface EventsAction {
    data object Retry : EventsAction
    data class SelectCampus(val campus: Campus?) : EventsAction
    data class OpenDetail(val eventId: String) : EventsAction
    data class Register(val eventId: String) : EventsAction
}
