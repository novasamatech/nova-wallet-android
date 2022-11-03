package io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class ReferendumPreVoteValidationFailure {

    class NoRelaychainAccount(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : ReferendumPreVoteValidationFailure(), NoChainAccountFoundError
}
