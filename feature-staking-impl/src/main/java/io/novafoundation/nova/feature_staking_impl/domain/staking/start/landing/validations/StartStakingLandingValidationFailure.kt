package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class StartStakingLandingValidationFailure {

    class NoChainAccountFound(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : StartStakingLandingValidationFailure(), NoChainAccountFoundError
}
