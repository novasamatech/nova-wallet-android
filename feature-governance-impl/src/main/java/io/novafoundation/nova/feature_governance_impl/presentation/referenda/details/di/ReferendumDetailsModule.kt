package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsViewModel

@Module(includes = [ViewModelModule::class])
class ReferendumDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumDetailsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumDetailsPayload
    ): ViewModel {
        return ReferendumDetailsViewModel(router, payload)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumDetailsViewModel::class.java)
    }
}
