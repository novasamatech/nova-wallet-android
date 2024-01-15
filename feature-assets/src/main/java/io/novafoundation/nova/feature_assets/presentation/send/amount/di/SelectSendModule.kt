package io.novafoundation.nova.feature_assets.presentation.send.amount.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendViewModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectSendModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectSendViewModel::class)
    fun provideViewModel(
        chainRegistry: ChainRegistry,
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        router: AssetsRouter,
        payload: SendPayload,
        initialRecipientAddress: String?,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        crossChainTransfersUseCase: CrossChainTransfersUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressInputMixinFactory: AddressInputMixinFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        accountRepository: AccountRepository,
        selectAddressMixinFactory: SelectAddressMixin.Factory
    ): ViewModel {
        return SelectSendViewModel(
            chainRegistry = chainRegistry,
            interactor = interactor,
            sendInteractor = sendInteractor,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            router = router,
            payload = payload,
            initialRecipientAddress = initialRecipientAddress,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            externalActions = externalActions,
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            selectedAccountUseCase = selectedAccountUseCase,
            addressInputMixinFactory = addressInputMixinFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            accountRepository = accountRepository,
            selectAddressMixinFactory = selectAddressMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SelectSendViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectSendViewModel::class.java)
    }
}
