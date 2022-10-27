package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.GovernanceLocksOverviewViewModel

@Module(includes = [ViewModelModule::class])
class GovernanceLocksOverviewModule {

    @Provides
    @IntoMap
    @ViewModelKey(GovernanceLocksOverviewViewModel::class)
    fun provideViewModel(
        governanceRouter: GovernanceRouter,
    ): ViewModel {
        return GovernanceLocksOverviewViewModel(
            governanceRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): GovernanceLocksOverviewViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(GovernanceLocksOverviewViewModel::class.java)
    }
}
