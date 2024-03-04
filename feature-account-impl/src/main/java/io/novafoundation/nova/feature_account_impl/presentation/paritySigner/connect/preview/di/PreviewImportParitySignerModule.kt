package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview.PreviewImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview.RealPreviewImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.PreviewImportParitySignerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PreviewImportParitySignerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(chainRegistry: ChainRegistry): PreviewImportParitySignerInteractor {
        return RealPreviewImportParitySignerInteractor(chainRegistry)
    }

    @Provides
    @IntoMap
    @ViewModelKey(PreviewImportParitySignerViewModel::class)
    fun provideViewModel(
        interactor: PreviewImportParitySignerInteractor,
        accountRouter: AccountRouter,
        @Caching iconGenerator: AddressIconGenerator,
        payload: ParitySignerAccountPayload,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager
    ): ViewModel {
        return PreviewImportParitySignerViewModel(
            interactor = interactor,
            accountRouter = accountRouter,
            iconGenerator = iconGenerator,
            payload = payload,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PreviewImportParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PreviewImportParitySignerViewModel::class.java)
    }
}
