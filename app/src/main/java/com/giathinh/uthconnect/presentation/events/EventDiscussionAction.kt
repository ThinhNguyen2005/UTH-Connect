package com.giathinh.uthconnect.presentation.events

import com.giathinh.uthconnect.domain.model.Event

sealed interface EventDiscussionAction {
    data class Open(val event: Event) : EventDiscussionAction
    data object Close : EventDiscussionAction
    data class DraftChange(val text: String) : EventDiscussionAction
    data class Send(val eventId: String) : EventDiscussionAction
    data class OpenGuestProfile(val studentId: String) : EventDiscussionAction
    data object CloseGuestProfile : EventDiscussionAction
}
