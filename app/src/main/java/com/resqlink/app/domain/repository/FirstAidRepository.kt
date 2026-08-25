package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.FirstAidGuide
import kotlinx.coroutines.flow.Flow

interface FirstAidRepository {
    /** Always served from the local cache -- first aid content must work with zero connectivity. */
    fun observeGuides(): Flow<List<FirstAidGuide>>
    fun search(query: String): Flow<List<FirstAidGuide>>
    suspend fun refreshFromServer(): Result<Unit>
}
