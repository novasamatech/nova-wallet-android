package io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class MainCrowdloanValidationPayload(
    val metaAccount: MetaAccount,
    val chain: Chain
)
