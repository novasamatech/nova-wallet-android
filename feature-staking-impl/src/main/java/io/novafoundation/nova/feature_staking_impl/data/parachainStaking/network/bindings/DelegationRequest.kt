package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegationAction
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

fun bindDelegationRequests(instance: Any?, collatorId: AccountId) = instance?.let {
    bindList(instance) { listElement -> bindDelegationRequest(collatorId, listElement) }
}.orEmpty()

private fun bindDelegationRequest(
    collatorId: AccountId,
    instance: Any?,
): ScheduledDelegationRequest {
    val delegationRequestStruct = instance.castToStruct()

    return ScheduledDelegationRequest(
        delegator = bindAccountId(delegationRequestStruct["delegator"]),
        whenExecutable = bindRoundIndex(delegationRequestStruct["whenExecutable"]),
        action = bindDelegationAction(delegationRequestStruct.getTyped("action")),
        collator = collatorId
    )
}

fun bindDelegationAction(
    instance: DictEnum.Entry<Any?>
): DelegationAction {
    return when (instance.name) {
        "Revoke" -> DelegationAction.Revoke(bindNumber(instance.value))
        "Decrease" -> DelegationAction.Decrease(bindNumber(instance.value))
        else -> incompatible()
    }
}
