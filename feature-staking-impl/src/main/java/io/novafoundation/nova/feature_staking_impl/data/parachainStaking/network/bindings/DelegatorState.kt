package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegationRequest
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorStatus
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.PendingDelegationRequests
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct

fun bindDelegatorState(
    dynamicInstance: Any?,
    accountId: AccountId,
    chain: Chain
): DelegatorState {
    return when (dynamicInstance) {
        null -> DelegatorState.None(accountId, chain)
        is Struct.Instance -> bindDelegator(dynamicInstance, accountId, chain)
        else -> incompatible()
    }
}

private fun bindDelegator(
    struct: Struct.Instance,
    accountId: AccountId,
    chain: Chain
): DelegatorState.Delegator {
    return DelegatorState.Delegator(
        accountId = accountId,
        chain = chain,
        delegations = bindList(struct["delegations"], ::bindBond),
        total = bindNumber(struct["total"]),
        requests = bindPendingDelegationRequests(struct.getTyped("requests")),
        status = bindDelegatorStatus(struct.getTyped("status"))
    )
}

private fun bindBond(
    instance: Any?,
): DelegatorBond {
    val struct = instance.castToStruct()

    return DelegatorBond(
        owner = bindAccountId(struct["owner"]),
        balance = bindNumber(struct["amount"])
    )
}

private fun bindPendingDelegationRequests(
    instance: Struct.Instance
): PendingDelegationRequests {
    return PendingDelegationRequests(
        revocationsCount = bindNumber(instance["revocations_count"]),
        requests = bindList(instance["requests"], ::bindDelegationRequest),
        lessTotal = bindNumber(instance["less_total"])
    )
}

// (collatorId, delegationRequest)
private fun bindDelegationRequest(
    instance: Any?,
): DelegationRequest {
    val (_, delegationRequest) = instance.castToList()
    val delegationRequestStruct = delegationRequest.castToStruct()

    return DelegationRequest(
        collator = bindAccountId(delegationRequestStruct["collator"]),
        amount = bindNumber(delegationRequestStruct["amount"]),
        whenExecutable = bindRoundIndex(delegationRequestStruct["when_executable"]),
        action = bindCollectionEnum(delegationRequestStruct["action"])
    )
}

private fun bindDelegatorStatus(
    instance: DictEnum.Entry<Any?>
): DelegatorStatus {
    return when (instance.name) {
        "Active" -> DelegatorStatus.Active
        "Leaving" -> DelegatorStatus.Leaving(
            roundIndex = bindRoundIndex(instance.value)
        )
        else -> incompatible()
    }
}
