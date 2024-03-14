package io.novafoundation.nova.feature_push_notifications.presentation.governance.adapter

import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceModel

data class PushGovernanceRVItem(
    val model: _root_ide_package_.io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceModel,
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
