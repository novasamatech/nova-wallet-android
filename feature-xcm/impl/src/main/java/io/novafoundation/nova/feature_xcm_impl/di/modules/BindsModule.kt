package io.novafoundation.nova.feature_xcm_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_impl.converter.RealMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_impl.converter.chain.RealChainMultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_impl.dryRun.RealDryRunApi
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
}
