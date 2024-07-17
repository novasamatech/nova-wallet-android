package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.formatting.remainingTime
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ayeVotesIfNotEmpty
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.currentlyPassing
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.WithDifferentVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.getName
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.SplitVote
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteDirectionModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimationStyleRefresher
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourMultiVotePreviewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVotePreviewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourMultiVoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourVoteModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

private val oneDay = 1.days

private data class AccountVoteFormatComponent(
    val direction: VoteDirectionModel,
    val amount: String,
    val votes: String,
    val multiplier: String
)

class RealReferendumFormatter(
    private val resourceManager: ResourceManager,
    private val trackFormatter: TrackFormatter,
    private val referendaStatusFormatter: ReferendaStatusFormatter
) : ReferendumFormatter {

    override fun formatVoting(voting: ReferendumVoting, token: Token): ReferendumVotingModel {
        return ReferendumVotingModel(
            positiveFraction = voting.approval.ayeVotesIfNotEmpty()?.fraction?.toFloat(),
            thresholdFraction = voting.approval.threshold.value.toFloat(),
            votingResultIcon = R.drawable.ic_close,
            votingResultIconColor = R.color.icon_negative,
            thresholdInfo = formatThresholdInfo(voting.support, token),
            thresholdInfoVisible = !voting.support.currentlyPassing(),
            positivePercentage = resourceManager.getString(
                R.string.referendum_aye_format,
                voting.approval.ayeVotes.fraction.formatFractionAsPercentage()
            ),
            negativePercentage = resourceManager.getString(
                R.string.referendum_nay_format,
                voting.approval.nayVotes.fraction.formatFractionAsPercentage()
            ),
            thresholdPercentage = resourceManager.getString(
                R.string.referendum_to_pass_format,
                voting.approval.threshold.value.formatFractionAsPercentage()
            )
        )
    }

    override fun formatReferendumTrack(track: ReferendumTrack, asset: Chain.Asset): ReferendumTrackModel {
        val trackModel = trackFormatter.formatTrack(track.track, asset)

        return ReferendumTrackModel(trackModel, sameWithOther = track.sameWithOther)
    }

    override fun formatOnChainName(call: GenericCall.Instance): String {
        return "${call.module.name}.${call.function.name}"
    }

    override fun formatUnknownReferendumTitle(referendumId: ReferendumId): String {
        return resourceManager.getString(R.string.referendum_name_unknown, formatId(referendumId))
    }

    private fun formatThresholdInfo(
        support: ReferendumVoting.Support,
        token: Token
    ): String {
        val thresholdFormatted = mapAmountToAmountModel(support.threshold.value, token).token
        val turnoutFormatted = token.amountFromPlanks(support.turnout).format()

        return resourceManager.getString(R.string.referendum_support_threshold_format, turnoutFormatted, thresholdFormatted)
    }

    override fun formatStatus(status: ReferendumStatus): ReferendumStatusModel {
        val statusName = referendaStatusFormatter.formatStatus(status.type)
        return when (status) {
            is ReferendumStatus.Ongoing.Preparing -> {
                ReferendumStatusModel(
                    name = referendaStatusFormatter.formatStatus(status.type),
                    colorRes = R.color.text_secondary
                )
            }

            is ReferendumStatus.Ongoing.InQueue -> ReferendumStatusModel(
                name = resourceManager.getString(
                    R.string.referendum_status_in_queue_format,
                    status.position.index,
                    status.position.maxSize
                ),
                colorRes = R.color.text_secondary
            )

            is ReferendumStatus.Ongoing.DecidingApprove,
            is ReferendumStatus.Ongoing.DecidingReject -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_secondary
            )

            is ReferendumStatus.Ongoing.Confirming -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_positive
            )

            is ReferendumStatus.Approved -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_positive
            )

            ReferendumStatus.Executed -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_positive
            )

            ReferendumStatus.NotExecuted.Rejected -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_negative
            )

            ReferendumStatus.NotExecuted.Cancelled -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_secondary
            )

            ReferendumStatus.NotExecuted.TimedOut -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_secondary
            )

            ReferendumStatus.NotExecuted.Killed -> ReferendumStatusModel(
                name = statusName,
                colorRes = R.color.text_negative
            )
        }
    }

    override fun formatTimeEstimation(status: ReferendumStatus): ReferendumTimeEstimation? {
        return when (status) {
            is ReferendumStatus.Ongoing.Preparing -> {
                when (val reason = status.reason) {
                    is PreparingReason.DecidingIn -> ReferendumTimeEstimation.Timer(
                        time = reason.timeLeft,
                        timeFormat = R.string.referendum_status_deciding_in,
                        textStyleRefresher = reason.timeLeft.referendumStatusStyleRefresher()
                    )

                    PreparingReason.WaitingForDeposit -> ReferendumTimeEstimation.Timer(
                        time = status.timeOutIn,
                        timeFormat = R.string.referendum_status_time_out_in,
                        textStyleRefresher = status.timeOutIn.referendumStatusStyleRefresher()
                    )
                }
            }

            is ReferendumStatus.Ongoing.InQueue -> {
                ReferendumTimeEstimation.Timer(
                    time = status.timeOutIn,
                    timeFormat = R.string.referendum_status_time_out_in,
                    textStyleRefresher = status.timeOutIn.referendumStatusStyleRefresher()
                )
            }

            is ReferendumStatus.Ongoing.DecidingReject -> ReferendumTimeEstimation.Timer(
                time = status.rejectIn,
                timeFormat = R.string.referendum_status_time_reject_in,
                textStyleRefresher = status.rejectIn.referendumStatusStyleRefresher()
            )

            is ReferendumStatus.Ongoing.DecidingApprove -> ReferendumTimeEstimation.Timer(
                time = status.confirmingIn,
                timeFormat = R.string.referendum_status_time_confirming_in,
                textStyleRefresher = status.confirmingIn.referendumStatusStyleRefresher()
            )

            is ReferendumStatus.Ongoing.Confirming -> ReferendumTimeEstimation.Timer(
                time = status.approveIn,
                timeFormat = R.string.referendum_status_time_approve_in,
                textStyleRefresher = status.approveIn.referendumStatusStyleRefresher()
            )

            is ReferendumStatus.Approved -> ReferendumTimeEstimation.Timer(
                time = status.executeIn,
                timeFormat = R.string.referendum_status_time_execute_in,
                textStyleRefresher = status.executeIn.referendumStatusStyleRefresher()
            )

            ReferendumStatus.Executed -> null
            ReferendumStatus.NotExecuted.Rejected -> null
            ReferendumStatus.NotExecuted.Cancelled -> null
            ReferendumStatus.NotExecuted.TimedOut -> null
            ReferendumStatus.NotExecuted.Killed -> null
        }
    }

    override fun formatId(referendumId: ReferendumId): String {
        return "#${referendumId.value.format()}"
    }

    override fun formatUserVote(referendumVote: ReferendumVote, chain: Chain, chainAsset: Chain.Asset): YourMultiVoteModel {
        val title = when (referendumVote) {
            is ReferendumVote.UserDelegated -> {
                val accountFormatted = referendumVote.voterDisplayIn(chain)

                resourceManager.getString(R.string.delegation_referendum_details_vote, accountFormatted)
            }

            is ReferendumVote.UserDirect -> resourceManager.getString(R.string.referendum_details_your_vote)

            is ReferendumVote.OtherAccount -> error("Not yet supported")
        }

        val yourVoteModels = formatAccountVote(referendumVote.vote, chainAsset).map { formattedVoteComponent ->
            val votesDetails = "${formattedVoteComponent.amount} × ${formattedVoteComponent.multiplier}x"
            val votesCount = resourceManager.getString(R.string.referendum_votes_format, formattedVoteComponent.votes)

            YourVoteModel(
                voteDirection = formattedVoteComponent.direction,
                vote = VoteModel(votesCount, votesDetails),
                voteTitle = title
            )
        }

        return YourMultiVoteModel(yourVoteModels)
    }

    override fun formatReferendumPreview(
        referendum: ReferendumPreview,
        token: Token,
        chain: Chain
    ): ReferendumModel {
        return ReferendumModel(
            id = referendum.id,
            status = formatStatus(referendum.status),
            name = mapReferendumNameToUi(referendum),
            timeEstimation = formatTimeEstimation(referendum.status),
            track = referendum.track?.let { formatReferendumTrack(it, token.configuration) },
            number = formatId(referendum.id),
            voting = referendum.voting?.let { formatVoting(it, token) },
            yourVote = referendum.referendumVote?.let { mapReferendumVoteToUi(it, token.configuration, chain) }
        )
    }

    private fun mapReferendumNameToUi(referendum: ReferendumPreview): String {
        return referendum.getName() ?: formatUnknownReferendumTitle(referendum.id)
    }

    private fun mapReferendumOnChainNameToUi(referendum: ReferendumPreview): String? {
        return when (val proposal = referendum.onChainMetadata?.proposal) {
            is ReferendumProposal.Call -> formatOnChainName(proposal.call)
            else -> null
        }
    }

    private fun mapReferendumVoteToUi(
        referendumVote: ReferendumVote,
        chainAsset: Chain.Asset,
        chain: Chain
    ): YourMultiVotePreviewModel {
        val voteComponents = formatAccountVote(referendumVote.vote, chainAsset).map { formattedComponent ->
            val details = when (referendumVote) {
                is ReferendumVote.UserDirect -> {
                    resourceManager.getString(R.string.referendum_your_vote_format, formattedComponent.votes)
                }

                is ReferendumVote.UserDelegated -> {
                    val accountFormatted = referendumVote.voterDisplayIn(chain)

                    resourceManager.getString(R.string.delegation_referendum_vote, formattedComponent.votes, accountFormatted)
                }

                is ReferendumVote.OtherAccount -> {
                    val accountFormatted = referendumVote.voterDisplayIn(chain)

                    resourceManager.getString(R.string.referendum_other_votes, formattedComponent.votes, accountFormatted)
                }
            }

            YourVotePreviewModel(
                voteDirection = formattedComponent.direction,
                details = details
            )
        }

        return YourMultiVotePreviewModel(voteComponents)
    }

    private fun WithDifferentVoter.voterDisplayIn(chain: Chain): String {
        return whoIdentity?.name ?: chain.addressOf(who)
    }

    private fun TimerValue.referendumStatusStyleRefresher(): ReferendumTimeEstimationStyleRefresher = {
        if (referendumStatusIsHot()) {
            ReferendumTimeEstimation.TextStyle.hot()
        } else {
            ReferendumTimeEstimation.TextStyle.regular()
        }
    }

    private fun ReferendumTimeEstimation.TextStyle.Companion.hot() = ReferendumTimeEstimation.TextStyle(
        iconRes = R.drawable.ic_fire,
        textColorRes = R.color.text_warning,
        iconColorRes = R.color.icon_warning,
    )

    private fun ReferendumTimeEstimation.TextStyle.Companion.regular() = ReferendumTimeEstimation.TextStyle(
        iconRes = R.drawable.ic_time_16,
        textColorRes = R.color.text_secondary,
        iconColorRes = R.color.icon_secondary,
    )

    private fun TimerValue.referendumStatusIsHot(): Boolean {
        return remainingTime().milliseconds < oneDay
    }

    private fun formatAccountVote(vote: AccountVote, chainAsset: Chain.Asset): List<AccountVoteFormatComponent> {
        return when (vote) {
            is AccountVote.Standard -> {
                val voteDirection = if (vote.vote.aye) VoteType.AYE else VoteType.NAY
                val convictionVote = GenericVoter.ConvictionVote(chainAsset.amountFromPlanks(vote.balance), vote.vote.conviction)

                val formattedComponent = formatDirectedConvictionVote(voteDirection, convictionVote, chainAsset)
                listOf(formattedComponent)
            }

            is AccountVote.Split -> formatNonZeroConvictionVote(
                VoteType.AYE to SplitVote(vote.aye, chainAsset),
                VoteType.NAY to SplitVote(vote.nay, chainAsset),
                chainAsset = chainAsset
            )

            is AccountVote.SplitAbstain -> formatNonZeroConvictionVote(
                VoteType.AYE to SplitVote(vote.aye, chainAsset),
                VoteType.NAY to SplitVote(vote.nay, chainAsset),
                VoteType.ABSTAIN to SplitVote(vote.abstain, chainAsset),
                chainAsset = chainAsset
            )

            AccountVote.Unsupported -> emptyList()
        }
    }

    private fun formatNonZeroConvictionVote(
        vararg directedVotes: Pair<VoteType, GenericVoter.ConvictionVote>,
        chainAsset: Chain.Asset
    ): List<AccountVoteFormatComponent> {
        return directedVotes.mapNotNull { (direction, convictionVote) ->
            if (convictionVote.amount.isZero) return@mapNotNull null

            formatDirectedConvictionVote(direction, convictionVote, chainAsset)
        }
    }

    private fun formatDirectedConvictionVote(
        direction: VoteType,
        convictionVote: GenericVoter.ConvictionVote,
        chainAsset: Chain.Asset
    ): AccountVoteFormatComponent = AccountVoteFormatComponent(
        direction = formatVoteType(direction),
        amount = convictionVote.amount.formatTokenAmount(chainAsset),
        votes = convictionVote.totalVotes.format(),
        multiplier = convictionVote.conviction.amountMultiplier().format()
    )

    private fun formatVoteType(voteDirection: VoteType): VoteDirectionModel {
        return when (voteDirection) {
            VoteType.AYE -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_aye), R.color.text_positive)
            VoteType.NAY -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_nay), R.color.text_negative)
            VoteType.ABSTAIN -> VoteDirectionModel(resourceManager.getString(R.string.referendum_vote_abstain), R.color.text_secondary)
        }
    }
}
