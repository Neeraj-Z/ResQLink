package com.resqlink.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resqlink.app.data.local.entity.FirstAidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FirstAidDao {

    @Query("SELECT * FROM first_aid_guides ORDER BY category, title")
    fun observeAll(): Flow<List<FirstAidEntity>>

    @Query("SELECT * FROM first_aid_guides WHERE category = :category")
    fun observeByCategory(category: String): Flow<List<FirstAidEntity>>

    @Query("SELECT * FROM first_aid_guides WHERE title LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<FirstAidEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(guides: List<FirstAidEntity>)
}
