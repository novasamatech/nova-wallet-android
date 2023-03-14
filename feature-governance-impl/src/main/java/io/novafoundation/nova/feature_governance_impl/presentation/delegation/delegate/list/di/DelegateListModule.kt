package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.DelegateListViewModel

@Module(includes = [ViewModelModule::class])
class DelegateListModule {

    @Provides
    @IntoMap
    @ViewModelKey(DelegateListViewModel::class)
    fun provideViewModel(
        delegateMappers: DelegateMappers,
        governanceSharedState: GovernanceSharedState,
        interactor: DelegateListInteractor,
        listChooserMixinFactory: ListChooserMixin.Factory,
        resourceManager: ResourceManager,
        router: GovernanceRouter
    ): ViewModel {
        return DelegateListViewModel(
            interactor = interactor,
            governanceSharedState = governanceSharedState,
            delegateMappers = delegateMappers,
            listChooserMixinFactory = listChooserMixinFactory,
            resourceManager = resourceManager,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): DelegateListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DelegateListViewModel::class.java)
    }
}
