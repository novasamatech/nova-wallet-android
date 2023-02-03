package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label

import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface DelegateLabelUseCase {

    suspend fun getDelegateLabel(delegate: AccountId): DelegateLabel
}
