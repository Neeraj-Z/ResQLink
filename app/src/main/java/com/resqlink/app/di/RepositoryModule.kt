package com.resqlink.app.di

import com.resqlink.app.data.location.FusedLocationRepository
import com.resqlink.app.data.mesh.NearbyMeshManager
import com.resqlink.app.data.repository.*
import com.resqlink.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces (domain layer) to their concrete implementations (data layer). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindSosRepository(impl: SosRepositoryImpl): SosRepository

    @Binds @Singleton
    abstract fun bindHazardRepository(impl: HazardRepositoryImpl): HazardRepository

    @Binds @Singleton
    abstract fun bindFirstAidRepository(impl: FirstAidRepositoryImpl): FirstAidRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindMeshRepository(impl: NearbyMeshManager): MeshRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: FusedLocationRepository): LocationRepository
}
