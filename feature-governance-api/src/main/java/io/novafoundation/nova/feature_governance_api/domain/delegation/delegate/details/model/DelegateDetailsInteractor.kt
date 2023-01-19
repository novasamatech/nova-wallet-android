package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption

interface DelegateDetailsInteractor {

    suspend fun getDelegateDetails(
        delegateAddress: String,
        governanceOption: SupportedGovernanceOption,
    ): Result<DelegateDetails>
}
