package io.novafoundation.nova.feature_assets.presentation.send.flow.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.send.flow.AssetSendFlowViewModel
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor

@Module(includes = [ViewModelModule::class])
class AssetSendFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSendFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSendFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSendFlowViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactor: AssetSearchInteractor,
        currencyInteractor: CurrencyInteractor,
        contributionsInteractor: ContributionsInteractor,
        controllableAssetCheckMixin: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase
    ): ViewModel {
        return AssetSendFlowViewModel(
            router = router,
            interactor = interactor,
            currencyInteractor = currencyInteractor,
            contributionsInteractor = contributionsInteractor,
            controllableAssetCheck = controllableAssetCheckMixin,
            accountUseCase = accountUseCase
        )
    }
}
