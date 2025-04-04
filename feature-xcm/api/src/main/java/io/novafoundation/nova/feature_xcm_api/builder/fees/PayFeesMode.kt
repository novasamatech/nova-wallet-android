package io.novafoundation.nova.feature_xcm_api.builder.fees

import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation

/**
 * Specifies how [XcmBuilder] should specify fees for PayFees instruction
 */
sealed class PayFeesMode {

    /**
     * Fees should be measured when building the xcm by calling provided [MeasureXcmFees] implementation
     */
    class Measured(val feeAssetId: AssetLocation) : PayFeesMode()

    /**
     * Should use exactly [fee] when specifying fees
     */
    class Exact(val fee: MultiAsset) : PayFeesMode()
}
