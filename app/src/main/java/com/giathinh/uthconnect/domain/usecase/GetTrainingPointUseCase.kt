package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import javax.inject.Inject

class GetTrainingPointUseCase @Inject constructor(
    private val repository: TrainingPointRepository,
) {
    suspend operator fun invoke(): Result<TrainingPointInfo> = repository.getTrainingPoint()
}
