package com.resqlink.app.data.repository

import com.resqlink.app.data.local.dao.HazardDao
import com.resqlink.app.data.local.entity.HazardEntity
import com.resqlink.app.data.remote.firestore.HazardFirestoreDataSource
import com.resqlink.app.domain.model.HazardSeverity
import com.resqlink.app.domain.model.HazardType
import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.repository.HazardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HazardRepositoryImpl @Inject constructor(
    private val dao: HazardDao,
    private val remote: HazardFirestoreDataSource
) : HazardRepository {

    override fun observeHazards(): Flow<List<HazardZone>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Fetches fresh hazards from Firestore and refreshes the local cache;
     * the UI keeps showing the old cache on failure. `lat`/`lng` are accepted
     * for interface compatibility with a future geo-radius query (see the
     * note in HazardFirestoreDataSource) but aren't used by the current
     * full-collection fetch.
     */
    override suspend fun refreshHazards(lat: Double, lng: Double): Result<Unit> = runCatching {
        val remoteHazards = remote.fetchActiveHazards()
        dao.upsertAll(remoteHazards)
        dao.pruneStale(remoteHazards.map { it.id })
    }
}

private fun HazardEntity.toDomain() = HazardZone(
    id = id, type = HazardType.valueOf(type), severity = HazardSeverity.valueOf(severity),
    title = title, description = description, latitude = latitude, longitude = longitude,
    radiusMeters = radiusMeters, issuedAtEpochMillis = issuedAtEpochMillis, source = source
)
