package io.novafoundation.nova.feature_xcm_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_impl.builder.RealXcmBuilderFactory
import io.novafoundation.nova.feature_xcm_impl.converter.RealMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_impl.converter.chain.RealChainMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_impl.runtimeApi.dryRun.RealDryRunApi
import io.novafoundation.nova.feature_xcm_impl.runtimeApi.xcmPayment.RealXcmPaymentApi
import io.novafoundation.nova.feature_xcm_impl.versions.detector.RealXcmVersionDetector

@Module
interface BindsModule {

    @Binds
    fun bindXcmVersionDetector(real: RealXcmVersionDetector): XcmVersionDetector

    @Binds
    fun bindChainMultiLocationConverterFactory(real: RealChainMultiLocationConverterFactory): ChainMultiLocationConverterFactory

    @Binds
    fun bindAssetMultiLocationConverterFactory(real: RealMultiLocationConverterFactory): MultiLocationConverterFactory

    @Binds
    fun bindDryRunApi(real: RealDryRunApi): DryRunApi

    @Binds
    fun bindXcmPaymentApi(real: RealXcmPaymentApi): XcmPaymentApi

    @Binds
    fun bindXcmBuilderFactory(real: RealXcmBuilderFactory): XcmBuilder.Factory
}
