package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StartSwipeGovValidationPayload(
    val chain: Chain,
    val metaAccount: MetaAccount
)
