package io.novafoundation.nova.feature_assets.presentation.send.confirm.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.confirm.ConfirmSendViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ConfirmSendModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmSendViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        validationExecutor: ValidationExecutor,
        router: WalletRouter,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressDisplayUseCase: AddressDisplayUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        resourceManager: ResourceManager,
        transferDraft: TransferDraft,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
    ): ViewModel {
        return ConfirmSendViewModel(
            interactor = interactor,
            sendInteractor = sendInteractor,
            router = router,
            addressIconGenerator = addressIconGenerator,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            selectedAccountUseCase = selectedAccountUseCase,
            addressDisplayUseCase = addressDisplayUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            walletUiUseCase = walletUiUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            transferDraft = transferDraft
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmSendViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmSendViewModel::class.java)
    }
}
