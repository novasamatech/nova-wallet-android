package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import java.math.BigDecimal
import java.math.BigInteger

class Collator(
    val accountIdHex: String,
    val address: String,
    val identity: OnChainIdentity?,
    val snapshot: CollatorSnapshot?,
    val candidateMetadata: CandidateMetadata,
    val minimumStakeToGetRewards: BigInteger,
    val apr: BigDecimal?,
) : Identifiable {

    override val identifier: String = accountIdHex
}

val Collator.isElected
    get() = snapshot != null

fun CollatorSnapshot.minimumStake(
    systemForcedMinStake: BigInteger,
    maxRewardableDelegatorsPerCollator: BigInteger
): BigInteger {
    if (delegations.size < maxRewardableDelegatorsPerCollator.toInt()) {
        return systemForcedMinStake
    }

    val minStakeToGetRewards = delegations.minOfOrNull { it.balance } ?: systemForcedMinStake

    return minStakeToGetRewards.coerceAtLeast(systemForcedMinStake)
}

fun Collator.estimatedAprReturns(amount: BigDecimal): PeriodReturns {
    return PeriodReturns(
        gainAmount = amount * apr.orZero(),
        gainFraction = apr.orZero(),
    )
}

fun Collator.accountId() = accountIdHex.fromHex()
