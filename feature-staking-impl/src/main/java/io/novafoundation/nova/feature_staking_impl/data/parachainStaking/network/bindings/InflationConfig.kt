package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbill
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class InflationInfo(
    // Staking expectations
    val expect: Range<Balance>,

    // Annual inflation range
    val annual: Range<Perbill>,

    // Round inflation range
    val round: Range<Perbill>,
)

fun bindInflationInfo(dynamic: Any?): InflationInfo {
    val asStruct = dynamic.castToStruct()

    return InflationInfo(
        expect = bindRange(asStruct["expect"], ::bindNumber),
        annual = bindRange(asStruct["annual"], ::bindPerbill),
        round = bindRange(asStruct["round"], ::bindPerbill)
    )
}
