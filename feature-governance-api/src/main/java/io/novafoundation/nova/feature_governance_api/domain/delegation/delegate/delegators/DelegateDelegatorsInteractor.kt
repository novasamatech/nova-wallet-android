package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface DelegateDelegatorsInteractor {

    fun delegatorsFlow(delegateId: AccountId): Flow<List<Delegator>>
}
