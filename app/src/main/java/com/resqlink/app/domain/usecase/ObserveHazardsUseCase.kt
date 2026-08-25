package com.resqlink.app.domain.usecase

import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.repository.HazardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHazardsUseCase @Inject constructor(
    private val repository: HazardRepository
) {
    operator fun invoke(): Flow<List<HazardZone>> = repository.observeHazards()
}
