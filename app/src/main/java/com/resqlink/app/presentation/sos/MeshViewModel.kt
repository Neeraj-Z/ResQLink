package com.resqlink.app.presentation.sos

import androidx.lifecycle.ViewModel
import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.domain.repository.MeshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin read-only wrapper around [MeshRepository] for the SOS screen's status
 * display. Start/stop calls are NOT made from here -- [MeshRepository] is a
 * `@Singleton`, and its actual lifecycle is owned by
 * [com.resqlink.app.service.SosForegroundService] so relay survives the
 * person leaving this screen (see that class's doc comment). This ViewModel
 * exists purely so the Composable can observe [relayState] without reaching
 * into the repository singleton directly.
 */
@HiltViewModel
class MeshViewModel @Inject constructor(
    meshRepository: MeshRepository
) : ViewModel() {
    val relayState: StateFlow<MeshRelayState> = meshRepository.relayState
}
