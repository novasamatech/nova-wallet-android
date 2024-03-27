package io.novafoundation.nova.feature_governance_api.presentation.referenda.common

import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatusType

interface ReferendaStatusFormatter {
    fun formatStatus(status: ReferendumStatusType): String
}
