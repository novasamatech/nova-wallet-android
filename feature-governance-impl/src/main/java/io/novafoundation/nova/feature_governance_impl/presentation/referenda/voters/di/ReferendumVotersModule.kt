package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersViewModel

@Module(includes = [ViewModelModule::class])
class ReferendumVotersModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumVotersViewModel::class)
    fun provideViewModel(
        payload: ReferendumVotersPayload,
        router: GovernanceRouter,
        governanceSharedState: GovernanceSharedState,
        externalAction: ExternalActions.Presentation,
        addressIconGenerator: AddressIconGenerator
    ): ViewModel {
        return ReferendumVotersViewModel(
            payload,
            router,
            governanceSharedState,
            externalAction,
            addressIconGenerator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumVotersViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumVotersViewModel::class.java)
    }
}
