package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class StartSwipeGovValidationFailure {

    class NoChainAccountFound(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : StartSwipeGovValidationFailure(), NoChainAccountFoundError
}
