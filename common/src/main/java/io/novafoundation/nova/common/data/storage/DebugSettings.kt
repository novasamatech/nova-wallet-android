package io.novafoundation.nova.common.data.storage

import io.novafoundation.nova.common.BuildConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class DebugSettings @Inject constructor(private val preferences: Preferences) {

    fun nodeLogsDisabled(): Boolean = BuildConfig.DEBUG && preferences.getBoolean(NODE_LOGS_KEY, false)

    fun setNodeLogsDisabled(disabled: Boolean) {
        check(BuildConfig.DEBUG)
        preferences.putBoolean(NODE_LOGS_KEY, disabled)
    }

    fun bittensorClaimEnabled(): Boolean = BuildConfig.DEBUG && preferences.getBoolean(CLAIM_KEY, false)

    fun observeBittensorClaim(): Flow<Boolean> = preferences.booleanFlow(CLAIM_KEY, false).map { BuildConfig.DEBUG && it }

    fun setBittensorClaimEnabled(enabled: Boolean) {
        check(BuildConfig.DEBUG)
        if (enabled) preferences.putBoolean(CLAIM_BUTTON_KEY, true)
        preferences.putBoolean(CLAIM_KEY, enabled)
    }

    fun bittensorClaimButtonEnabled(): Boolean =
        BuildConfig.DEBUG && (bittensorClaimEnabled() || preferences.getBoolean(CLAIM_BUTTON_KEY, false))

    fun observeBittensorClaimButton(): Flow<Boolean> = combine(
        observeBittensorClaim(),
        preferences.booleanFlow(CLAIM_BUTTON_KEY, false)
    ) { dryRun, button -> BuildConfig.DEBUG && (dryRun || button) }

    fun setBittensorClaimButtonEnabled(enabled: Boolean) {
        check(BuildConfig.DEBUG)
        if (!enabled) preferences.putBoolean(CLAIM_KEY, false)
        preferences.putBoolean(CLAIM_BUTTON_KEY, enabled)
    }

    private companion object {
        const val NODE_LOGS_KEY = "debug.nodeLogs.disabled"
        const val CLAIM_KEY = "debug.bittensor.claim.dryRun"
        const val CLAIM_BUTTON_KEY = "debug.bittensor.claim.alwaysEnabled"
    }
}
