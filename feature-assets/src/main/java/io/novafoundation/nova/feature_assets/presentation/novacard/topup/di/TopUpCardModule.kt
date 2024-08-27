package io.novafoundation.nova.feature_assets.presentation.novacard.topup.di

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
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class TopUpCardModule {

    @Provides
    @IntoMap
    @ViewModelKey(TopUpCardViewModel::class)
    fun provideViewModel(
        chainRegistry: ChainRegistry,
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        router: AssetsRouter,
        payload: TopUpCardPayload,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressInputMixinFactory: AddressInputMixinFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory
    ): ViewModel {
        return TopUpCardViewModel(
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
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): TopUpCardViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(TopUpCardViewModel::class.java)
    }
}
