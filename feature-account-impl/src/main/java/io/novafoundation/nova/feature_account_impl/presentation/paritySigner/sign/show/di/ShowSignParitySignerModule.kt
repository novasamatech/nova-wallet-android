package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.RealShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ShowSignParitySignerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(): ShowSignParitySignerInteractor = RealShowSignParitySignerInteractor()

    @Provides
    @IntoMap
    @ViewModelKey(ShowSignParitySignerViewModel::class)
    fun provideViewModel(
        interactor: ShowSignParitySignerInteractor,
        signSharedState: SharedState<SeparateFlowSignerState>,
        qrCodeGenerator: QrCodeGenerator,
        communicator: PolkadotVaultVariantSignCommunicator,
        payload: ShowSignParitySignerPayload,
        chainRegistry: ChainRegistry,
        addressIconGenerator: AddressIconGenerator,
        addressDisplayUseCase: AddressDisplayUseCase,
        router: AccountRouter,
        externalActions: ExternalActions.Presentation,
        qrCodeExpiredPresentableFactory: QrCodeExpiredPresentableFactory,
        extrinsicValidityUseCase: ExtrinsicValidityUseCase,
        resourceManager: ResourceManager,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    ): ViewModel {
        return ShowSignParitySignerViewModel(
            router = router,
            interactor = interactor,
            signSharedState = signSharedState,
            qrCodeGenerator = qrCodeGenerator,
            responder = communicator,
            payload = payload,
            chainRegistry = chainRegistry,
            addressIconGenerator = addressIconGenerator,
            addressDisplayUseCase = addressDisplayUseCase,
            externalActions = externalActions,
            polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
            qrCodeExpiredPresentableFactory = qrCodeExpiredPresentableFactory,
            extrinsicValidityUseCase = extrinsicValidityUseCase,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ShowSignParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ShowSignParitySignerViewModel::class.java)
    }
}
