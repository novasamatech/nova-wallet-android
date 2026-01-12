package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegationAction
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

fun bindDelegationAction(
    instance: DictEnum.Entry<Any?>
): DelegationAction {
    return when (instance.name) {
        "Revoke" -> DelegationAction.Revoke(bindNumber(instance.value))
        "Decrease" -> DelegationAction.Decrease(bindNumber(instance.value))
        else -> incompatible()
    }
}
