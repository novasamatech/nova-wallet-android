package io.novafoundation.nova.feature_staking_api.domain.model.parachain

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal
import java.math.BigInteger

sealed class DelegatorState(
    val chain: Chain,
    val chainAsset: Chain.Asset,
) {

    class Delegator(
        val accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        val delegations: List<DelegatorBond>,
        val total: Balance,
        val lessTotal: Balance,
    ) : DelegatorState(chain, chainAsset)

    class None(chain: Chain, chainAsset: Chain.Asset) : DelegatorState(chain, chainAsset)
}

fun DelegatorState.stakeablePlanks(freeBalance: BigInteger): BigInteger {
    return (freeBalance - totalBonded).coerceAtLeast(BigInteger.ZERO)
}

fun DelegatorState.stakeableAmount(freeBalance: BigInteger): BigDecimal {
    return chainAsset.amountFromPlanks(stakeablePlanks(freeBalance))
}

private val DelegatorState.totalBonded: BigInteger
    get() = asDelegator()?.total.orZero()

val DelegatorState.activeBonded: Balance
    get() = when (this) {
        is DelegatorState.Delegator -> total - lessTotal
        is DelegatorState.None -> Balance.ZERO
    }

val DelegatorState.delegationsCount
    get() = when (this) {
        is DelegatorState.Delegator -> delegations.size
        is DelegatorState.None -> 0
    }

fun DelegatorState.Delegator.delegatedCollatorIds() = delegations.map { it.owner }
fun DelegatorState.Delegator.delegatedCollatorIdsHex() = delegations.map { it.owner.toHexString() }

fun DelegatorState.delegationAmountTo(collatorId: AccountId): BalanceOf? {
    return castOrNull<DelegatorState.Delegator>()?.delegations?.find { it.owner.contentEquals(collatorId) }?.balance
}

fun DelegatorState.hasDelegation(collatorId: AccountId): Boolean = this is DelegatorState.Delegator && delegations.any { it.owner.contentEquals(collatorId) }

fun DelegatorState.asDelegator(): DelegatorState.Delegator? = castOrNull<DelegatorState.Delegator>()

class DelegatorBond(
    val owner: AccountId,
    val balance: BalanceOf,
)

class ScheduledDelegationRequest(
    val collator: AccountId,
    val delegator: AccountId,
    val whenExecutable: RoundIndex,
    val action: DelegationAction
)

fun ScheduledDelegationRequest.redeemableIn(roundIndex: RoundIndex): Boolean = whenExecutable <= roundIndex
fun ScheduledDelegationRequest.unbondingIn(roundIndex: RoundIndex): Boolean = whenExecutable > roundIndex

sealed class DelegationAction(val amount: BalanceOf) {
    class Revoke(amount: Balance) : DelegationAction(amount)

    class Decrease(amount: Balance) : DelegationAction(amount)
}

typealias RoundIndex = BigInteger

fun DelegatorState.Delegator.address() = chain.addressOf(accountId)
