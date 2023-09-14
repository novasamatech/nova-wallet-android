package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StartStakingLandingValidationPayload(
    val chain: Chain,
    val metaAccount: MetaAccount
)
