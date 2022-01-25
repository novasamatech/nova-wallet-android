package io.novafoundation.nova.feature_dapp_impl.presentation.search.di

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
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

@Module(includes = [ViewModelModule::class])
class DAppSearchModule {

    @Provides
    @ScreenScope
    fun provideInteractor(dAppMetadataRepository: DAppMetadataRepository) = SearchDappInteractor(dAppMetadataRepository)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppSearchViewModel {
        return ViewModelProvider(fragment, factory).get(DAppSearchViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppSearchViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        resourceManager: ResourceManager,
        interactor: SearchDappInteractor,
        searchResponder: DAppSearchCommunicator,
        payload: SearchPayload
    ): ViewModel {
        return DAppSearchViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            dAppSearchResponder = searchResponder,
            payload = payload
        )
    }
}
