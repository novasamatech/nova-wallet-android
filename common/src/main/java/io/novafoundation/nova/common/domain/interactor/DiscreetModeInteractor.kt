package io.novafoundation.nova.common.domain.interactor

import io.novafoundation.nova.common.data.model.DiscreetMode
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.data.repository.toggle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface DiscreetModeInteractor {
    fun observeDiscreetMode(): Flow<DiscreetMode>

    fun toggleMaskingMode()
}

private const val DISCREET_MODE_FEATURE = "DISCREET_MODE_FEATURE"

class RealDiscreetModeInteractor(
    private val toggleFeatureRepository: ToggleFeatureRepository
) : DiscreetModeInteractor {

    override fun observeDiscreetMode(): Flow<DiscreetMode> {
        return observeDiscreetModeEnabled()
            .map { toDiscreetMode(it) }
            .distinctUntilChanged()
    }

    override fun toggleMaskingMode() {
        toggleFeatureRepository.toggle(DISCREET_MODE_FEATURE)
    }

    private fun observeDiscreetModeEnabled() = toggleFeatureRepository.observe(DISCREET_MODE_FEATURE, false)
}

private fun toDiscreetMode(enabled: Boolean): DiscreetMode = when (enabled) {
    true -> DiscreetMode.ENABLED
    false -> DiscreetMode.DISABLED
}
