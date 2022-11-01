package io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting

import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus

class RealReferendaSortingProvider : ReferendaSortingProvider {

    override suspend fun getReferendumSorting(group: ReferendumGroup): Comparator<ReferendumPreview> {
        return when (group) {
            ReferendumGroup.ONGOING -> getOngoingSorting()
            ReferendumGroup.COMPLETED -> getCompletedSorting()
        }
    }

    override suspend fun getGroupSorting(): Comparator<ReferendumGroup> {
        return compareBy {
            when (it) {
                ReferendumGroup.ONGOING -> 0
                ReferendumGroup.COMPLETED -> 1
            }
        }
    }

    private fun getOngoingSorting(): Comparator<ReferendumPreview> {
        return compareBy {
            when (val status = it.status) {
                is ReferendumStatus.Ongoing.Confirming -> status.approveIn.millis
                is ReferendumStatus.Ongoing.InQueue -> status.timeOutIn.millis
                is ReferendumStatus.Ongoing.Preparing -> status.timeOutIn.millis
                is ReferendumStatus.Ongoing.Rejecting -> status.rejectIn.millis

                // other statuses should not be in Ongoing group but just in case - put it at the end
                else -> Long.MAX_VALUE
            }
        }
    }

    private fun getCompletedSorting(): Comparator<ReferendumPreview> {
        return compareBy<ReferendumPreview> {
            when (val status = it.status) {
                is ReferendumStatus.Approved -> status.executeIn.millis

                // approved at the end
                else -> Long.MAX_VALUE
            }
        }.thenByDescending { it.id.value }
    }
}
