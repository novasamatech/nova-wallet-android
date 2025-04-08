package io.novafoundation.nova.feature_xcm_impl.builder

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import javax.inject.Inject

@FeatureScope
internal class RealXcmBuilderFactory @Inject constructor() : XcmBuilder.Factory {

    override fun create(
        initial: ChainLocation,
        xcmVersion: XcmVersion,
        measureXcmFees: MeasureXcmFees
    ): XcmBuilder {
        return RealXcmBuilder(initial, xcmVersion, measureXcmFees)
    }
}
