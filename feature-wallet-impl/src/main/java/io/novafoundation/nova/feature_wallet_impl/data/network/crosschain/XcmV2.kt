package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import java.math.BigInteger

typealias XcmMultiAssets = List<XcmMultiAsset>

sealed class VersionedXcm {

    class V2(val message: XcmV2) : VersionedXcm()
}

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
    class ReceiveTeleportedAsset(val assets: XcmMultiAssets) : XcmV2Instruction()

    class DepositReserveAsset(
        val assets: XcmMultiAssetFilter,
        val maxAssets: BigInteger,
        val dest: MultiLocation,
        val xcm: XcmV2
    ) : XcmV2Instruction()
}

sealed class XcmMultiAssetFilter {

    sealed class Wild : XcmMultiAssetFilter() {

        object All : Wild()
    }
}

sealed class VersionedMultiAssets {

    class V1(val assets: XcmMultiAssets) : VersionedMultiAssets()

    class V2(val assets: XcmMultiAssets) : VersionedMultiAssets()
}

sealed class VersionedMultiAsset {

    class V1(val asset: XcmMultiAsset) : VersionedMultiAsset()

    class V2(val asset: XcmMultiAsset) : VersionedMultiAsset()
}

sealed class VersionedMultiLocation {
    class V1(val multiLocation: MultiLocation) : VersionedMultiLocation()
    class V2(val multiLocation: MultiLocation) : VersionedMultiLocation()
}

fun XcmMultiAssets.versioned(lowestAllowedVersion: XcmVersion?) = when {
    lowestAllowedVersion == null -> VersionedMultiAssets.V2(this) // try out best with latest known version
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiAssets.V1(this)
    else -> VersionedMultiAssets.V2(this)
}

fun XcmMultiAsset.versioned(lowestAllowedVersion: XcmVersion?) = when {
    lowestAllowedVersion == null -> VersionedMultiAsset.V2(this) // try out best with latest known version
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiAsset.V1(this)
    else -> VersionedMultiAsset.V2(this)
}

fun MultiLocation.versioned(lowestAllowedVersion: XcmVersion?) = when {
    lowestAllowedVersion == null -> VersionedMultiLocation.V2(this) // try out best with latest known version
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiLocation.V1(this)
    else -> VersionedMultiLocation.V2(this)
}

class XcmMultiAsset(
    val id: Id,
    val fungibility: Fungibility,
) {

    companion object;

    sealed class Id {

        class Concrete(val multiLocation: MultiLocation) : Id()
    }

    sealed class Fungibility {

        class Fungible(val amount: Balance) : Fungibility()
    }
}

fun XcmMultiAsset.Companion.from(
    multiLocation: MultiLocation,
    amount: Balance
) = XcmMultiAsset(
    id = XcmMultiAsset.Id.Concrete(multiLocation),
    fungibility = XcmMultiAsset.Fungibility.Fungible(amount)
)

sealed class WeightLimit {

    object Unlimited : WeightLimit()

    class Limited(val weight: Weight) : WeightLimit()
}
