package io.novafoundation.nova.feature_xcm_api.di

import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.config.XcmConfigRepository
import io.novafoundation.nova.feature_xcm_api.converter.LocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector

interface XcmFeatureApi {

    val locationConverterFactory: LocationConverterFactory

    val xcmVersionDetector: XcmVersionDetector

    val dryRunApi: DryRunApi

    val xcmPaymentApi: XcmPaymentApi

    val xcmBuilderFactory: XcmBuilder.Factory

    val xcmConfigRepository: XcmConfigRepository
}
