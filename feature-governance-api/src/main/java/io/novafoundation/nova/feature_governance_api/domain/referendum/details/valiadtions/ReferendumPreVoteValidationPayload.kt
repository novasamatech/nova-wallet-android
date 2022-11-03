package io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ReferendumPreVoteValidationPayload(
    val metaAccount: MetaAccount,
    val chain: Chain
)
