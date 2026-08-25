package com.resqlink.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.resqlink.app.data.local.dao.FamilyDao
import com.resqlink.app.data.local.dao.FirstAidDao
import com.resqlink.app.data.local.dao.HazardDao
import com.resqlink.app.data.local.dao.SosDao
import com.resqlink.app.data.local.dao.UserDao
import com.resqlink.app.data.local.entity.FamilyMemberEntity
import com.resqlink.app.data.local.entity.FirstAidEntity
import com.resqlink.app.data.local.entity.HazardEntity
import com.resqlink.app.data.local.entity.SosEntity
import com.resqlink.app.data.local.entity.UserEntity

/**
 * Single Room database for the whole app. ResQLink is offline-first, so this
 * database -- not the network -- is the primary source of truth for the UI.
 * Bump [version] and supply a Migration whenever the schema changes; never
 * rely on destructive fallback in a shipped build.
 */
@Database(
    entities = [
        SosEntity::class,
        HazardEntity::class,
        FirstAidEntity::class,
        FamilyMemberEntity::class,
        UserEntity::class
    ],
    version = 2, // v2: added SosEntity.hopCount for mesh relay hop-count capping (see NearbyMeshManager)
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sosDao(): SosDao
    abstract fun hazardDao(): HazardDao
    abstract fun firstAidDao(): FirstAidDao
    abstract fun familyDao(): FamilyDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "resqlink.db"
    }
}
