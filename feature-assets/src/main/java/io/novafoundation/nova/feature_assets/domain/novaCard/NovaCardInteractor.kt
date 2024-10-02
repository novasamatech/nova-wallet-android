package io.novafoundation.nova.feature_assets.domain.novaCard

import io.novafoundation.nova.feature_assets.data.repository.NovaCardStateRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.minutes

interface NovaCardInteractor {

    fun isNovaCardStateActive(): Boolean

    fun setNovaCardState(active: Boolean)

    fun observeNovaCardState(): Flow<Boolean>

    fun setTimeCardBeingIssued(time: Long)

    fun getTimeToCardCreation(): Long
}

const val TIMER_MINUTES = 5

class RealNovaCardInteractor(
    private val novaCardStateRepository: NovaCardStateRepository
) : NovaCardInteractor {

    override fun isNovaCardStateActive(): Boolean {
        return novaCardStateRepository.isNovaCardStateActive()
    }

    override fun setNovaCardState(active: Boolean) {
        return novaCardStateRepository.setNovaCardState(active)
    }

    override fun observeNovaCardState(): Flow<Boolean> {
        return novaCardStateRepository.observeNovaCardState()
    }

    override fun setTimeCardBeingIssued(time: Long) {
        novaCardStateRepository.setTimeCardBeingIssued(time)
    }

    override fun getTimeToCardCreation(): Long {
        val cardBeingIssuedTime = novaCardStateRepository.getTimeCardBeingIssued()
        val cardCreationTime = cardBeingIssuedTime + TIMER_MINUTES.minutes.inWholeMilliseconds
        val currentTime = System.currentTimeMillis()
        val millisecondsToCardCreation = cardCreationTime - currentTime

        return millisecondsToCardCreation.coerceAtLeast(0)
    }
}
