package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.model.DiscreetMode
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset.AssetSellFlowViewModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable.MaskableAmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable.MaskableAmountFormatterFactory

@Module(includes = [ViewModelModule::class])
class AssetSellFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSellFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSellFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSellFlowViewModel::class)
    fun provideViewModel(
        interactorFactory: AssetSearchInteractorFactory,
        router: AssetsRouter,
        externalBalancesInteractor: ExternalBalancesInteractor,
        currencyInteractor: CurrencyInteractor,
        controllableAssetCheck: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        assetIconProvider: AssetIconProvider,
        assetViewModeInteractor: AssetViewModeInteractor,
        amountFormatterFactory: MaskableAmountFormatterFactory
    ): ViewModel {
        return AssetSellFlowViewModel(
            interactorFactory = interactorFactory,
            router = router,
            externalBalancesInteractor = externalBalancesInteractor,
            currencyInteractor = currencyInteractor,
            controllableAssetCheck = controllableAssetCheck,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            assetIconProvider = assetIconProvider,
            assetViewModeInteractor = assetViewModeInteractor,
            amountFormatter = amountFormatterFactory.create(DiscreetMode.DISABLED) // For assets flow DiscreetMode is always disabled
        )
    }
}
