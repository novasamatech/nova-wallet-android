package io.novafoundation.nova.feature_governance_impl.domain.referendum.details

import com.google.gson.Gson
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
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
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.toTallyOrNull
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition.ALWAYS
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.repository.preImageOf
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.data.thresold.gov1.asGovV1VotingThresholdOrNull
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.PreimagePreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetails
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline.State
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.constructReferendumStatus
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex

class RealReferendumDetailsInteractor(
    private val preImageParser: ReferendumPreImageParser,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val referendaConstructor: ReferendaConstructor,
    private val preImageSizer: PreImageSizer,
    private val callFormatter: Gson,
    private val identityRepository: OnChainIdentityRepository,
    private val offChainReferendumVotingSharedComputation: OffChainReferendumVotingSharedComputation
) : ReferendumDetailsInteractor {

    override fun referendumDetailsFlow(
        referendumId: ReferendumId,
        selectedGovernanceOption: SupportedGovernanceOption,
        voterAccountId: AccountId?,
        coroutineScope: CoroutineScope
    ): Flow<ReferendumDetails?> {
        return flowOfAll { referendumDetailsFlowSuspend(referendumId, selectedGovernanceOption, voterAccountId, coroutineScope) }
    }

    override suspend fun detailsFor(preImage: PreImage, chain: Chain): ReferendumCall? {
        return preImageParser.parse(preImage, chain.id)
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

    override suspend fun isSupportAbstainVoting(selectedGovernanceOption: SupportedGovernanceOption): Boolean {
        return governanceSourceRegistry.sourceFor(selectedGovernanceOption)
            .convictionVoting
            .isAbstainVotingAvailable()
    }

    /**
     * Emmit null if referendum is not exist
     */
    private suspend fun referendumDetailsFlowSuspend(
        referendumId: ReferendumId,
        selectedGovernanceOption: SupportedGovernanceOption,
        voterAccountId: AccountId?,
        coroutineScope: CoroutineScope
    ): Flow<ReferendumDetails?> {
        val chain = selectedGovernanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)

        val offChainInfoDeferred = coroutineScope.async { governanceSource.offChainInfo.referendumDetails(referendumId, chain) }
        val electorateDeferred = coroutineScope.async { governanceSource.referenda.electorate(chain.id) }
        val tracksByIdDeferred = coroutineScope.async { governanceSource.referenda.getTracksById(chain.id) }

        return combineTransform(
            referendumFlow(governanceSource, selectedGovernanceOption, referendumId, coroutineScope),
            chainStateRepository.currentBlockNumberFlow(chain.id)
        ) { referendumWithVotingDetails, currentBlockNumber ->
            // If null it means that referendum with given id doesn't exist
            if (referendumWithVotingDetails == null) {
                emit(null)
                return@combineTransform
            }

            val onChainReferendum = referendumWithVotingDetails.onChainReferendum
            val offChainVotingDetails = referendumWithVotingDetails.votingDetails
            val offChainInfo = offChainInfoDeferred.await()
            val electorate = electorateDeferred.await()
            val tracksById = tracksByIdDeferred.await()

            val preImage = governanceSource.preImageRepository.preImageOf(onChainReferendum.proposal(), chain.id)

            val track = (onChainReferendum.track() ?: offChainVotingDetails.dataOrNull?.trackId)?.let(tracksById::get)

            val vote = voterAccountId?.let {
                val voteByReferendumId = governanceSource.convictionVoting.votingFor(voterAccountId, chain.id)
                    .flattenCastingVotes()

                val onChainVote = voteByReferendumId[onChainReferendum.id]

                governanceSource.constructVoterVote(it, chain, referendumId, onChainVote)
            }

            val voting = referendaConstructor.constructReferendumVoting(
                tally = onChainReferendum.status.asOngoingOrNull()?.tally ?: offChainVotingDetails.dataOrNull?.votingInfo?.toTallyOrNull(),
                offChainVotingDetails = offChainVotingDetails,
                currentBlockNumber = currentBlockNumber,
                electorate = electorate,
            )

            val threshold = referendaConstructor.constructReferendumThreshold(
                referendum = onChainReferendum,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber,
                electorate = electorate
            )

            val currentStatus = referendaConstructor.constructReferendumStatus(
                selectedGovernanceOption = selectedGovernanceOption,
                onChainReferendum = onChainReferendum,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber,
                votingByReferenda = mapOf(referendumId to voting),
                thresholdByReferenda = mapOf(referendumId to threshold)
            )

            val referendumDetails = ReferendumDetails(
                id = onChainReferendum.id,
                offChainMetadata = offChainInfo?.let {
                    ReferendumDetails.OffChainMetadata(
                        title = it.title,
                        description = it.description,
                    )
                },
                proposer = constructProposer(onChainReferendum, offChainInfo, chain),
                onChainMetadata = onChainReferendum.proposal()?.hash()?.let { hash ->
                    ReferendumDetails.OnChainMetadata(
                        preImage = preImage,
                        preImageHash = hash
                    )
                },
                track = track?.let { ReferendumTrack(mapTrackInfoToTrack(it), sameWithOther = tracksById.size == 1) },
                voting = voting,
                threshold = threshold,
                timeline = ReferendumTimeline(
                    currentStatus = currentStatus,
                    pastEntries = offChainInfo.pastTimeLine(currentStatus) mergeWith referendaConstructor.constructPastTimeline(
                        chain = chain,
                        onChainReferendum = onChainReferendum,
                        calculatedStatus = currentStatus,
                        currentBlockNumber = currentBlockNumber
                    )
                ),
                userVote = vote,
                fullDetails = ReferendumDetails.FullDetails(
                    deposit = onChainReferendum.status.asOngoingOrNull()?.proposerDeposit(),
                    voteThreshold = onChainReferendum.status.asOngoingOrNull()?.threshold?.asGovV1VotingThresholdOrNull(),
                    approvalCurve = track?.minApproval,
                    supportCurve = track?.minSupport,
                )
            )

            emit(referendumDetails)
        }
    }

    @Suppress("MoveVariableDeclarationIntoWhen")
    private suspend fun GovernanceSource.constructVoterVote(
        voter: AccountId,
        chain: Chain,
        referendumId: ReferendumId,
        onChainVote: AccountVote?,
    ): ReferendumVote? {
        val historicalVote = delegationsRepository.historicalVoteOf(voter, referendumId, chain)

        val offChainReferendumVote = when (historicalVote) {
            is UserVote.Delegated -> {
                val identity = identityRepository.getIdentityFromId(chain.id, historicalVote.delegate)

                ReferendumVote.UserDelegated(
                    who = historicalVote.delegate,
                    whoIdentity = identity?.let(::Identity),
                    vote = historicalVote.vote
                )
            }

            is UserVote.Direct -> ReferendumVote.UserDirect(historicalVote.vote)

            null -> null
        }

        val onChainReferendumVote = onChainVote?.let(ReferendumVote::UserDirect)

        // priority is for on chain data
        return offChainReferendumVote ?: onChainReferendumVote
    }

    private fun constructProposer(
        onChainReferendum: OnChainReferendum,
        offChainReferendumDetails: OffChainReferendumDetails?,
        chain: Chain,
    ): ReferendumProposer? {
        val submissionDeposit = onChainReferendum.submissionDeposit()
        val offChainProposerAddress = offChainReferendumDetails?.proposerAddress

        val proposerAccountId = when {
            submissionDeposit != null -> submissionDeposit.who
            offChainProposerAddress != null -> chain.accountIdOrNull(offChainProposerAddress)
            else -> null
        }

        return proposerAccountId?.let {
            ReferendumProposer(
                accountId = it,
                offChainNickname = offChainReferendumDetails?.proposerName
            )
        }
    }

    private infix fun List<ReferendumTimeline.Entry>.mergeWith(another: List<ReferendumTimeline.Entry>): List<ReferendumTimeline.Entry> {
        return (this + another).distinctBy { it.state }
    }

    private fun OffChainReferendumDetails?.pastTimeLine(currentStatus: ReferendumStatus): List<ReferendumTimeline.Entry> {
        return this?.timeLine?.let { timeLine ->
            timeLine.filter { it.state.isPastStateAt(currentStatus) }
        }.orEmpty()
    }

    private fun State.isPastStateAt(currentStatus: ReferendumStatus): Boolean {
        return when (this) {
            // When currentStatus is Approved we will display it as a current status so no extra entry should be present in the past timeline
            State.APPROVED -> currentStatus is ReferendumStatus.Executed
            else -> true
        }
    }

    private suspend fun referendumFlow(
        governanceSource: GovernanceSource,
        governanceOption: SupportedGovernanceOption,
        referendumId: ReferendumId,
        coroutineScope: CoroutineScope
    ): Flow<OnChainReferendaWithVotingDetails?> {
        return governanceSource.referenda.onChainReferendumFlow(governanceOption.assetWithChain.chain.id, referendumId)
            .withIndex()
            .transformLatest { (index, onChainReferendum) ->
                if (onChainReferendum == null) {
                    emit(null)
                    return@transformLatest
                }

                // First time emmit without voting details
                if (index == 0) {
                    emit(OnChainReferendaWithVotingDetails(onChainReferendum, votingDetails = ExtendedLoadingState.Loading))
                }

                emit(
                    OnChainReferendaWithVotingDetails(
                        onChainReferendum,
                        votingDetails = offChainReferendumVotingSharedComputation.votingDetails(onChainReferendum, governanceOption, coroutineScope)
                            .asLoaded()
                    )
                )
            }
    }
}

private class OnChainReferendaWithVotingDetails(
    val onChainReferendum: OnChainReferendum,
    val votingDetails: ExtendedLoadingState<OffChainReferendumVotingDetails?>
)
