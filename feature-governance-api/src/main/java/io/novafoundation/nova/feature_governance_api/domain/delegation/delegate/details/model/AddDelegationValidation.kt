package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias AddDelegationValidationSystem = ValidationSystem<AddDelegationValidationPayload, AddDelegationValidationFailure>

sealed interface AddDelegationValidationFailure {
    class NoChainAccountFailure(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : AddDelegationValidationFailure, NoChainAccountFoundError
}

data class AddDelegationValidationPayload(
    val chain: Chain,
    val metaAccount: MetaAccount
)
