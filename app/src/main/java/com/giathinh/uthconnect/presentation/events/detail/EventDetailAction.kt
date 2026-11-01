package com.giathinh.uthconnect.presentation.events.detail

sealed interface EventDetailAction {
    data object Retry : EventDetailAction
    data object Register : EventDetailAction
}
