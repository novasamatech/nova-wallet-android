package io.novafoundation.nova.feature_push_notifications.domain.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushSettings(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val subscribedMetaAccounts: Set<Long>,
    val stakingReward: ChainFeature,
    val governance: Map<ChainId, GovernanceState>,
    val multisigs: MultisigsState
) {

    data class GovernanceState(
        val newReferendaEnabled: Boolean,
        val referendumUpdateEnabled: Boolean,
        val govMyDelegateVotedEnabled: Boolean,
        val tracks: Set<TrackId>
    )

    data class MultisigsState(
        val isEnabled: Boolean, // General notifications state. Other states may be enabled to save state when general one is disabled
        val isInitiatingEnabled: Boolean,
        val isApprovingEnabled: Boolean,
        val isExecutionEnabled: Boolean,
        val isRejectionEnabled: Boolean
    ) {
        companion object {
            fun disabled() = MultisigsState(
                isEnabled = false,
                isInitiatingEnabled = false,
                isApprovingEnabled = false,
                isExecutionEnabled = false,
                isRejectionEnabled = false
            )

            fun enabled() = MultisigsState(
                isEnabled = true,
                isInitiatingEnabled = true,
                isApprovingEnabled = true,
                isExecutionEnabled = true,
                isRejectionEnabled = true
            )
        }
    }

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<ChainId>) : ChainFeature()
    }

    fun settingsIsEmpty(): Boolean {
        return !announcementsEnabled &&
            !sentTokensEnabled &&
            !receivedTokensEnabled &&
            stakingReward.isEmpty() &&
            !isGovEnabled()
    }
}

fun PushSettings.ChainFeature.isEmpty(): Boolean {
    return when (this) {
        is PushSettings.ChainFeature.All -> false
        is PushSettings.ChainFeature.Concrete -> chainIds.isEmpty()
    }
}

fun PushSettings.ChainFeature.isNotEmpty(): Boolean {
    return !isEmpty()
}

fun PushSettings.isGovEnabled(): Boolean {
    return governance.values.any {
        (it.newReferendaEnabled || it.referendumUpdateEnabled || it.govMyDelegateVotedEnabled) && it.tracks.isNotEmpty()
    }
}

fun PushSettings.MultisigsState.isAllTypesDisabled(): Boolean {
    return !isInitiatingEnabled && !isApprovingEnabled && !isExecutionEnabled && !isRejectionEnabled
}

fun PushSettings.MultisigsState.disableIfAllTypesDisabled(): PushSettings.MultisigsState {
    return if (isAllTypesDisabled()) copy(isEnabled = false) else this
}

fun PushSettings.MultisigsState.isInitiatingEnabledTotal() = isInitiatingEnabled && isEnabled
fun PushSettings.MultisigsState.isApprovingEnabledTotal() = isApprovingEnabled && isEnabled
fun PushSettings.MultisigsState.isExecutionEnabledTotal() = isExecutionEnabled && isEnabled
fun PushSettings.MultisigsState.isRejectionEnabledTotal() = isRejectionEnabled && isEnabled
