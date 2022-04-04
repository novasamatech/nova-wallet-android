package io.novafoundation.nova.feature_assets.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.BuildConfig
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.feature_assets.data.buyToken.RampProvider
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.AssetFiltersRepository
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.PreferencesAssetFiltersRepository
import io.novafoundation.nova.feature_assets.di.modules.SendModule
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.WalletInteractorImpl
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [SendModule::class])
class AssetsFeatureModule {

    @Provides
    @FeatureScope
    fun provideAssetFiltersRepository(preferences: Preferences): AssetFiltersRepository {
        return PreferencesAssetFiltersRepository(preferences)
    }

    @Provides
    @FeatureScope
    fun provideWalletInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        assetFiltersRepository: AssetFiltersRepository,
        chainRegistry: ChainRegistry,
        nftRepository: NftRepository,
    ): WalletInteractor = WalletInteractorImpl(
        walletRepository,
        accountRepository,
        assetFiltersRepository,
        chainRegistry,
        nftRepository
    )

    @Provides
    @FeatureScope
    fun provideBuyTokenIntegration(): BuyTokenRegistry {
        return BuyTokenRegistry(
            providers = listOf(
                // TODO waiting for secret keys for Ramp and Moonpay
                RampProvider(host = BuildConfig.RAMP_HOST, apiToken = null/*BuildConfig.RAMP_TOKEN*/),
//                MoonPayProvider(host = BuildConfig.MOONPAY_HOST, publicKey = BuildConfig.MOONPAY_PUBLIC_KEY, privateKey = BuildConfig.MOONPAY_PRIVATE_KEY)
            )
        )
    }

    @Provides
    fun provideBuyMixinFactory(
        buyTokenRegistry: BuyTokenRegistry,
        chainRegistry: ChainRegistry,
        accountUseCase: SelectedAccountUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    ): BuyMixinFactory = BuyMixinFactory(
        buyTokenRegistry = buyTokenRegistry,
        chainRegistry = chainRegistry,
        accountUseCase = accountUseCase,
        awaitableMixinFactory = actionAwaitableMixinFactory
    )

    @Provides
    @FeatureScope
    fun provideHistoryFiltersProviderFactory(
        computationalCache: ComputationalCache
    ) = HistoryFiltersProviderFactory(computationalCache)
}
