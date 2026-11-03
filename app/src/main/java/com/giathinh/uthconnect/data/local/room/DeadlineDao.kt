package com.giathinh.uthconnect.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines ORDER BY dueDateEpochDay")
    fun observeAll(): Flow<List<DeadlineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deadline: DeadlineEntity)

    @Query("DELETE FROM deadlines WHERE id = :id")
    suspend fun deleteById(id: String)
}
