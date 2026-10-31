package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.TrainingPointInfo

interface TrainingPointRepository {
    suspend fun getTrainingPoint(): Result<TrainingPointInfo>
}
