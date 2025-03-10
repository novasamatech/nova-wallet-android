package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2

import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.offchain.OffChainReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response.ReferendaPreviewV2Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response.ReferendumDetailsV2Response
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceReferenda.Source

class SubSquareV2ReferendaDataSource(
    private val subSquareApi: SubSquareV2Api
) : OffChainReferendaDataSource<Source.SubSquare> {

    override suspend fun referendumPreviews(baseUrl: String, options: Source.SubSquare): List<OffChainReferendumPreview> {
        val fullUrl = previewsUrlOf(baseUrl)

        val response = subSquareApi.getReferendumPreviews(fullUrl)

        return response.items.map(::mapPolkassemblyPostToPreview)
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, baseUrl: String, options: Source.SubSquare): OffChainReferendumDetails {
        val detailsUrl = detailsUrlOf(baseUrl, referendumId)
        val referendaDetails = subSquareApi.getReferendumDetails(detailsUrl)

        return mapPolkassemblyPostToDetails(referendaDetails)
    }

    private fun mapPolkassemblyPostToPreview(post: ReferendaPreviewV2Response.Referendum): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            title = post.title,
            referendumId = ReferendumId(post.referendumIndex),
        )
    }

    private fun mapPolkassemblyPostToDetails(
        referendum: ReferendumDetailsV2Response
    ): OffChainReferendumDetails {
        val timeline = referendum.onchainData.timeline.mapNotNull(::mapReferendumStatusToTimelineEntry)

        return OffChainReferendumDetails(
            title = referendum.title,
            description = referendum.content,
            proposerAddress = referendum.author?.address,
            proposerName = referendum.author?.username,
            timeLine = timeline
        )
    }

    private fun mapReferendumStatusToTimelineEntry(status: ReferendumDetailsV2Response.Status): ReferendumTimeline.Entry? {
        val timelineState = when (status.name) {
            "Submitted" -> ReferendumTimeline.State.CREATED
            "Confirmed" -> ReferendumTimeline.State.APPROVED
            "Rejected" -> ReferendumTimeline.State.REJECTED
            "Cancelled" -> ReferendumTimeline.State.CANCELLED
            "TimedOut" -> ReferendumTimeline.State.TIMED_OUT
            "Killed" -> ReferendumTimeline.State.KILLED
            else -> null
        }

        return timelineState?.let {
            ReferendumTimeline.Entry(timelineState, status.indexer.blockTime)
        }
    }

    private fun previewsUrlOf(baseUrl: String) = baseUrl.ensureSuffix("/") + "gov2/referendums"

    private fun detailsUrlOf(baseUrl: String, referendumId: ReferendumId) = baseUrl.ensureSuffix("/") + "gov2/referendums/${referendumId.value}"
}
