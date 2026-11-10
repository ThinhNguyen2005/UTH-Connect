package com.giathinh.uthconnect.presentation.attendance

sealed interface AttendanceAction {
    data object Refresh : AttendanceAction
    data class Search(val query: String) : AttendanceAction
    data class Filter(val filter: AttendanceFilter) : AttendanceAction
}
