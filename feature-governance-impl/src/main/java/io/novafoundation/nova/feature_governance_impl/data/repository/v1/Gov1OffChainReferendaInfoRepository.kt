package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.PolkassemblyApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request.ReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request.ReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response.ReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response.ReferendumDetailsResponse
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.util.*

class Gov1OffChainReferendaInfoRepository(
    private val polkassemblyApi: PolkassemblyApi
) : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        val governanceExternalApi = chain.externalApi?.governance
        if (governanceExternalApi?.type == Chain.ExternalApi.Section.Type.POLKASSEMBLY) {
            val request = ReferendumPreviewRequest()
            val response = polkassemblyApi.getReferendumPreviews(governanceExternalApi.url, request)
            return response.data.posts.map {
                mapPolkassemblyPostToPreview(it)
            }
        }

        return emptyList()
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumDetails? {
        val governanceExternalApi = chain.externalApi?.governance
        if (governanceExternalApi?.type == Chain.ExternalApi.Section.Type.POLKASSEMBLY) {
            val request = ReferendumDetailsRequest(referendumId.value)
            val response = polkassemblyApi.getReferendumDetails(governanceExternalApi.url, request)
            val referendumDetails = response.data.posts.getOrNull(0)
            return referendumDetails?.let(::mapPolkassemblyPostToDetails)
        }

        return null
    }

    private fun mapPolkassemblyPostToPreview(post: ReferendaPreviewResponse.Post): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            post.title,
            ReferendumId(post.id),
        )
    }

    private fun mapPolkassemblyPostToDetails(post: ReferendumDetailsResponse.Post): OffChainReferendumDetails {
        val timeline = post.onchain_link
            .onchain_referendum
            .getOrNull(0)
            ?.referendumStatus
            ?.map {
                mapReferendumStatusToTimelineEntry(it)
            }

        return OffChainReferendumDetails(
            post.title,
            post.content,
            post.author.username,
            timeline
        )
    }

    private fun mapReferendumStatusToTimelineEntry(status: ReferendumDetailsResponse.Status): ReferendumTimeline.Entry {
        val timelineState = when (status.status) {
            "Started" -> ReferendumTimeline.State.CREATED
            "Passed" -> ReferendumTimeline.State.APPROVED
            "NotPassed" -> ReferendumTimeline.State.REJECTED
            "Executed" -> ReferendumTimeline.State.EXECUTED
            else -> error("Unkonown referendum status")
        }

        val statusDate = parseDateISO_8601(status.blockNumber.startDateTime)

        return ReferendumTimeline.Entry(timelineState, statusDate?.time)
    }
}
