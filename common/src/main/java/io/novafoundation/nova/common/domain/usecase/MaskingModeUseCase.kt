package io.novafoundation.nova.common.domain.usecase

import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.data.repository.toggle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface MaskingModeUseCase {

    fun observeMaskingMode(): Flow<MaskingMode>

    fun toggleMaskingMode()

    fun toggleHideBalancesOnLaunch()

    fun observeHideBalancesOnLaunchEnabled(): Flow<Boolean>
}

private const val MASKING_MODE_FEATURE = "MASKING_MODE_FEATURE"
private const val MASKING_MODE_ON_LAUNCH_FEATURE = "MASKING_MODE_ON_LAUNCH_FEATURE"

class RealMaskingModeUseCase(
    private val toggleFeatureRepository: ToggleFeatureRepository
) : MaskingModeUseCase {

    init {
        toggleFeatureRepository.set(MASKING_MODE_FEATURE, initialMaskingModeState())
    }

    override fun observeMaskingMode(): Flow<MaskingMode> {
        return observeMaskingModeEnabled()
            .map { toMaskingMode(it) }
            .distinctUntilChanged()
    }

    override fun toggleMaskingMode() {
        toggleFeatureRepository.toggle(MASKING_MODE_FEATURE)
    }

    override fun toggleHideBalancesOnLaunch() {
        val isHideOnLaunchEnabled = toggleFeatureRepository.toggle(MASKING_MODE_ON_LAUNCH_FEATURE)
        toggleFeatureRepository.set(MASKING_MODE_FEATURE, isHideOnLaunchEnabled)
    }

    override fun observeHideBalancesOnLaunchEnabled(): Flow<Boolean> {
        return toggleFeatureRepository.observe(MASKING_MODE_ON_LAUNCH_FEATURE, false)
    }

    private fun observeMaskingModeEnabled() = toggleFeatureRepository.observe(MASKING_MODE_FEATURE)

    private fun initialMaskingModeState(): Boolean {
        return toggleFeatureRepository.get(MASKING_MODE_ON_LAUNCH_FEATURE, false) ||
            toggleFeatureRepository.get(MASKING_MODE_FEATURE, false)
    }
}

private fun toMaskingMode(enabled: Boolean): MaskingMode = when (enabled) {
    true -> MaskingMode.ENABLED
    false -> MaskingMode.DISABLED
}
