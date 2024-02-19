package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct

fun bindDelegatorState(
    dynamicInstance: Any?,
    accountId: AccountId,
    chain: Chain,
    chainAsset: Chain.Asset,
): DelegatorState {
    return when (dynamicInstance) {
        null -> DelegatorState.None(chain, chainAsset)
        is Struct.Instance -> bindDelegator(dynamicInstance, accountId, chain, chainAsset)
        else -> incompatible()
    }
}

private fun bindDelegator(
    struct: Struct.Instance,
    accountId: AccountId,
    chain: Chain,
    chainAsset: Chain.Asset,
): DelegatorState.Delegator {
    return DelegatorState.Delegator(
        accountId = accountId,
        chain = chain,
        chainAsset = chainAsset,
        delegations = bindList(struct["delegations"], ::bindBond),
        total = bindNumber(struct["total"]),
        lessTotal = bindNumber(struct["lessTotal"])
    )
}

fun bindBond(
    instance: Any?,
): DelegatorBond {
    val struct = instance.castToStruct()

    return DelegatorBond(
        owner = bindAccountId(struct["owner"]),
        balance = bindNumber(struct["amount"])
    )
}
