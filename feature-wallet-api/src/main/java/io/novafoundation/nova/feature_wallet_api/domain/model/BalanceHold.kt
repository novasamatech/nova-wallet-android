package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core_db.model.BalanceHoldLocal
import io.novafoundation.nova.core_db.model.BalanceHoldLocal.HoldIdLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold.HoldId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceHold(
    val id: HoldId,
    val amountInPlanks: Balance,
    val chainAsset: Chain.Asset
) : Identifiable {
    
    class HoldId(val module: String, val reason: String)

    // Keep in tact with `BalanceBreakdownIds`
    override val identifier: String = "${id.module}: ${id.reason}"
}

fun mapBalanceHoldFromLocal(
    asset: Chain.Asset,
    hold: BalanceHoldLocal
): BalanceHold {
    return BalanceHold(
        id = hold.id.toDomain(),
        amountInPlanks = hold.amount,
        chainAsset = asset
    )
}

private fun HoldIdLocal.toDomain(): HoldId {
    return HoldId(module, reason)
}
