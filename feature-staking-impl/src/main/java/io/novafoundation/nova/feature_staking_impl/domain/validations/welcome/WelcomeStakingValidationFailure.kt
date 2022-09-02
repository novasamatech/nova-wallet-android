package io.novafoundation.nova.feature_staking_impl.domain.validations.welcome

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.NoChainAccountFoundError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class WelcomeStakingValidationFailure {
    object MaxNominatorsReached : WelcomeStakingValidationFailure()

    class NoRelayChainAccount(
        override val chain: Chain,
        override val account: MetaAccount,
        override val addAccountState: NoChainAccountFoundError.AddAccountState
    ) : WelcomeStakingValidationFailure(), NoChainAccountFoundError
}
