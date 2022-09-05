package io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class MainCrowdloanValidationFailure {

    class NoRelaychainAccount(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : MainCrowdloanValidationFailure(), NoChainAccountFoundError
}
