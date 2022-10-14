package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_governance_api.data.repository.ReferendumUrlConstructor

class GovernanceDApp(
    val metadata: DappMetadata?,
    val urlConstructor: ReferendumUrlConstructor
)
