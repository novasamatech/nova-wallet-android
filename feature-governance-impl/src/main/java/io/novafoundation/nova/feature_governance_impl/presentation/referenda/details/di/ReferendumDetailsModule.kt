package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class ReferendumDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumDetailsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumDetailsPayload,
        interactor: ReferendumDetailsInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        selectedAssetSharedState: GovernanceSharedState,
        governanceIdentityProviderFactory: GovernanceIdentityProviderFactory,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        tokenUseCase: TokenUseCase,
        referendumFormatter: ReferendumFormatter,
        externalActions: ExternalActions.Presentation
    ): ViewModel {
        return ReferendumDetailsViewModel(
            router = router,
            payload = payload,
            interactor = interactor,
            selectedAccountUseCase = selectedAccountUseCase,
            selectedAssetSharedState = selectedAssetSharedState,
            governanceIdentityProviderFactory = governanceIdentityProviderFactory,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            tokenUseCase = tokenUseCase,
            referendumFormatter = referendumFormatter,
            externalActions = externalActions
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumDetailsViewModel::class.java)
    }
}
