package io.novafoundation.nova.feature_xcm_api.di

import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector

interface XcmFeatureApi {

    val assetMultiLocationConverterFactory: MultiLocationConverterFactory

    val chainMultiLocationConverterFactory: ChainMultiLocationConverterFactory

    val xcmVersionDetector: XcmVersionDetector

    val dryRunApi: DryRunApi
}
