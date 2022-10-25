package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlin.time.Duration

interface GovernanceVoteAssistant {

    sealed class Change<T>(val previousValue: T, val newValue: T) {

        class Changed<T>(previousValue: T, newValue: T, val absoluteDifference: T, val positive: Boolean) : Change<T>(previousValue, newValue)

        class Same<T>(value: T) : Change<T>(previousValue = value, newValue = value)
    }

    class LocksChange(
        val lockedAmountChange: Change<Balance>,
        val lockedPeriodChange: Change<Duration>,
        val transferableChange: Change<Balance>
    )

    val onChainReferendum: OnChainReferendum

    val track: ReferendumTrack?

    val trackVoting: Voting?

    suspend fun estimateLocksAfterVoting(amount: Balance, conviction: Conviction, asset: Asset): LocksChange
}

fun <T : Comparable<T>> Change(
    previousValue: T,
    newValue: T,
    absoluteDifference: T
): GovernanceVoteAssistant.Change<T> {
    return if (previousValue == newValue) {
        GovernanceVoteAssistant.Change.Same(newValue)
    } else {
        GovernanceVoteAssistant.Change.Changed(
            previousValue = previousValue,
            newValue = newValue,
            absoluteDifference = absoluteDifference,
            positive = newValue > previousValue
        )
    }
}

fun GovernanceVoteAssistant.Change<*>.isReduced(): Boolean {
    return this is GovernanceVoteAssistant.Change.Changed && !positive
}

fun GovernanceVoteAssistant.Change<*>.isIncreased(): Boolean {
    return this is GovernanceVoteAssistant.Change.Changed && positive
}
