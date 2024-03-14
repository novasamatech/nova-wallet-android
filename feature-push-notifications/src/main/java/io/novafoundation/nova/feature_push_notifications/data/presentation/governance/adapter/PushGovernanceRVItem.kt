package io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter

import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceModel

data class PushGovernanceRVItem(
    val model: PushGovernanceModel,
    val tracksText: String
) {
    val chainId = model.chainId

    val governance = model.governance

    val chainName = model.chainName

    val chainIconUrl = model.chainIconUrl

    val isEnabled = model.isEnabled

    val isNewReferendaEnabled = model.isNewReferendaEnabled

    val isReferendaUpdatesEnabled = model.isReferendaUpdatesEnabled
}
