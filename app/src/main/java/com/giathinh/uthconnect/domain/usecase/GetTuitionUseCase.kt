package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import javax.inject.Inject

class GetTuitionUseCase @Inject constructor(
    private val repository: TuitionRepository,
) {
    suspend operator fun invoke(): Result<TuitionInfo> = repository.getTuition()
}
