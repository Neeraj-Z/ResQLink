package com.resqlink.app.di

import android.content.Context
import androidx.room.Room
import com.resqlink.app.data.local.AppDatabase
import com.resqlink.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAOs as app-scoped singletons. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // .addMigrations(MIGRATION_1_2, ...) as the schema evolves
            //
            // Dev-time safety net: while this project's schema is still changing
            // rapidly, a version bump with no matching Migration would otherwise
            // crash every install with "Room cannot verify the data integrity"
            // (RoomOpenHelper's identity-hash check). fallbackToDestructiveMigration()
            // makes Room just wipe and recreate the DB on a version mismatch instead.
            // Replace this with real Migration objects before any real release --
            // destructively wiping a user's SOS history on an app update is not
            // acceptable in production, only during coursework/dev iteration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSosDao(db: AppDatabase): SosDao = db.sosDao()
    @Provides fun provideHazardDao(db: AppDatabase): HazardDao = db.hazardDao()
    @Provides fun provideFirstAidDao(db: AppDatabase): FirstAidDao = db.firstAidDao()
    @Provides fun provideFamilyDao(db: AppDatabase): FamilyDao = db.familyDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}
