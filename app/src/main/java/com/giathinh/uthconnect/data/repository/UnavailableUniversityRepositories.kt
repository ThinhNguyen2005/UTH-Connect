package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import javax.inject.Inject

/** UTH has not authorized a tuition or training-points data source for this application. */
class UnavailableTuitionRepository @Inject constructor() : TuitionRepository {
    override suspend fun getTuition(): Result<TuitionInfo> = Result.failure(UnsupportedOperationException("Chưa kết nối dữ liệu học phí chính thức từ UTH"))
}

class UnavailableTrainingPointRepository @Inject constructor() : TrainingPointRepository {
    override suspend fun getTrainingPoint(): Result<TrainingPointInfo> = Result.failure(UnsupportedOperationException("Chưa kết nối dữ liệu điểm rèn luyện chính thức từ UTH"))
}
