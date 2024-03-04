package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.label

import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegateMetadataOrNull
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabel
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealDelegateLabelUseCase(
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val identityRepository: OnChainIdentityRepository,
) : DelegateLabelUseCase {

    override suspend fun getDelegateLabel(delegate: AccountId): DelegateLabel = withContext(Dispatchers.Default) {
        val option = governanceSharedState.selectedOption()
        val chain = option.assetWithChain.chain
        val delegationsRepository = governanceSourceRegistry.sourceFor(option).delegationsRepository

        val metadata = delegationsRepository.getDelegateMetadataOrNull(chain, delegate)
        val identity = identityRepository.getIdentityFromId(chain.id, delegate)

        DelegateLabel(
            accountId = delegate,
            onChainIdentity = identity,
            metadata = metadata?.let {
                DelegateLabel.Metadata(
                    name = metadata.name,
                    iconUrl = metadata.profileImageUrl,
                    accountType = mapAccountTypeToDomain(metadata.isOrganization)
                )
            }
        )
    }
}
