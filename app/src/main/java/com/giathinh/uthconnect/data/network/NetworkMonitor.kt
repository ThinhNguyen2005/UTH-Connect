package com.giathinh.uthconnect.data.network

import kotlinx.coroutines.flow.Flow

/**
 * Giám sát trạng thái kết nối mạng của thiết bị (Online / Offline).
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
