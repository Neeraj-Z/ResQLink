package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.HazardZone
import kotlinx.coroutines.flow.Flow

interface HazardRepository {
    fun observeHazards(): Flow<List<HazardZone>>
    suspend fun refreshHazards(lat: Double, lng: Double): Result<Unit>
}
