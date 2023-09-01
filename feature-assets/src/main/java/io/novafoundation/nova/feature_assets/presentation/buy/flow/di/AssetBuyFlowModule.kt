package io.novafoundation.nova.feature_assets.presentation.buy.flow.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.buy.flow.AssetBuyFlowViewModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor

@Module(includes = [ViewModelModule::class])
class AssetBuyFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetBuyFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetBuyFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetBuyFlowViewModel::class)
    fun provideViewModel(
        interactor: AssetSearchInteractor,
        router: AssetsRouter,
        externalBalancesInteractor: ExternalBalancesInteractor,
        currencyInteractor: CurrencyInteractor,
        controllableAssetCheck: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase,
        buyMixinFactory: BuyMixinFactory
    ): ViewModel {
        return AssetBuyFlowViewModel(
            interactor = interactor,
            router = router,
            externalBalancesInteractor = externalBalancesInteractor,
            currencyInteractor = currencyInteractor,
            controllableAssetCheck = controllableAssetCheck,
            accountUseCase = accountUseCase,
            buyMixinFactory = buyMixinFactory
        )
    }
}
