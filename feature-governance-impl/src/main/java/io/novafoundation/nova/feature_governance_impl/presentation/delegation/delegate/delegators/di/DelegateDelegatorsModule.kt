package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter

@Module(includes = [ViewModelModule::class])
class DelegateDelegatorsModule {

    @Provides
    @IntoMap
    @ViewModelKey(DelegateDelegatorsViewModel::class)
    fun provideViewModel(
        payload: DelegateDelegatorsPayload,
        router: GovernanceRouter,
        governanceSharedState: GovernanceSharedState,
        externalActions: ExternalActions.Presentation,
        interactor: DelegateDelegatorsInteractor,
        votersFormatter: VotersFormatter,
        delegateMappers: DelegateMappers,
    ): ViewModel {
        return DelegateDelegatorsViewModel(
            payload = payload,
            router = router,
            governanceSharedState = governanceSharedState,
            externalActions = externalActions,
            interactor = interactor,
            votersFormatter = votersFormatter,
            delegateMappers = delegateMappers
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): DelegateDelegatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DelegateDelegatorsViewModel::class.java)
    }
}
