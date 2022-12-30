package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsViewModel

@Module(includes = [ViewModelModule::class])
class DAppOptionsModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppOptionsViewModel {
        return ViewModelProvider(fragment, factory).get(DAppOptionsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppOptionsViewModel::class)
    fun provideViewModel(
        dAppOptionsCommunicator: DAppOptionsCommunicator,
        router: DAppRouter,
        payload: DAppOptionsPayload,
        interactor: DappInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return DAppOptionsViewModel(
            payload = payload,
            responder = dAppOptionsCommunicator,
            router = router,
            interactor = interactor,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory
        )
    }
}
