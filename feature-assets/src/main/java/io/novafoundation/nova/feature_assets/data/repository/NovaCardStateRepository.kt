package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NovaCardStateRepository {

    fun isNovaCardStateActive(): Boolean

    fun setNovaCardState(active: Boolean)

    fun observeNovaCardState(): Flow<Boolean>

    fun setTimeCardBeingIssued(time: Long)

    fun getTimeCardBeingIssued(): Long
}

private const val PREFS_NOVA_CARD_STATE = "PREFS_NOVA_CARD_STATE"
private const val PREFS_TIME_CARD_BEING_ISSUED = "PREFS_TIME_CARD_BEING_ISSUED"

class RealNovaCardStateRepository(
    private val preferences: Preferences
) : NovaCardStateRepository {

    override fun isNovaCardStateActive(): Boolean {
        return preferences.getBoolean(PREFS_NOVA_CARD_STATE, false)
    }

    override fun setNovaCardState(active: Boolean) {
        preferences.putBoolean(PREFS_NOVA_CARD_STATE, active)
    }

    override fun observeNovaCardState(): Flow<Boolean> {
        return preferences.keyFlow(PREFS_NOVA_CARD_STATE)
            .map { preferences.getBoolean(PREFS_NOVA_CARD_STATE, false) }
    }

    override fun setTimeCardBeingIssued(time: Long) {
        preferences.putLong(PREFS_TIME_CARD_BEING_ISSUED, time)
    }

    override fun getTimeCardBeingIssued(): Long {
        return preferences.getLong(PREFS_TIME_CARD_BEING_ISSUED, 0)
    }
}
