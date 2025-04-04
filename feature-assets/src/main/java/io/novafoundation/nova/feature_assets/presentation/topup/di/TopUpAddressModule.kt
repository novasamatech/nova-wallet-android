package io.novafoundation.nova.feature_assets.presentation.topup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressCommunicator
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressPayload
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class TopUpAddressModule {

    @Provides
    @IntoMap
    @ViewModelKey(TopUpAddressViewModel::class)
    fun provideViewModel(
        chainRegistry: ChainRegistry,
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        router: AssetsRouter,
        payload: TopUpAddressPayload,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressInputMixinFactory: AddressInputMixinFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        maxActionProviderFactory: MaxActionProviderFactory,
        communicator: TopUpAddressCommunicator,
    ): ViewModel {
        return TopUpAddressViewModel(
            chainRegistry = chainRegistry,
            interactor = interactor,
            sendInteractor = sendInteractor,
            router = router,
            payload = payload,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            selectedAccountUseCase = selectedAccountUseCase,
            addressInputMixinFactory = addressInputMixinFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            maxActionProviderFactory = maxActionProviderFactory,
            responder = communicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): TopUpAddressViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(TopUpAddressViewModel::class.java)
    }
}
