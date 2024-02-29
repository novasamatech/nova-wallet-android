package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.search.DelegateSearchInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.DelegateCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.search.RealDelegateSearchInteractor
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.DelegatesSharedComputation
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search.DelegateSearchViewModel

@Module(includes = [ViewModelModule::class])
class DelegateSearchModule {

    @Provides
    fun provideDelegateListInteractor(
        delegateCommonRepository: DelegateCommonRepository,
        identityRepository: OnChainIdentityRepository,
        delegatesSharedComputation: DelegatesSharedComputation
    ): DelegateSearchInteractor = RealDelegateSearchInteractor(
        identityRepository = identityRepository,
        delegateCommonRepository = delegateCommonRepository,
        delegatesSharedComputation = delegatesSharedComputation
    )

    @Provides
    @IntoMap
    @ViewModelKey(DelegateSearchViewModel::class)
    fun provideViewModel(
        delegateMappers: DelegateMappers,
        governanceSharedState: GovernanceSharedState,
        interactor: DelegateSearchInteractor,
        resourceManager: ResourceManager,
        router: GovernanceRouter
    ): ViewModel {
        return DelegateSearchViewModel(
            interactor = interactor,
            governanceSharedState = governanceSharedState,
            delegateMappers = delegateMappers,
            resourceManager = resourceManager,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): DelegateSearchViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DelegateSearchViewModel::class.java)
    }
}
