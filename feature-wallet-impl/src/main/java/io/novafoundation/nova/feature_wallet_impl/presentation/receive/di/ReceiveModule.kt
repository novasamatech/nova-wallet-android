package io.novafoundation.nova.feature_wallet_impl.presentation.receive.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.receive.ReceiveViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ReceiveModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReceiveViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        qrCodeGenerator: QrCodeGenerator,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        router: WalletRouter,
        chainRegistry: ChainRegistry,
        selectedAccountUseCase: SelectedAccountUseCase,
        payload: AssetPayload,
    ): ViewModel {
        return ReceiveViewModel(
            interactor,
            qrCodeGenerator,
            addressIconGenerator,
            resourceManager,
            externalActions,
            payload,
            chainRegistry,
            selectedAccountUseCase,
            router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ReceiveViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReceiveViewModel::class.java)
    }
}
