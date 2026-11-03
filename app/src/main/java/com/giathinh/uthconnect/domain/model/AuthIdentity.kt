package com.giathinh.uthconnect.domain.model

/** Danh tính Firebase đang chờ xác minh; không phải phiên có quyền truy cập Main. */
data class AuthIdentity(val uid: String, val email: String, val emailVerified: Boolean)
