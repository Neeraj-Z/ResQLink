package com.resqlink.app.domain.usecase

import com.resqlink.app.domain.model.FirstAidGuide
import com.resqlink.app.domain.repository.FirstAidRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFirstAidGuidesUseCase @Inject constructor(
    private val repository: FirstAidRepository
) {
    operator fun invoke(query: String = ""): Flow<List<FirstAidGuide>> =
        if (query.isBlank()) repository.observeGuides() else repository.search(query)
}
