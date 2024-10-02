package io.novafoundation.nova.feature_assets.domain.novaCard

import io.novafoundation.nova.feature_assets.data.repository.NovaCardStateRepository

interface NovaCardInteractor {

    fun isNovaCardStateActive(): Boolean

    fun setNovaCardState(active: Boolean)
}

class RealNovaCardInteractor(
    private val novaCardStateRepository: NovaCardStateRepository
) : NovaCardInteractor {

    override fun isNovaCardStateActive(): Boolean {
        return novaCardStateRepository.isNovaCardStateActive()
    }

    override fun setNovaCardState(active: Boolean) {
        return novaCardStateRepository.setNovaCardState(active)
    }
}
