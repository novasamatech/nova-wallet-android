package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated.YourDelegationsViewModel

@Module(includes = [ViewModelModule::class])
class YourDelegationsModule {

    @Provides
    @IntoMap
    @ViewModelKey(YourDelegationsViewModel::class)
    fun provideViewModel(
        delegateMappers: DelegateMappers,
        governanceSharedState: GovernanceSharedState,
        interactor: DelegateListInteractor,
        router: GovernanceRouter,
    ): ViewModel {
        return YourDelegationsViewModel(
            interactor = interactor,
            governanceSharedState = governanceSharedState,
            delegateMappers = delegateMappers,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): YourDelegationsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(YourDelegationsViewModel::class.java)
    }
}
