package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.storage.Preferences

interface NovaCardStateRepository {

    fun isNovaCardStateActive(): Boolean

    fun setNovaCardState(active: Boolean)
}

private const val PREFS_NOVA_CARD_STATE = "PREFS_NOVA_CARD_STATE"

class RealNovaCardStateRepository(
    private val preferences: Preferences
) : NovaCardStateRepository {

    override fun isNovaCardStateActive(): Boolean {
        return preferences.getBoolean(PREFS_NOVA_CARD_STATE, false)
    }

    override fun setNovaCardState(active: Boolean) {
        preferences.putBoolean(PREFS_NOVA_CARD_STATE, active)
    }
}
