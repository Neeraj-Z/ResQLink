package com.resqlink.app.data.local.dao

import androidx.room.*
import com.resqlink.app.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family_members")
    fun observeAll(): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun delete(id: String)
}
