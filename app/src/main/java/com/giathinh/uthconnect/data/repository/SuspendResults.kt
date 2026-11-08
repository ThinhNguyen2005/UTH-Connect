package com.giathinh.uthconnect.data.repository

import kotlinx.coroutines.CancellationException

/** Like runCatching, but never converts coroutine cancellation into a domain failure. */
internal suspend inline fun <T> suspendResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
