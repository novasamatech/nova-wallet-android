package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.sign.MetamaskSignInteractorFactory
import io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.sign.PolkadotJsSignInteractorFactory
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignViewModel
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskSendTransactionRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.PolkadotJsSignRequest
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class DAppSignModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        polkadotJsSignInteractorFactory: PolkadotJsSignInteractorFactory,
        metamaskSignInteractorFactory: MetamaskSignInteractorFactory,
        request: DAppSignPayload
    ): DAppSignInteractor = when (val body = request.body) {
        is PolkadotJsSignRequest -> polkadotJsSignInteractorFactory.create(body)
        is MetamaskSendTransactionRequest -> metamaskSignInteractorFactory.create(body)
        else -> throw IllegalArgumentException("Unknown sign request")
    }

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
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        commonInteractor: DappInteractor,
        interactor: DAppSignInteractor,
        payload: DAppSignPayload,
        selectedAccountUseCase: SelectedAccountUseCase,
        communicator: DAppSignCommunicator,
        walletUiUseCase: WalletUiUseCase,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager
    ): ViewModel {
        return DAppSignViewModel(
            router = router,
            selectedAccountUseCase = selectedAccountUseCase,
            interactor = interactor,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            payload = payload,
            commonInteractor = commonInteractor,
            responder = communicator,
            walletUiUseCase = walletUiUseCase,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager
        )
    }
}
