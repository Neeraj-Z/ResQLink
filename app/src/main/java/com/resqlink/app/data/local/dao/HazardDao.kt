package com.resqlink.app.data.local.dao

import androidx.room.*
import com.resqlink.app.data.local.entity.HazardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HazardDao {

    @Query("SELECT * FROM hazard_zones ORDER BY issuedAtEpochMillis DESC")
    fun observeAll(): Flow<List<HazardEntity>>

    @Query("SELECT * FROM hazard_zones WHERE severity = :severity")
    fun observeBySeverity(severity: String): Flow<List<HazardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(zones: List<HazardEntity>)

    @Query("DELETE FROM hazard_zones WHERE id NOT IN (:activeIds)")
    suspend fun pruneStale(activeIds: List<String>)
}
