package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1

import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.offchain.OffChainReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response.ReferendaPreviewV1Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response.ReferendumDetailsV1Response
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceReferenda.Source

class SubSquareV1ReferendaDataSource(
    private val subSquareApi: SubSquareV1Api,
) : OffChainReferendaDataSource<Source.SubSquare> {

    override suspend fun referendumPreviews(baseUrl: String, options: Source.SubSquare): List<OffChainReferendumPreview> {
        val fullUrl = previewsUrlOf(baseUrl)

        val response = subSquareApi.getReferendumPreviews(fullUrl)

        return response.items.map(::mapReferendumPreviewResponseToPreview)
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, baseUrl: String, options: Source.SubSquare): OffChainReferendumDetails? {
        val fullUrl = detailsUrlOf(baseUrl, referendumId)

        val response = subSquareApi.getReferendumDetails(fullUrl)

        return mapReferendumDetailsResponseToDetails(response)
    }

    private fun mapReferendumPreviewResponseToPreview(post: ReferendaPreviewV1Response.Referendum): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            title = post.title,
            referendumId = ReferendumId(post.referendumIndex),
        )
    }

    private fun mapReferendumDetailsResponseToDetails(referendum: ReferendumDetailsV1Response): OffChainReferendumDetails {
        val timeline = referendum.onchainData.timeline.mapNotNull(::mapReferendumStatusToTimelineEntry)

        return OffChainReferendumDetails(
            title = referendum.title,
            description = referendum.content,
            proposerAddress = referendum.author?.address,
            proposerName = referendum.author?.username,
            timeLine = timeline,
            abstainVotes = null
        )
    }

    private fun mapReferendumStatusToTimelineEntry(status: ReferendumDetailsV1Response.Status): ReferendumTimeline.Entry? {
        val timelineState = when (status.method) {
            "Started" -> ReferendumTimeline.State.CREATED
            "Passed" -> ReferendumTimeline.State.APPROVED
            "NotPassed" -> ReferendumTimeline.State.REJECTED
            "Executed" -> ReferendumTimeline.State.EXECUTED
            else -> null
        }

        return timelineState?.let {
            ReferendumTimeline.Entry(timelineState, status.indexer.blockTime)
        }
    }

    private fun previewsUrlOf(baseUrl: String) = baseUrl.ensureSuffix("/") + "democracy/referendums"

    private fun detailsUrlOf(baseUrl: String, referendumId: ReferendumId) = baseUrl.ensureSuffix("/") + "democracy/referendums/${referendumId.value}"
}
