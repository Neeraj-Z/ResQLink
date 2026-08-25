package com.resqlink.app.data.repository

import com.resqlink.app.data.local.dao.FirstAidDao
import com.resqlink.app.data.local.entity.FirstAidEntity
import com.resqlink.app.data.remote.firestore.FirstAidFirestoreDataSource
import com.resqlink.app.domain.model.FirstAidGuide
import com.resqlink.app.domain.repository.FirstAidRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** First-aid guides are bundled/cached content: Room is authoritative, Firestore only refreshes it. */
class FirstAidRepositoryImpl @Inject constructor(
    private val dao: FirstAidDao,
    private val remote: FirstAidFirestoreDataSource
) : FirstAidRepository {

    override fun observeGuides(): Flow<List<FirstAidGuide>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<FirstAidGuide>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFromServer(): Result<Unit> = runCatching {
        val guides = remote.fetchGuides()
        dao.upsertAll(guides)
    }
}

private fun FirstAidEntity.toDomain() = FirstAidGuide(
    id = id, title = title, category = category, summary = summary,
    steps = steps, criticalWarning = criticalWarning, imageUrl = imageUrl
)
