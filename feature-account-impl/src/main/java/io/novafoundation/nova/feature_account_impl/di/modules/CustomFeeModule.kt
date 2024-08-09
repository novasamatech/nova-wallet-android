package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.RealFeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class CustomFeeModule {

    @Provides
    @FeatureScope
    fun provideAssetHubFeePaymentProvider(
        chainRegistry: ChainRegistry,
        multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    ) = AssetHubFeePaymentProvider(
        chainRegistry,
        multiChainRuntimeCallsApi
    )

    @Provides
    @FeatureScope
    fun provideHydrationFeePaymentProvider(
        chainRegistry: ChainRegistry,
        multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    ) = HydrationFeePaymentProvider(
        chainRegistry,
        multiChainRuntimeCallsApi
    )

    @Provides
    @FeatureScope
    fun provideDefaultFeePaymentProvider() = DefaultFeePaymentProvider()

    @Provides
    @FeatureScope
    fun provideWatchOnlySigningPresenter(
        defaultFeePaymentProvider: DefaultFeePaymentProvider,
        assetHubFeePaymentProvider: AssetHubFeePaymentProvider,
        hydrationFeePaymentProvider: HydrationFeePaymentProvider
    ): FeePaymentProviderRegistry = RealFeePaymentProviderRegistry(
        defaultFeePaymentProvider,
        assetHubFeePaymentProvider,
        hydrationFeePaymentProvider
    )
}
