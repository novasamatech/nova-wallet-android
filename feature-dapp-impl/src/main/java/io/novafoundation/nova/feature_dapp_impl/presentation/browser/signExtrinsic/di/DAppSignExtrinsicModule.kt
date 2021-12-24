package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DappSignExtrinsicInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicResponder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.GenesisHashUtilityTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class DAppSignExtrinsicModule {

    @Provides
    @ScreenScope
    fun provideTokenUseCase(
        payload: DAppSignExtrinsicPayload,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository
    ): TokenUseCase = GenesisHashUtilityTokenUseCase(payload.signerPayloadJSON.genesisHash, chainRegistry, tokenRepository)

    @Provides
    @ScreenScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        extrinsicService: ExtrinsicService,
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
    ) = DappSignExtrinsicInteractor(extrinsicService, accountRepository, chainRegistry, secretStoreV2)

    @Provides
    @ScreenScope
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppSignExtrinsicViewModel {
        return ViewModelProvider(fragment, factory).get(DAppSignExtrinsicViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppSignExtrinsicViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        addressIconGenerator: AddressIconGenerator,
        dappSignExtrinsicInteractor: DappSignExtrinsicInteractor,
        chainRegistry: ChainRegistry,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        commonInteractor: DappInteractor,
        payload: DAppSignExtrinsicPayload,
        tokenUseCase: TokenUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        communicator: DAppSignExtrinsicCommunicator
    ): ViewModel {
        return DAppSignExtrinsicViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            interactor = dappSignExtrinsicInteractor,
            chainRegistry = chainRegistry,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            payload = payload,
            tokenUseCase = tokenUseCase,
            commonInteractor = commonInteractor,
            responder = communicator
        )
    }
}
