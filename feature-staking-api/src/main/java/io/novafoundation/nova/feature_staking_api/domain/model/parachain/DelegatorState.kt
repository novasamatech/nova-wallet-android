package io.novafoundation.nova.feature_staking_api.domain.model.parachain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

sealed class DelegatorState(
    val accountId: AccountId,
    val chain: Chain,
) {

    class Delegator(
        accountId: AccountId,
        chain: Chain,
        val delegations: List<DelegatorBond>,
        val total: Balance,
        val requests: PendingDelegationRequests,
        val status: DelegatorStatus,
    ) : DelegatorState(accountId, chain)

    class None(accountId: AccountId, chain: Chain) : DelegatorState(accountId, chain)
}

sealed class DelegatorStatus {

    object Active : DelegatorStatus()

    class Leaving(val roundIndex: RoundIndex) : DelegatorStatus()
}

class DelegatorBond(
    val owner: AccountId,
    val balance: Balance,
)

class PendingDelegationRequests(
    val revocationsCount: BigInteger,
    val requests: List<DelegationRequest>,
    val lessTotal: Balance,
)

class DelegationRequest(
    val collator: AccountId,
    val amount: Balance,
    val whenExecutable: RoundIndex,
    val action: DelegationChange
)

enum class DelegationChange {
    Revoke, Decrease
}

typealias RoundIndex = BigInteger
