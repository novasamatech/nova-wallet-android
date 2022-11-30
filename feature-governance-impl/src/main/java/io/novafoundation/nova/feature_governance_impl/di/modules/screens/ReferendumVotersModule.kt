package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.voters.RealReferendumVotersInteractor

@Module
class ReferendumVotersModule {

    @Provides
    @FeatureScope
    fun provideReferendaVotersInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        @OnChainIdentity identityProvider: IdentityProvider,
        governanceSharedState: GovernanceSharedState,
    ): ReferendumVotersInteractor = RealReferendumVotersInteractor(
        governanceSourceRegistry = governanceSourceRegistry,
        identityProvider = identityProvider,
        governanceSharedState = governanceSharedState,
    )
}
