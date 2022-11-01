package io.novafoundation.nova.feature_governance_impl.domain.referendum.details

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.asOngoingOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.hash
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.proposerDeposit
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.submissionDeposit
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition.ALWAYS
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.PreimagePreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetails
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.constructReferendumStatus
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParser
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RealReferendumDetailsInteractor(
    private val preImageParsers: Collection<ReferendumCallParser>,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val referendaConstructor: ReferendaConstructor,
    private val preImageSizer: PreImageSizer,
    private val callFormatter: Gson,
) : ReferendumDetailsInteractor {

    override fun referendumDetailsFlow(
        referendumId: ReferendumId,
        chain: Chain,
        voterAccountId: AccountId?,
    ): Flow<ReferendumDetails> {
        return flowOfAll { referendumDetailsFlowSuspend(referendumId, chain, voterAccountId) }
    }

    override suspend fun detailsFor(preImage: PreImage, chain: Chain): ReferendumCall? {
        return preImageParsers.tryFindNonNull { parser ->
            parser.parse(preImage, chain.id)
        }
    }

    override suspend fun previewFor(preImage: PreImage): PreimagePreview {
        val shouldDisplay = preImageSizer.satisfiesSizeConstraint(
            preImageSize = preImage.encodedCall.size.toBigInteger(),
            constraint = PreImageSizer.SizeConstraint.SMALL
        )

        return if (shouldDisplay) {
            val formatted = callFormatter.toJson(preImage.call)

            PreimagePreview.Display(formatted)
        } else {
            PreimagePreview.TooLong
        }
    }

    private suspend fun referendumDetailsFlowSuspend(
        referendumId: ReferendumId,
        chain: Chain,
        voterAccountId: AccountId?,
    ): Flow<ReferendumDetails> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)
        val offChainInfo = governanceSource.offChainInfo.referendumDetails(chain)
        val totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)

        return combine(
            governanceSource.referenda.onChainReferendumFlow(chain.id, referendumId),
            chainStateRepository.currentBlockNumberFlow(chain.id)
        ) { onChainReferendum, currentBlockNumber ->
            val preImage = governanceSource.preImageRepository.preImageOf(onChainReferendum.proposal(), chain.id)
            val track = onChainReferendum.track()?.let(tracksById::get)

            val vote = voterAccountId?.let {
                val voteByReferendumId = governanceSource.convictionVoting.votingFor(voterAccountId, chain.id)
                    .flattenCastingVotes()

                voteByReferendumId[onChainReferendum.id]
            }

            val voting = referendaConstructor.constructReferendumVoting(
                referendum = onChainReferendum,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber,
                totalIssuance = totalIssuance
            )

            val currentStatus = referendaConstructor.constructReferendumStatus(
                chain = chain,
                onChainReferendum = onChainReferendum,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber,
                votingByReferenda = mapOf(referendumId to voting)
            )

            ReferendumDetails(
                id = onChainReferendum.id,
                offChainMetadata = offChainInfo?.let {
                    ReferendumDetails.OffChainMetadata(
                        title = it.title,
                        description = it.description,
                    )
                },
                proposer = onChainReferendum.submissionDeposit()?.let { deposit ->
                    val accountId = deposit.who
                    val nickname = offChainInfo?.proposerName

                    ReferendumProposer(accountId, nickname)
                },
                onChainMetadata = onChainReferendum.proposal()?.hash()?.let { hash ->
                    ReferendumDetails.OnChainMetadata(
                        preImage = preImage,
                        preImageHash = hash
                    )
                },
                track = track?.let { ReferendumTrack(it.id, it.name) },
                voting = voting,
                timeline = ReferendumTimeline(
                    currentStatus = currentStatus,
                    pastEntries = offChainInfo?.pastTimeline ?: referendaConstructor.constructPastTimeline(
                        chain = chain,
                        onChainReferendum = onChainReferendum,
                        calculatedStatus = currentStatus,
                        currentBlockNumber = currentBlockNumber
                    )
                ),
                userVote = vote,
                fullDetails = ReferendumDetails.FullDetails(
                    deposit = onChainReferendum.status.asOngoingOrNull()?.proposerDeposit(),
                    approvalCurve = track?.minApproval,
                    supportCurve = track?.minSupport,
                )
            )
        }
    }
}

private suspend fun PreImageRepository.preImageOf(
    proposal: Proposal?,
    chainId: ChainId,
): PreImage? {
    return when (proposal) {
        is Proposal.Inline -> {
            PreImage(encodedCall = proposal.encodedCall, call = proposal.call)
        }
        is Proposal.Legacy -> {
            val request = PreImageRequest(proposal.hash, knownSize = null, fetchIf = ALWAYS)
            getPreimageFor(request, chainId)
        }
        is Proposal.Lookup -> {
            val request = PreImageRequest(proposal.hash, knownSize = proposal.callLength, fetchIf = ALWAYS)
            getPreimageFor(request, chainId)
        }
        null -> null
    }
}
