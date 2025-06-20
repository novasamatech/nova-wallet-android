package io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ExternalSignInteractor
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm.EvmSignInteractorFactory
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.PolkadotSignInteractorFactory
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.ExternalSignViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class])
class ExternalSignModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        polkadotSignInteractorFactory: PolkadotSignInteractorFactory,
        metamaskSignInteractorFactory: EvmSignInteractorFactory,
        payload: ExternalSignPayload
    ): ExternalSignInteractor = when (val request = payload.signRequest) {
        is ExternalSignRequest.Polkadot -> polkadotSignInteractorFactory.create(request, payload.wallet)
        is ExternalSignRequest.Evm -> metamaskSignInteractorFactory.create(request, payload.wallet)
    }

    @Provides
    @ScreenScope
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ExternalSignViewModel {
        return ViewModelProvider(fragment, factory).get(ExternalSignViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ExternalSignViewModel::class)
    fun provideViewModel(
        router: ExternalSignRouter,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        interactor: ExternalSignInteractor,
        payload: ExternalSignPayload,
        communicator: ExternalSignCommunicator,
        walletUiUseCase: WalletUiUseCase,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        actionAwaitableMixin: ActionAwaitableMixin.Factory
    ): ViewModel {
        return ExternalSignViewModel(
            router = router,
            interactor = interactor,
            feeLoaderMixinV2Factory = feeLoaderMixinFactory,
            payload = payload,
            responder = communicator,
            walletUiUseCase = walletUiUseCase,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            actionAwaitableMixinFactory = actionAwaitableMixin
        )
    }
}
