package io.novafoundation.nova.feature_assets.domain.novaCard

import io.novafoundation.nova.feature_assets.data.repository.NovaCardStateRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.minutes

interface NovaCardInteractor {

    fun isNovaCardCreated(): Boolean

    fun getNovaCardState(): NovaCardState

    fun setNovaCardState(state: NovaCardState)

    suspend fun setTopUpFinishedEvent()

    fun observeTopUpFinishedEvent(): Flow<Unit>

    fun observeNovaCardState(): Flow<NovaCardState>

    fun setLastTopUpTime(time: Long)

    fun getEstimatedTopUpDuration(): Long
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

    override suspend fun setTopUpFinishedEvent() {
        novaCardStateRepository.setTopUpFinishedEvent()
    }

    override fun observeTopUpFinishedEvent(): Flow<Unit> {
        return novaCardStateRepository.observeTopUpFinishedEvent()
    }

    override fun observeNovaCardState(): Flow<NovaCardState> {
        return novaCardStateRepository.observeNovaCardCreationState()
    }

    override fun setLastTopUpTime(time: Long) {
        novaCardStateRepository.setLastTopUpTime(time)
    }

    override fun getEstimatedTopUpDuration(): Long {
        val lastTopUpTime = novaCardStateRepository.getLastTopUpTime()
        val onTopUpFinishTime = lastTopUpTime + TIMER_MINUTES.minutes.inWholeMilliseconds
        val currentTime = System.currentTimeMillis()
        val estimatedDurationToFinishTopUp = onTopUpFinishTime - currentTime

        return estimatedDurationToFinishTopUp.coerceAtLeast(0)
    }
}
