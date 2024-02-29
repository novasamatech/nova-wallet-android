package io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters.ReferendaFiltersViewModel

@Module(includes = [ViewModelModule::class])
class ReferendaFiltersModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendaFiltersViewModel::class)
    fun provideViewModel(
        referendaFiltersInteractor: ReferendaFiltersInteractor,
        governanceRouter: GovernanceRouter
    ): ViewModel {
        return ReferendaFiltersViewModel(referendaFiltersInteractor, governanceRouter)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendaFiltersViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendaFiltersViewModel::class.java)
    }
}
