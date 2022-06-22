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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectSendModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectSendViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        validationExecutor: ValidationExecutor,
        selectedAccountUseCase: SelectedAccountUseCase,
        router: WalletRouter,
        chainRegistry: ChainRegistry,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        resourceManager: ResourceManager,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        addressInputMixinFactory: AddressInputMixinFactory,
        assetPayload: AssetPayload,
        recipientAddress: String?,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        ): ViewModel {
        return SelectSendViewModel(
            interactor = interactor,
            router = router,
            assetPayload = assetPayload,
            chainRegistry = chainRegistry,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            resourceManager = resourceManager,
            amountChooserMixinFactory = amountChooserMixinFactory,
            sendInteractor = sendInteractor,
            validationExecutor = validationExecutor,
            selectedAccountUseCase = selectedAccountUseCase,
            addressInputMixinFactory = addressInputMixinFactory,
            initialRecipientAddress = recipientAddress,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory
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
