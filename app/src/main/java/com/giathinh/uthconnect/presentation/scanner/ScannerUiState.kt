package com.giathinh.uthconnect.presentation.scanner

import com.giathinh.uthconnect.domain.usecase.CheckInResult

data class ScannerUiState(
    val authorized: Boolean = false,
    val code: String = "",
    val result: CheckInResult? = null,
    val successCount: Int = 0,
    val cameraPermissionGranted: Boolean = false,
    val cameraError: Boolean = false,
    val scanSession: Int = 0,
    val isSubmitting: Boolean = false,
)
