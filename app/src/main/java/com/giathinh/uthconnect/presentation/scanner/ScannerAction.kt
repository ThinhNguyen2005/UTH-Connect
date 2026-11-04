package com.giathinh.uthconnect.presentation.scanner

sealed interface ScannerAction {
    data class CodeChanged(val value: String) : ScannerAction
    data class CameraCodeDetected(val value: String) : ScannerAction
    data class CameraPermissionChanged(val granted: Boolean) : ScannerAction
    data class CameraErrorChanged(val hasError: Boolean) : ScannerAction
    data object ClearScan : ScannerAction
    data object Submit : ScannerAction
}
