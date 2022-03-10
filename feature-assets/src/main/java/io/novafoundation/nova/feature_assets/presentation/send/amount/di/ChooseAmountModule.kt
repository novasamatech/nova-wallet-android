package io.novafoundation.nova.feature_assets.presentation.send.amount.di

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
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.send.amount.ChooseAmountViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ChooseAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(ChooseAmountViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        validationExecutor: ValidationExecutor,
        selectedAccountUseCase: SelectedAccountUseCase,
        router: WalletRouter,
        addressModelGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        recipientAddress: String,
        assetPayload: AssetPayload,
        chainRegistry: ChainRegistry,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        addressDisplayUseCase: AddressDisplayUseCase,
        resourceManager: ResourceManager,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
    ): ViewModel {
        return ChooseAmountViewModel(
            interactor = interactor,
            router = router,
            addressIconGenerator = addressModelGenerator,
            externalActions = externalActions,
            recipientAddress = recipientAddress,
            assetPayload = assetPayload,
            chainRegistry = chainRegistry,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            resourceManager = resourceManager,
            amountChooserMixinFactory = amountChooserMixinFactory,
            addressDisplayUseCase = addressDisplayUseCase,
            sendInteractor = sendInteractor,
            validationExecutor = validationExecutor,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ChooseAmountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChooseAmountViewModel::class.java)
    }
}
