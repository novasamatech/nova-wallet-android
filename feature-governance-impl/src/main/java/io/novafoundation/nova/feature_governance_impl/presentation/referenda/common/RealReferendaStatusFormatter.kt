package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatusType
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_governance_impl.R

class RealReferendaStatusFormatter(
    private val resourceManager: ResourceManager,
) : ReferendaStatusFormatter {

    override fun formatStatus(status: ReferendumStatusType): String {
        return when (status) {
            ReferendumStatusType.WAITING_DEPOSIT -> resourceManager.getString(R.string.referendum_status_waiting_deposit)
            ReferendumStatusType.PREPARING -> resourceManager.getString(R.string.referendum_status_preparing)
            ReferendumStatusType.IN_QUEUE -> resourceManager.getString(R.string.referendum_status_in_queue)
            ReferendumStatusType.DECIDING -> resourceManager.getString(R.string.referendum_status_deciding)
            ReferendumStatusType.CONFIRMING -> resourceManager.getString(R.string.referendum_status_passing)
            ReferendumStatusType.APPROVED -> resourceManager.getString(R.string.referendum_status_approved)
            ReferendumStatusType.EXECUTED -> resourceManager.getString(R.string.referendum_status_executed)
            ReferendumStatusType.TIMED_OUT -> resourceManager.getString(R.string.referendum_status_timeout)
            ReferendumStatusType.KILLED -> resourceManager.getString(R.string.referendum_status_killed)
            ReferendumStatusType.CANCELLED -> resourceManager.getString(R.string.referendum_status_cancelled)
            ReferendumStatusType.REJECTED -> resourceManager.getString(R.string.referendum_status_rejected)
        }
    }
}
