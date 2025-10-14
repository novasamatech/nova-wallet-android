package io.novafoundation.nova.feature_xcm_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.config.XcmConfigRepository
import io.novafoundation.nova.feature_xcm_api.converter.LocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_impl.builder.RealXcmBuilderFactory
import io.novafoundation.nova.feature_xcm_impl.config.RealXcmConfigRepository
import io.novafoundation.nova.feature_xcm_impl.converter.asset.RealLocationConverterFactory
import io.novafoundation.nova.feature_xcm_impl.runtimeApi.dryRun.RealDryRunApi
import io.novafoundation.nova.feature_xcm_impl.runtimeApi.xcmPayment.RealXcmPaymentApi
import io.novafoundation.nova.feature_xcm_impl.versions.detector.RealXcmVersionDetector

@Module
internal interface BindsModule {

    @Binds
    fun bindXcmVersionDetector(real: RealXcmVersionDetector): XcmVersionDetector

    @Binds
    fun bindDryRunApi(real: RealDryRunApi): DryRunApi

    @Binds
    fun bindXcmPaymentApi(real: RealXcmPaymentApi): XcmPaymentApi

    @Binds
    fun bindXcmBuilderFactory(real: RealXcmBuilderFactory): XcmBuilder.Factory

    @Binds
    fun bindXcmConfigRepository(real: RealXcmConfigRepository): XcmConfigRepository

    @Binds
    fun bindLocationConverterFactory(real: RealLocationConverterFactory): LocationConverterFactory
}
