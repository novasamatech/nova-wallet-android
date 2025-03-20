package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

interface NovaCardStateRepository {

    fun getNovaCardCreationState(): NovaCardState

    fun setNovaCardCreationState(state: NovaCardState)

    fun observeNovaCardCreationState(): Flow<NovaCardState>

    fun setLastTopUpTime(time: Long)

    fun getLastTopUpTime(): Long

    suspend fun setTopUpFinishedEvent()

    fun observeTopUpFinishedEvent(): Flow<Unit>
}

private const val PREFS_NOVA_CARD_STATE = "PREFS_NOVA_CARD_STATE"
private const val PREFS_TIME_CARD_BEING_ISSUED = "PREFS_TIME_CARD_BEING_ISSUED"

class RealNovaCardStateRepository(
    private val preferences: Preferences
) : NovaCardStateRepository {

    private val topUpFinishedEvent = MutableSharedFlow<Unit>()

    override fun getNovaCardCreationState(): NovaCardState {
        val novaCardState = preferences.getString(PREFS_NOVA_CARD_STATE, NovaCardState.NONE.toString())
        return NovaCardState.valueOf(novaCardState)
    }

    override fun setNovaCardCreationState(state: NovaCardState) {
        preferences.putString(PREFS_NOVA_CARD_STATE, state.toString())
    }

    override fun observeNovaCardCreationState(): Flow<NovaCardState> {
        return preferences.keyFlow(PREFS_NOVA_CARD_STATE)
            .map { getNovaCardCreationState() }
    }

    override fun setLastTopUpTime(time: Long) {
        preferences.putLong(PREFS_TIME_CARD_BEING_ISSUED, time)
    }

    override fun getLastTopUpTime(): Long {
        return preferences.getLong(PREFS_TIME_CARD_BEING_ISSUED, 0)
    }

    override suspend fun setTopUpFinishedEvent() {
        topUpFinishedEvent.emit(Unit)
    }

    override fun observeTopUpFinishedEvent(): Flow<Unit> {
        return topUpFinishedEvent
    }
}
