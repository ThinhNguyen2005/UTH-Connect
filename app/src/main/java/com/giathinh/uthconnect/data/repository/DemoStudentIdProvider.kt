package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Temporary profile for the demo. Replace with the Auth session after AUTH-1 is merged. */
@Singleton
class DemoStudentIdProvider @Inject constructor() : StudentIdProvider {
    override suspend fun studentId(): Result<String> = Result.success(DEMO_STUDENT_ID)

    private companion object {
        const val DEMO_STUDENT_ID = "2023040123"
    }
}
