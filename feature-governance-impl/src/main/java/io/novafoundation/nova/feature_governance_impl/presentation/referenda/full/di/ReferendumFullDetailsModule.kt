package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class ReferendumFullDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumFullDetailsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumFullDetailsPayload,
        identityProviderFactory: GovernanceIdentityProviderFactory,
        addressIconGenerator: AddressIconGenerator,
        governanceSharedState: GovernanceSharedState,
        tokenUseCase: TokenUseCase,
        externalAction: ExternalActions.Presentation,
        copyTextLauncher: CopyTextLauncher.Presentation,
        resourceManager: ResourceManager
    ): ViewModel {
        return ReferendumFullDetailsViewModel(
            router,
            payload,
            identityProviderFactory,
            addressIconGenerator,
            governanceSharedState,
            tokenUseCase,
            externalAction,
            copyTextLauncher,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumFullDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumFullDetailsViewModel::class.java)
    }
}
