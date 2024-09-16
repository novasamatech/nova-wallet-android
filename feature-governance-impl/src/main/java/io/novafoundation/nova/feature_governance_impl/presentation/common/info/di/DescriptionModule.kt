package io.novafoundation.nova.feature_governance_impl.presentation.common.description.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.modules.shared.MarkdownFullModule
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter

@Module(includes = [ViewModelModule::class, MarkdownFullModule::class])
class ReferendumInfoModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumInfoViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumInfoPayload,
        interactor: ReferendumDetailsInteractor,
        selectedAssetSharedState: GovernanceSharedState,
        referendumFormatter: ReferendumFormatter,
        resourceManager: ResourceManager,
        governanceIdentityProviderFactory: GovernanceIdentityProviderFactory,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        markwon: Markwon
    ): ViewModel {
        return ReferendumInfoViewModel(
            router = router,
            payload = payload,
            interactor = interactor,
            selectedAssetSharedState = selectedAssetSharedState,
            referendumFormatter = referendumFormatter,
            resourceManager = resourceManager,
            governanceIdentityProviderFactory = governanceIdentityProviderFactory,
            addressIconGenerator = addressIconGenerator,
            externalActions = externalActions,
            markwon = markwon
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumInfoViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumInfoViewModel::class.java)
    }
}
