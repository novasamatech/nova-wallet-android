package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_api.domain.model.SessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionIndex
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class MythDelegation(
    val session: SessionIndex,
    val stake: Balance
)

fun bindDelegationInfo(decoded: Any?): MythDelegation {
    val asStruct = decoded.castToStruct()

    return MythDelegation(
        session = bindSessionIndex(asStruct["session"]),
        stake = bindNumber(asStruct["stake"])
    )
}
