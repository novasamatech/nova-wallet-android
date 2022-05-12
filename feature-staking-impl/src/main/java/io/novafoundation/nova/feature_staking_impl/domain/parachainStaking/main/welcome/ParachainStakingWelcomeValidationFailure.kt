package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class ParachainStakingWelcomeValidationFailure {

    class MissingEthereumAccount(val chain: Chain, val metaAccount: MetaAccount) : ParachainStakingWelcomeValidationFailure()
}
