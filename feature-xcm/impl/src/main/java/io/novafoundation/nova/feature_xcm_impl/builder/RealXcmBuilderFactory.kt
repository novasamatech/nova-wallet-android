package io.novafoundation.nova.feature_xcm_impl.builder

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.XcmAssetIssuer
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_impl.builder.fees.DryRunMeasuresXcmFees
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class RealXcmBuilderFactory @Inject constructor(
    private val dryRunApi: DryRunApi,
    private val xcmPaymentApi: XcmPaymentApi,
    private val chainRegistry: ChainRegistry,
) : XcmBuilder.Factory {

    override fun create(
        initial: ChainLocation,
        xcmVersion: XcmVersion,
        measureXcmFees: MeasureXcmFees
    ): XcmBuilder {
        return RealXcmBuilder(initial, xcmVersion, measureXcmFees)
    }

    override fun dryRunMeasureFees(assetIssuer: XcmAssetIssuer): MeasureXcmFees {
        return DryRunMeasuresXcmFees(
            dryRunApi = dryRunApi,
            xcmPaymentApi = xcmPaymentApi,
            chainRegistry = chainRegistry,
            assetIssuer = assetIssuer
        )
    }
}
