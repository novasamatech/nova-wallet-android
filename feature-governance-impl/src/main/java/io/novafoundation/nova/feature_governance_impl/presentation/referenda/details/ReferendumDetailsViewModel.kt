package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

class ReferendumDetailsViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumDetailsPayload
) : BaseViewModel() {
    
    fun backClicked() {
        router.back()
    }
}
