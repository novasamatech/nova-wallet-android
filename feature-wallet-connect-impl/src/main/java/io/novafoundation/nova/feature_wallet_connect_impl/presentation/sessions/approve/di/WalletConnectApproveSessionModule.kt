package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.ApproveSessionCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.WalletConnectApproveSessionViewModel

@Module(includes = [ViewModelModule::class])
class WalletConnectApproveSessionModule {

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectApproveSessionViewModel::class)
    fun provideViewModel(
        router: WalletConnectRouter,
        interactor: WalletConnectSessionInteractor,
        communicator: ApproveSessionCommunicator,
        resourceManager: ResourceManager,
        selectWalletMixinFactory: SelectWalletMixin.Factory
    ): ViewModel {
        return WalletConnectApproveSessionViewModel(
            router = router,
            interactor = interactor,
            responder = communicator,
            resourceManager = resourceManager,
            selectWalletMixinFactory = selectWalletMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectApproveSessionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectApproveSessionViewModel::class.java)
    }
}
