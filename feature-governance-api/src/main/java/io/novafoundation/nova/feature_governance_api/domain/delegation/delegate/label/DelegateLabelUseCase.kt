package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label

import io.novasama.substrate_sdk_android.runtime.AccountId

interface DelegateLabelUseCase {

    suspend fun getDelegateLabel(delegate: AccountId): DelegateLabel
}
