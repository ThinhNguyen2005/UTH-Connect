package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject

sealed interface SubmitVerificationOutcome {
    data class Success(val requestId: String) : SubmitVerificationOutcome
    data class InvalidStudentId(val message: String) : SubmitVerificationOutcome
    data object AlreadyVerified : SubmitVerificationOutcome
    data object AlreadyTaken : SubmitVerificationOutcome
    data class Failure(val message: String) : SubmitVerificationOutcome
}

class SubmitVerificationRequestUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(rawStudentId: String): SubmitVerificationOutcome {
        val studentId = rawStudentId.trim()
        if (studentId.length !in 5..32) {
            return SubmitVerificationOutcome.InvalidStudentId("Mã số sinh viên phải có từ 5 đến 32 ký tự")
        }

        return authRepository.submitVerificationRequest(studentId).fold(
            onSuccess = { result ->
                SubmitVerificationOutcome.Success(result.requestId)
            },
            onFailure = { error ->
                val msg = error.message.orEmpty()
                when {
                    msg.contains("STUDENT_ALREADY_VERIFIED", ignoreCase = true) ->
                        SubmitVerificationOutcome.AlreadyVerified
                    msg.contains("STUDENT_ID_ALREADY_TAKEN", ignoreCase = true) || msg.contains("23505") ->
                        SubmitVerificationOutcome.AlreadyTaken
                    msg.contains("INVALID_STUDENT_ID", ignoreCase = true) ->
                        SubmitVerificationOutcome.InvalidStudentId("Mã số sinh viên không hợp lệ")
                    else ->
                        SubmitVerificationOutcome.Failure(msg.ifEmpty { "Gửi yêu cầu thất bại" })
                }
            },
        )
    }
}

