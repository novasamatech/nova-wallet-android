package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlin.time.Duration

interface GovernanceVoteAssistant {

    class LocksChange(
        val lockedAmountChange: Change<Balance>,
        val lockedPeriodChange: Change<Duration>,
        val transferableChange: Change<Balance>
    )

    class ReusableLock(val type: Type, val amount: Balance) {
        enum class Type {
            GOVERNANCE, ALL
        }
    }

    val onChainReferendum: OnChainReferendum

    val trackVoting: Voting?

    suspend fun estimateLocksAfterVoting(amount: Balance, conviction: Conviction, asset: Asset): LocksChange

    suspend fun reusableLocks(): List<ReusableLock>
}

fun <T : Comparable<T>> Change(
    previousValue: T,
    newValue: T,
    absoluteDifference: T
): Change<T> {
    return if (previousValue == newValue) {
        Change.Same(newValue)
    } else {
        Change.Changed(
            previousValue = previousValue,
            newValue = newValue,
            absoluteDifference = absoluteDifference,
            positive = newValue > previousValue
        )
    }
}
