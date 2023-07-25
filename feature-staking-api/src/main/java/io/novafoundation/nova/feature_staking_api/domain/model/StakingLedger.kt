package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

class StakingLedger(
    val stashId: AccountId,
    val total: BigInteger,
    val active: BigInteger,
    val unlocking: List<UnlockChunk>,
    val claimedRewards: List<BigInteger>
)

class UnlockChunk(val amount: BigInteger, val era: BigInteger): EraRedeemable {
    override val redeemEra: EraIndex = era
}

fun StakingLedger.sumStaking(
    condition: (chunk: UnlockChunk) -> Boolean
): BigInteger {
    return unlocking
        .filter { condition(it) }
        .sumByBigInteger(UnlockChunk::amount)
}

fun StakingLedger?.activeBalance(): Balance {
    return this?.active.orZero()
}
