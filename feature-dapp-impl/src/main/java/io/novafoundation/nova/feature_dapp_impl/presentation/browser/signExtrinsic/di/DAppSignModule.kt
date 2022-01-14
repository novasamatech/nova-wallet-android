package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
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
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignViewModel
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.maybeSignExtrinsic
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.GenesisHashUtilityTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class DAppSignModule {

    @Provides
    @ScreenScope
    fun provideTokenUseCase(
        payload: DAppSignPayload,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository
    ): TokenUseCase? = payload.signerPayload.maybeSignExtrinsic()?.let {
        GenesisHashUtilityTokenUseCase(it.genesisHash, chainRegistry, tokenRepository)
    }

    @Provides
    @ScreenScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        extrinsicService: ExtrinsicService,
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
        @ExtrinsicSerialization extrinsicGson: Gson
    ) = DappSignExtrinsicInteractor(extrinsicService, accountRepository, chainRegistry, secretStoreV2, extrinsicGson)

    @Provides
    @ScreenScope
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppSignViewModel {
        return ViewModelProvider(fragment, factory).get(DAppSignViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppSignViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        addressIconGenerator: AddressIconGenerator,
        dappSignExtrinsicInteractor: DappSignExtrinsicInteractor,
        chainRegistry: ChainRegistry,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        commonInteractor: DappInteractor,
        payload: DAppSignPayload,
        tokenUseCase: TokenUseCase?,
        selectedAccountUseCase: SelectedAccountUseCase,
        communicator: DAppSignCommunicator
    ): ViewModel {
        return DAppSignViewModel(
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

