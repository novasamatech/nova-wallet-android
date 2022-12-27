package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.PolkassemblyV1Api
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.request.ParachainReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.request.ParachainReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.request.ReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.request.ReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.response.ParachainReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.response.ReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.response.ReferendumDetailsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.response.getId
import io.novafoundation.nova.runtime.ext.polkassemblyParameters
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class Gov1OffChainReferendaInfoRepository(
    private val polkassemblyApi: PolkassemblyV1Api
) : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        try {
            val governanceExternalApi = chain.externalApi?.governance
            if (governanceExternalApi?.type == Chain.ExternalApi.Section.Type.POLKASSEMBLY) {
                val network = governanceExternalApi.polkassemblyParameters?.network
                return if (network == null) {
                    referendaRelaychainRequest(governanceExternalApi.url)
                } else {
                    referendaParachainRequest(governanceExternalApi.url, network)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return emptyList()
    }

    private suspend fun referendaRelaychainRequest(url: String): List<OffChainReferendumPreview> {
        val request = ReferendumPreviewRequest()
        val response = polkassemblyApi.getReferendumPreviews(url, request)
        return response.data.posts.map {
            mapPolkassemblyPostToPreview(it)
        }
    }

    private suspend fun referendaParachainRequest(url: String, network: String): List<OffChainReferendumPreview> {
        val request = ParachainReferendumPreviewRequest(network)
        val response = polkassemblyApi.getParachainReferendumPreviews(url, request)
        return response.data.posts.map {
            mapParachainPolkassemblyPostToPreview(it)
        }
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumDetails? {
        try {
            val governanceExternalApi = chain.externalApi?.governance
            if (governanceExternalApi?.type == Chain.ExternalApi.Section.Type.POLKASSEMBLY) {
                val network = governanceExternalApi.polkassemblyParameters?.network
                val referendumDetails = if (network == null) {
                    detailsRelaychain(governanceExternalApi.url, referendumId)
                } else {
                    detailsParachain(governanceExternalApi.url, network, referendumId)
                }
                return referendumDetails?.let(::mapPolkassemblyPostToDetails)
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    private suspend fun detailsRelaychain(url: String, referendumId: ReferendumId): ReferendumDetailsResponse.Post? {
        val request = ReferendumDetailsRequest(referendumId.value)
        val response = polkassemblyApi.getReferendumDetails(url, request)
        return response.data.posts.firstOrNull()
    }

    private suspend fun detailsParachain(url: String, network: String, referendumId: ReferendumId): ReferendumDetailsResponse.Post? {
        val request = ParachainReferendumDetailsRequest(network, referendumId.value)
        val response = polkassemblyApi.getParachainReferendumDetails(url, request)
        return response.data.posts.firstOrNull()
    }

    private fun mapPolkassemblyPostToPreview(post: ReferendaPreviewResponse.Post): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            post.title,
            ReferendumId(post.getId()),
        )
    }

    private fun mapParachainPolkassemblyPostToPreview(post: ParachainReferendaPreviewResponse.Post): OffChainReferendumPreview {
        return OffChainReferendumPreview(
            post.title,
            ReferendumId(post.getId()),
        )
    }

    private fun mapPolkassemblyPostToDetails(post: ReferendumDetailsResponse.Post): OffChainReferendumDetails {
        val timeline = post.onchainLink
            ?.onchainReferendum
            ?.getOrNull(0)
            ?.referendumStatus
            ?.map {
                mapReferendumStatusToTimelineEntry(it)
            }

        return OffChainReferendumDetails(
            title = post.title,
            description = post.content,
            proposerName = null, // author of the post on PA might not be equal to on-chain submitter so we want to be safe here
            proposerAddress = post.onchainLink?.proposerAddress,
            pastTimeline = timeline
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
