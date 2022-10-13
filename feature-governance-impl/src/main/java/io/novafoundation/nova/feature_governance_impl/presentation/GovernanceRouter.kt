package io.novafoundation.nova.feature_governance_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload

interface GovernanceRouter : ReturnableRouter {
    fun openReferendum(payload: ReferendumDetailsPayload)
}
