package io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.ChooseAmountViewModel
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.impl.PhishingWarningProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ChooseAmountModule {

    @Provides
    fun providePhishingAddressMixin(interactor: WalletInteractor): PhishingWarningMixin {
        return PhishingWarningProvider(interactor)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ChooseAmountViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        router: WalletRouter,
        addressModelGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        transferValidityChecks: TransferValidityChecks.Presentation,
        recipientAddress: String,
        assetPayload: AssetPayload,
        chainRegistry: ChainRegistry,
        phishingWarning: PhishingWarningMixin,
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
            transferValidityChecks = transferValidityChecks,
            recipientAddress = recipientAddress,
            assetPayload = assetPayload,
            chainRegistry = chainRegistry,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            resourceManager = resourceManager,
            phishingAddress = phishingWarning,
            amountChooserMixinFactory = amountChooserMixinFactory,
            addressDisplayUseCase = addressDisplayUseCase
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
