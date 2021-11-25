package io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm.ConfirmTransferViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ConfirmTransferModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmTransferViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        router: WalletRouter,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        transferValidityChecks: TransferValidityChecks.Presentation,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressDisplayUseCase: AddressDisplayUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        resourceManager: ResourceManager,
        transferDraft: TransferDraft,
        chainRegistry: ChainRegistry,
    ): ViewModel {
        return ConfirmTransferViewModel(
            interactor,
            router,
            addressIconGenerator,
            externalActions,
            transferValidityChecks,
            chainRegistry,
            selectedAccountUseCase,
            addressDisplayUseCase,
            resourceManager,
            feeLoaderMixinFactory,
            amountChooserMixinFactory,
            transferDraft
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmTransferViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmTransferViewModel::class.java)
    }
}
