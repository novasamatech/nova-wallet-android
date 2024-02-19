package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model

import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface DelegateDetailsInteractor {

    fun delegateDetailsFlow(
        delegateAccountId: AccountId,
    ): Flow<DelegateDetails>

    fun validationSystemFor(): AddDelegationValidationSystem
}
