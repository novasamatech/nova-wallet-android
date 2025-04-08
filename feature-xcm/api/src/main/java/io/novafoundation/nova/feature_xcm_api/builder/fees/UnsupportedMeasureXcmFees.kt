package io.novafoundation.nova.feature_xcm_api.builder.fees

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation

class UnsupportedMeasureXcmFees : MeasureXcmFees {
    override suspend fun measureFees(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation
    ): BalanceOf {
        error("Measurement not supported")
    }
}
