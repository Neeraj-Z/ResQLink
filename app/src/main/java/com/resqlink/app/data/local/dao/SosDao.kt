package com.resqlink.app.data.local.dao

import androidx.room.*
import com.resqlink.app.data.local.entity.SosEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SosDao {

    @Query("SELECT * FROM sos_alerts ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<SosEntity>>

    @Query("SELECT * FROM sos_alerts WHERE isSyncedToServer = 0")
    suspend fun getUnsynced(): List<SosEntity>

    /** Used by the mesh relay to avoid re-storing/re-broadcasting an alert it has already seen -- this is what stops flood loops. */
    @Query("SELECT EXISTS(SELECT 1 FROM sos_alerts WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: SosEntity)

    @Update
    suspend fun update(alert: SosEntity)

    @Query("UPDATE sos_alerts SET isSyncedToServer = 1, status = :status WHERE id = :id")
    suspend fun markSynced(id: String, status: String)

    @Query("DELETE FROM sos_alerts WHERE id = :id")
    suspend fun delete(id: String)
}
