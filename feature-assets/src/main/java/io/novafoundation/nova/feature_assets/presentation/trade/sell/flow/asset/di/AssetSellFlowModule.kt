package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
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
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountFormatter

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
        amountFormatter: AmountFormatter
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
            amountFormatter = amountFormatter
        )
    }
}
