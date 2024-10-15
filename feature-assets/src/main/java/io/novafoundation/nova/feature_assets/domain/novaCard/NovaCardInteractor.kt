package io.novafoundation.nova.feature_assets.domain.novaCard

import io.novafoundation.nova.feature_assets.data.repository.NovaCardStateRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.minutes

enum class NovaCardState {
    NONE,
    CREATION,
    CREATED
}

interface NovaCardInteractor {

    fun isNovaCardCreated(): Boolean

    fun getNovaCardState(): NovaCardState

    fun setNovaCardState(state: NovaCardState)

    fun observeNovaCardState(): Flow<NovaCardState>

    fun setTimeCardBeingIssued(time: Long)

    fun getTimeToCardCreation(): Long
}

const val TIMER_MINUTES = 5

class RealNovaCardInteractor(
    private val novaCardStateRepository: NovaCardStateRepository
) : NovaCardInteractor {

    override fun isNovaCardCreated(): Boolean {
        return novaCardStateRepository.getNovaCardCreationState() == NovaCardState.CREATED
    }

    override fun getNovaCardState(): NovaCardState {
        return novaCardStateRepository.getNovaCardCreationState()
    }

    override fun setNovaCardState(state: NovaCardState) {
        return novaCardStateRepository.setNovaCardCreationState(state)
    }

    override fun observeNovaCardState(): Flow<NovaCardState> {
        return novaCardStateRepository.observeNovaCardCreationState()
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
