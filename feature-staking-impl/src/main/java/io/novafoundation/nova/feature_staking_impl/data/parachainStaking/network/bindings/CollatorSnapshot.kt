package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class CollatorSnapshot(
    val bond: Balance,
    val delegations: List<DelegatorBond>,
    val total: Balance,
)

fun bindCollatorSnapshot(instance: Any?): CollatorSnapshot {
    val asStruct = instance.castToStruct()

    return CollatorSnapshot(
        bond = bindNumber(asStruct["bond"]),
        delegations = bindList(asStruct["delegations"], ::bindBond),
        total = bindNumber(asStruct["total"])
    )
}
