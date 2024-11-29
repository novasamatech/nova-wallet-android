package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersion
import java.math.BigInteger

typealias XcmMultiAssets = List<XcmMultiAsset>

sealed class VersionedXcm {

    class V2(val message: XcmMessage) : VersionedXcm()

    class V3(val message: XcmMessage): VersionedXcm()
}

class XcmMessage(val instructions: List<XcmVInstruction>)

sealed class XcmVInstruction {

    class WithdrawAsset(val assets: XcmMultiAssets) : XcmVInstruction()

    class DepositAsset(
        val assets: XcmMultiAssetFilter,
        val maxAssets: BigInteger,
        val beneficiary: MultiLocation
    ) : XcmVInstruction()

    class BuyExecution(val fees: XcmMultiAsset, val weightLimit: WeightLimit) : XcmVInstruction()

    object ClearOrigin : XcmVInstruction()

    class ReserveAssetDeposited(val assets: XcmMultiAssets) : XcmVInstruction()
    class ReceiveTeleportedAsset(val assets: XcmMultiAssets) : XcmVInstruction()

    class DepositReserveAsset(
        val assets: XcmMultiAssetFilter,
        val maxAssets: BigInteger,
        val dest: MultiLocation,
        val xcm: XcmMessage
    ) : XcmVInstruction()
}

sealed class XcmMultiAssetFilter {

    sealed class Wild : XcmMultiAssetFilter() {

        object All : Wild()
    }
}

sealed class VersionedMultiAssets {

    class V1(val assets: XcmMultiAssets) : VersionedMultiAssets()

    class V2(val assets: XcmMultiAssets) : VersionedMultiAssets()

    class V3(val assets: XcmMultiAssets): VersionedMultiAssets()
}

sealed class VersionedMultiAsset {

    class V1(val asset: XcmMultiAsset) : VersionedMultiAsset()

    class V2(val asset: XcmMultiAsset) : VersionedMultiAsset()

    class V3(val asset: XcmMultiAsset): VersionedMultiAsset()
}

sealed class VersionedMultiLocation {

    class V1(val multiLocation: MultiLocation) : VersionedMultiLocation()

    class V2(val multiLocation: MultiLocation) : VersionedMultiLocation()

    class V3(val multiLocation: MultiLocation): VersionedMultiLocation()
}

fun XcmMessage.versioned(lowestAllowedVersion: XcmVersion) = when {
    lowestAllowedVersion <= XcmVersion.V2 -> VersionedXcm.V2(this)
    else -> VersionedXcm.V3(this)
}

fun XcmMultiAssets.versioned(lowestAllowedVersion: XcmVersion) = when {
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiAssets.V1(this)
    lowestAllowedVersion == XcmVersion.V2 -> VersionedMultiAssets.V2(this)
    else -> VersionedMultiAssets.V3(this)
}

fun XcmMultiAsset.versioned(lowestAllowedVersion: XcmVersion) = when {
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiAsset.V1(this)
    lowestAllowedVersion == XcmVersion.V2 -> VersionedMultiAsset.V2(this)
    else -> VersionedMultiAsset.V3(this)
}

fun MultiLocation.versioned(lowestAllowedVersion: XcmVersion) = when {
    lowestAllowedVersion <= XcmVersion.V1 -> VersionedMultiLocation.V1(this)
    lowestAllowedVersion == XcmVersion.V2 -> VersionedMultiLocation.V2(this)
    else -> VersionedMultiLocation.V3(this)
}

class XcmMultiAsset(
    val id: Id,
    val fungibility: Fungibility,
) {

    companion object;

    @Deprecated("Deprecated in favour of MultiAssetId and VersionedMultiAssetId as those cover more use-cases")
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
