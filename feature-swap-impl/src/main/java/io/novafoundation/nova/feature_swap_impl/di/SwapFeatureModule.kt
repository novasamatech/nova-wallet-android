package io.novafoundation.nova.feature_swap_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.swap.RealSwapService
import io.novafoundation.nova.feature_swap_impl.presentation.state.RealSwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_swap_impl.presentation.state.RealSwapSettingsState
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class SwapFeatureModule {

    @Provides
    @FeatureScope
    fun provideAssetConversionExchangeFactory(
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        runtimeCallsApi: MultiChainRuntimeCallsApi,
        extrinsicService: ExtrinsicService,
        assetSourceRegistry: AssetSourceRegistry,
    ): AssetConversionExchangeFactory {
        return AssetConversionExchangeFactory(
            chainRegistry = chainRegistry,
            remoteStorageSource = remoteStorageSource,
            runtimeCallsApi = runtimeCallsApi,
            extrinsicService = extrinsicService,
            assetSourceRegistry = assetSourceRegistry,
        )
    }

    @FeatureScope
    @Provides
    fun provideSwapService(
        assetConversionExchangeFactory: AssetConversionExchangeFactory,
        computationalCache: ComputationalCache,
        chainRegistry: ChainRegistry
    ): SwapService {
        return RealSwapService(assetConversionExchangeFactory, computationalCache, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideSwapSettingsSharedState(): RealSwapSettingsState {
        return RealSwapSettingsState()
    }

    @Provides
    @FeatureScope
    fun provideSwapInteractor(
        swapService: SwapService,
        swapSettingsState: RealSwapSettingsState,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository
    ): SwapInteractor {
        return SwapInteractor(
            swapService,
            swapSettingsState,
            walletRepository,
            accountRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideSwapSettingsStoreProvider(
        computationalCache: ComputationalCache,
        chainRegistry: ChainRegistry
    ): SwapSettingsStateProvider {
        return RealSwapSettingsStateProvider(computationalCache, chainRegistry)
    }
}
