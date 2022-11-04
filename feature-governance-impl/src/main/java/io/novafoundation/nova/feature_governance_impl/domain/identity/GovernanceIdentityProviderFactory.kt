package io.novafoundation.nova.feature_governance_impl.domain.identity

import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.oneOf
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import kotlinx.coroutines.flow.Flow

class GovernanceIdentityProviderFactory(
    private val localProvider: IdentityProvider,
    private val onChainProvider: IdentityProvider,
) {

    fun proposerProvider(
        proposerFlow: Flow<ReferendumProposer?>
    ): IdentityProvider {
        val referendumProposerProvider = ReferendumProposerIdentityProvider(proposerFlow)

        return IdentityProvider.oneOf(onChainProvider, referendumProposerProvider, localProvider)
    }

    fun defaultProvider(): IdentityProvider {
        return IdentityProvider.oneOf(onChainProvider, localProvider)
    }
}
