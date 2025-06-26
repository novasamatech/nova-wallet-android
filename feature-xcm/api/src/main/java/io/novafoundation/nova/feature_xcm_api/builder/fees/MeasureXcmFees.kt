package io.novafoundation.nova.feature_xcm_api.builder.fees

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation

/**
 * Measure fees for a given xcm message. Used by [XcmBuilder] when processing [XcmBuilder.payFees]
 * with [PayFeesMode.Measured] specified
 */
interface MeasureXcmFees {

    suspend fun measureFees(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation,
    ): BalanceOf
}
