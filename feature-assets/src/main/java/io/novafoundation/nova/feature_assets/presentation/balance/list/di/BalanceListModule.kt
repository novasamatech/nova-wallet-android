package io.novafoundation.nova.feature_assets.presentation.balance.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdownInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetListMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.ExpandableAssetsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.list.BalanceListViewModel
import io.novafoundation.nova.feature_assets.presentation.novacard.common.NovaCardRestrictionCheckMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable.MaskableAmountFormatterProvider
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase

@Module(includes = [ViewModelModule::class])
class BalanceListModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        nftRepository: NftRepository,
        assetsViewModeRepository: AssetsViewModeRepository
    ) = AssetsListInteractor(accountRepository, nftRepository, assetsViewModeRepository)

    @Provides
    @ScreenScope
    fun provideBalanceBreakdownInteractor(
        accountRepository: AccountRepository,
        balanceLocksRepository: BalanceLocksRepository,
        balanceHoldsRepository: BalanceHoldsRepository
    ): BalanceBreakdownInteractor {
        return BalanceBreakdownInteractor(
            accountRepository,
            balanceLocksRepository,
            balanceHoldsRepository
        )
    }

    @Provides
    @ScreenScope
    fun provideAssetListMixinFactory(
        walletInteractor: WalletInteractor,
        assetsListInteractor: AssetsListInteractor,
        externalBalancesInteractor: ExternalBalancesInteractor,
        expandableAssetsMixinFactory: ExpandableAssetsMixinFactory
    ): AssetListMixinFactory {
        return AssetListMixinFactory(
            walletInteractor,
            assetsListInteractor,
            externalBalancesInteractor,
            expandableAssetsMixinFactory
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BalanceListViewModel::class)
    fun provideViewModel(
        promotionBannersMixinFactory: PromotionBannersMixinFactory,
        bannerSourceFactory: BannersSourceFactory,
        walletInteractor: WalletInteractor,
        assetsListInteractor: AssetsListInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        router: AssetsRouter,
        currencyInteractor: CurrencyInteractor,
        balanceBreakdownInteractor: BalanceBreakdownInteractor,
        resourceManager: ResourceManager,
        walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
        swapAvailabilityInteractor: SwapAvailabilityInteractor,
        assetListMixinFactory: AssetListMixinFactory,
        amountFormatter: AmountFormatter,
        buySellSelectorMixinFactory: BuySellSelectorMixinFactory,
        multisigPendingOperationsService: MultisigPendingOperationsService,
        novaCardRestrictionCheckMixin: NovaCardRestrictionCheckMixin,
        maskableAmountFormatterProvider: MaskableAmountFormatterProvider
    ): ViewModel {
        return BalanceListViewModel(
            promotionBannersMixinFactory = promotionBannersMixinFactory,
            bannerSourceFactory = bannerSourceFactory,
            walletInteractor = walletInteractor,
            assetsListInteractor = assetsListInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            router = router,
            currencyInteractor = currencyInteractor,
            balanceBreakdownInteractor = balanceBreakdownInteractor,
            resourceManager = resourceManager,
            walletConnectSessionsUseCase = walletConnectSessionsUseCase,
            swapAvailabilityInteractor = swapAvailabilityInteractor,
            assetListMixinFactory = assetListMixinFactory,
            amountFormatter = amountFormatter,
            maskableAmountFormatterProvider = maskableAmountFormatterProvider,
            buySellSelectorMixinFactory = buySellSelectorMixinFactory,
            multisigPendingOperationsService = multisigPendingOperationsService,
            novaCardRestrictionCheckMixin = novaCardRestrictionCheckMixin
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): BalanceListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(BalanceListViewModel::class.java)
    }
}
