package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class DelegatedStakingDelegation(
    val amount: Balance
)

fun bindDelegatedStakingDelegation(decoded: Any): DelegatedStakingDelegation {
    val asStruct = decoded.castToStruct()

    return DelegatedStakingDelegation(
        amount = bindNumber(asStruct["amount"])
    )
}
