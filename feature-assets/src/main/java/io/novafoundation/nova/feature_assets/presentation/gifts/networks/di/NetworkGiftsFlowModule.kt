package io.novafoundation.nova.feature_assets.presentation.gifts.networks.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.gifts.networks.NetworkGiftsFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.send.flow.network.NetworkSendFlowViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class NetworkGiftsFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NetworkGiftsFlowViewModel {
        return ViewModelProvider(fragment, factory).get(NetworkGiftsFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NetworkGiftsFlowViewModel::class)
    fun provideViewModel(
        interactor: AssetNetworksInteractor,
        router: AssetsRouter,
        externalBalancesInteractor: ExternalBalancesInteractor,
        controllableAssetCheck: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        networkFlowPayload: NetworkFlowPayload,
        chainRegistry: ChainRegistry,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return NetworkGiftsFlowViewModel(
            interactor = interactor,
            router = router,
            externalBalancesInteractor = externalBalancesInteractor,
            controllableAssetCheck = controllableAssetCheck,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            networkFlowPayload = networkFlowPayload,
            chainRegistry = chainRegistry,
            amountFormatter = amountFormatter
        )
    }
}
