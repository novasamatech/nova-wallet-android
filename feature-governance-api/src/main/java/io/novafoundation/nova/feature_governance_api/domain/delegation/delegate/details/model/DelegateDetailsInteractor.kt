package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model

import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface DelegateDetailsInteractor {

    suspend fun getDelegateDetails(
        delegateAccountId: AccountId,
    ): Result<DelegateDetails>
}
