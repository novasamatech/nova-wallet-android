package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class VotedReferendaModule {

    @Provides
    @IntoMap
    @ViewModelKey(VotedReferendaViewModel::class)
    fun provideViewModel(
        interactor: ReferendaListInteractor,
        governanceSharedState: GovernanceSharedState,
        selectedTokenUseCase: TokenUseCase,
        governanceRouter: GovernanceRouter,
        referendumFormatter: ReferendumFormatter,
        payload: VotedReferendaPayload,
        resourceManager: ResourceManager
    ): ViewModel {
        return VotedReferendaViewModel(
            interactor = interactor,
            governanceSharedState = governanceSharedState,
            selectedTokenUseCase = selectedTokenUseCase,
            governanceRouter = governanceRouter,
            referendumFormatter = referendumFormatter,
            resourceManager = resourceManager,
            payload = payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): VotedReferendaViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(VotedReferendaViewModel::class.java)
    }
}
