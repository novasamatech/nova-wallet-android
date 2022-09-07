package io.novafoundation.nova.feature_staking_impl.domain.validations.welcome

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class WelcomeStakingValidationPayload(
    val metaAccount: MetaAccount,
    val chain: Chain
)
