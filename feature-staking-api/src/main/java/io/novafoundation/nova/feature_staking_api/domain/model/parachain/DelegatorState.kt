package io.novafoundation.nova.feature_staking_api.domain.model.parachain

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

sealed class DelegatorState(
    val chain: Chain,
) {

    class Delegator(
        val accountId: AccountId,
        chain: Chain,
        val delegations: List<DelegatorBond>,
        val total: Balance,
        val status: DelegatorStatus,
    ) : DelegatorState(chain)

    class None(chain: Chain) : DelegatorState(chain)
}

val DelegatorState.delegationsCount
    get() = when (this) {
        is DelegatorState.Delegator -> delegations.size
        is DelegatorState.None -> 0
    }

fun DelegatorState.delegationAmountTo(collatorId: AccountId): BalanceOf? {
    return castOrNull<DelegatorState.Delegator>()?.delegations?.find { it.owner.contentEquals(collatorId) }?.balance
}

fun DelegatorState.hasDelegation(collatorId: AccountId): Boolean = this is DelegatorState.Delegator && delegations.any { it.owner.contentEquals(collatorId) }

sealed class DelegatorStatus {

    object Active : DelegatorStatus()

    class Leaving(val roundIndex: RoundIndex) : DelegatorStatus()
}

class DelegatorBond(
    val owner: AccountId,
    val balance: Balance,
)

class ScheduledDelegationRequest(
    val delegator: AccountId,
    val whenExecutable: RoundIndex,
    val action: DelegationAction
)

sealed class DelegationAction(val amount: Balance) {
    class Revoke(amount: Balance) : DelegationAction(amount)

    class Decrease(amount: Balance) : DelegationAction(amount)
}

typealias RoundIndex = BigInteger

fun DelegatorState.Delegator.address() = chain.addressOf(accountId)
