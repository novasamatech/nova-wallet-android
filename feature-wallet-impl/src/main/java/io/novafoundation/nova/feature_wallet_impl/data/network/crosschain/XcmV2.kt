package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import java.math.BigInteger

typealias XcmMultiAssets = List<XcmMultiAsset>

class XcmV2(val instructions: List<XcmV2Instruction>)

sealed class XcmV2Instruction {

    class WithdrawAsset(val assets: XcmMultiAssets) : XcmV2Instruction()

    class DepositAsset(
        val assets: XcmMultiAssetFilter,
        val maxAssets: BigInteger,
        val beneficiary: MultiLocation
    ) : XcmV2Instruction()

    class BuyExecution(val fees: XcmMultiAsset, val weightLimit: WeightLimit) : XcmV2Instruction()

    object ClearOrigin : XcmV2Instruction()

    class ReserveAssetDeposited(val assets: XcmMultiAssets) : XcmV2Instruction()
}

sealed class XcmMultiAssetFilter {

    sealed class Wild : XcmMultiAssetFilter() {

        object All : Wild()
    }
}

class XcmMultiAsset(
    val id: Id,
    val fungibility: Fungibility,
) {

    sealed class Id {

        class Concrete(val multiLocation: MultiLocation) : Id()
    }

    sealed class Fungibility {

        class Fungible(val amount: Balance) : Fungibility()
    }
}

sealed class WeightLimit {

    object Unlimited : WeightLimit()

    class Limited(val weight: Weight) : WeightLimit()
}
