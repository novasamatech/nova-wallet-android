package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.offchain.OffChainReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.request.ReferendumDetailsV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.request.ReferendumPreviewV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.response.ReferendaPreviewV2Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.response.ReferendumDetailsV2Response
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceReferenda.Source

class PolkassemblyV2ReferendaDataSource(
    private val polkassemblyApi: PolkassemblyV2Api
) : OffChainReferendaDataSource<Source.Polkassembly> {

    override suspend fun referendumPreviews(baseUrl: String, options: Source.Polkassembly): List<OffChainReferendumPreview> {
        val request = ReferendumPreviewV2Request()
        val response = polkassemblyApi.getReferendumPreviews(baseUrl, request)

        return response.data.posts.map(::mapPolkassemblyPostToPreview)
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, baseUrl: String, options: Source.Polkassembly): OffChainReferendumDetails? {
        val request = ReferendumDetailsV2Request(referendumId.value)
        val response = polkassemblyApi.getReferendumDetails(baseUrl, request)
        val referendumDetails = response.data.posts.firstOrNull()

        return referendumDetails?.let(::mapPolkassemblyPostToDetails)
    }

    private fun mapPolkassemblyPostToPreview(post: ReferendaPreviewV2Response.Post): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            title = post.title,
            referendumId = ReferendumId(post.onChainLink.onChainReferendumId),
        )
    }

    private fun mapPolkassemblyPostToDetails(post: ReferendumDetailsV2Response.Post): OffChainReferendumDetails {
        val timeline = post.onchainLink.onchainReferendum
            .firstOrNull()
            ?.referendumStatus
            ?.mapNotNull {
                mapReferendumStatusToTimelineEntry(it)
            }

        return OffChainReferendumDetails(
            title = post.title,
            description = post.content,
            proposerAddress = null,
            proposerName = post.author.username,
            timeLine = timeline
        )
    }

    private fun mapReferendumStatusToTimelineEntry(status: ReferendumDetailsV2Response.Status): ReferendumTimeline.Entry? {
        val timelineState = when (status.status) {
            "Submitted" -> ReferendumTimeline.State.CREATED
            "Ongoing" -> ReferendumTimeline.State.CREATED
            "Approved" -> ReferendumTimeline.State.APPROVED
            "Rejected" -> ReferendumTimeline.State.REJECTED
            "Cancelled" -> ReferendumTimeline.State.CANCELLED
            "TimedOut" -> ReferendumTimeline.State.TIMED_OUT
            "Killed" -> ReferendumTimeline.State.KILLED
            "Executed" -> ReferendumTimeline.State.EXECUTED
            else -> null
        }

        val statusDate = parseDateISO_8601(status.blockNumber.startDateTime)

        return timelineState?.let {
            ReferendumTimeline.Entry(timelineState, statusDate?.time)
        }
    }
}
