package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlin.time.Duration

interface GovernanceVoteAssistant {

    class LockEstimation(
        val amount: Balance,
        val duration: Duration
    )

    sealed class Change<T>(val previousValue: T, val newValue: T) {

        class Changed<T>(previousValue: T, newValue: T, val absoluteDifference: T, val positive: Boolean) : Change<T>(previousValue, newValue)

        class Same<T>(value: T) : Change<T>(previousValue = value, newValue = value)
    }

    class LocksChange(
        val amountChange: Change<Balance>,
        val periodChange: Change<Duration>
    )

    val onChainReferendum: OnChainReferendum

    val trackVoting: Voting?

    suspend fun estimateLocksAfterVoting(amount: Balance, conviction: Conviction): LocksChange
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

fun <T> GovernanceVoteAssistant.Change<T>.changedOrNull(): GovernanceVoteAssistant.Change.Changed<T>? = castOrNull()
