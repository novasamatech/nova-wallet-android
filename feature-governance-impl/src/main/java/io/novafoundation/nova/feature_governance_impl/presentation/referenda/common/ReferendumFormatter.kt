package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.formatting.remainingTime
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isAye
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ayeVotesIfNotEmpty
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.passing
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.AUCTION_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.BIG_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.BIG_TIPPER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.FELLOWSHIP_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.GENERAL_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.LEASE_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.MEDIUM_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.OTHER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.REFERENDUM_CANCELLER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.REFERENDUM_KILLER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.ROOT
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.SMALL_SPEND
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.SMALL_TIPPER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.STAKING_ADMIN
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.TREASURER
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackType.WHITELISTED_CALLER
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimationStyleRefresher
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVotePreviewModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourVoteModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

interface ReferendumFormatter {

    fun formatVoting(voting: ReferendumVoting, token: Token): ReferendumVotingModel

    fun formatTrack(track: ReferendumTrack, asset: Chain.Asset): ReferendumTrackModel

    fun formatOnChainName(call: GenericCall.Instance): String

    fun formatUnknownReferendumTitle(referendumId: ReferendumId): String

    fun formatStatus(status: ReferendumStatus): ReferendumStatusModel

    fun formatTimeEstimation(status: ReferendumStatus): ReferendumTimeEstimation?

    fun formatId(referendumId: ReferendumId): String

    fun formatUserVote(vote: AccountVote, token: Token): YourVoteModel?

    fun formatReferendumPreview(
        referendum: ReferendumPreview,
        token: Token,
        chain: Chain
    ): ReferendumModel
}

private val oneDay = 1.days

class RealReferendumFormatter(
    private val resourceManager: ResourceManager,
    private val trackCategorizer: TrackCategorizer,
) : ReferendumFormatter {

    override fun formatVoting(voting: ReferendumVoting, token: Token): ReferendumVotingModel {
        return ReferendumVotingModel(
            positiveFraction = voting.approval.ayeVotesIfNotEmpty()?.fraction?.toFloat(),
            thresholdFraction = voting.approval.threshold.value.toFloat(),
            votingResultIcon = R.drawable.ic_close,
            votingResultIconColor = R.color.icon_negative,
            thresholdInfo = formatThresholdInfo(voting.support, token),
            thresholdInfoVisible = !voting.support.passing(),
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

    override fun formatTrack(track: ReferendumTrack, asset: Chain.Asset): ReferendumTrackModel {
        return when (trackCategorizer.typeOf(track.name)) {
            ROOT -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_root),
                icon = asset.iconUrl?.let { Icon.FromLink(it) } ?: Icon.FromDrawableRes(R.drawable.ic_block),
                sameWithOther = track.sameWithOther
            )
            WHITELISTED_CALLER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_whitelisted_caller),
                icon = Icon.FromDrawableRes(R.drawable.ic_users),
                sameWithOther = track.sameWithOther
            )
            STAKING_ADMIN -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_staking_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_staking_filled),
                sameWithOther = track.sameWithOther
            )
            TREASURER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_treasurer),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            LEASE_ADMIN -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_lease_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
                sameWithOther = track.sameWithOther
            )
            FELLOWSHIP_ADMIN -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_fellowship_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_users),
                sameWithOther = track.sameWithOther
            )
            GENERAL_ADMIN -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_general_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
                sameWithOther = track.sameWithOther
            )
            AUCTION_ADMIN -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_auction_admin),
                icon = Icon.FromDrawableRes(R.drawable.ic_rocket),
                sameWithOther = track.sameWithOther
            )
            REFERENDUM_CANCELLER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_canceller),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
                sameWithOther = track.sameWithOther
            )
            REFERENDUM_KILLER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_killer),
                icon = Icon.FromDrawableRes(R.drawable.ic_governance_check_to_slot),
                sameWithOther = track.sameWithOther
            )
            SMALL_TIPPER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            BIG_TIPPER -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_tipper),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            SMALL_SPEND -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            MEDIUM_SPEND -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_medium_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            BIG_SPEND -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_spender),
                icon = Icon.FromDrawableRes(R.drawable.ic_gem),
                sameWithOther = track.sameWithOther
            )
            OTHER -> ReferendumTrackModel(
                name = mapUnknownTrackNameToUi(track.name),
                icon = Icon.FromDrawableRes(R.drawable.ic_block),
                sameWithOther = track.sameWithOther
            )
        }
    }

    override fun formatOnChainName(call: GenericCall.Instance): String {
        return "${call.module.name}.${call.function.name}"
    }

    override fun formatUnknownReferendumTitle(referendumId: ReferendumId): String {
        return resourceManager.getString(R.string.referendum_name_unknown, formatId(referendumId))
    }

    private fun mapUnknownTrackNameToUi(name: String): String {
        return name.replace("_", " ").capitalize()
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
        return when (status) {
            is ReferendumStatus.Ongoing.Preparing -> {
                val titleRes = if (status.reason is PreparingReason.WaitingForDeposit) {
                    R.string.referendum_status_waiting_deposit
                } else {
                    R.string.referendum_status_preparing
                }

                ReferendumStatusModel(
                    name = resourceManager.getString(titleRes),
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
            is ReferendumStatus.Ongoing.Rejecting -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_not_passing),
                colorRes = R.color.text_negative
            )
            is ReferendumStatus.Ongoing.Confirming -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_passing),
                colorRes = R.color.text_positive
            )
            is ReferendumStatus.Approved -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_approved),
                colorRes = R.color.text_positive
            )
            ReferendumStatus.Executed -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_executed),
                colorRes = R.color.text_positive
            )
            ReferendumStatus.NotExecuted.Rejected -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_rejected),
                colorRes = R.color.text_negative
            )
            ReferendumStatus.NotExecuted.Cancelled -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_cancelled),
                colorRes = R.color.text_secondary
            )
            ReferendumStatus.NotExecuted.TimedOut -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_timeout),
                colorRes = R.color.text_secondary
            )
            ReferendumStatus.NotExecuted.Killed -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_killed),
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
            is ReferendumStatus.Ongoing.Rejecting -> ReferendumTimeEstimation.Timer(
                time = status.rejectIn,
                timeFormat = R.string.referendum_status_time_reject_in,
                textStyleRefresher = status.rejectIn.referendumStatusStyleRefresher()
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

    override fun formatUserVote(vote: AccountVote, token: Token): YourVoteModel? {
        val isAye = vote.isAye() ?: return null
        val votes = vote.votes(token.configuration) ?: return null

        val voteTypeRes = if (isAye) R.string.referendum_vote_aye else R.string.referendum_vote_nay
        val colorRes = if (isAye) R.color.text_positive else R.color.text_negative

        val votesAmountFormatted = mapAmountToAmountModel(votes.amount, token).token
        val multiplierFormatted = votes.multiplier.format()

        val votesFormatted = resourceManager.getString(R.string.referendum_votes_format, votes.total.format())
        val votesDetails = "$votesAmountFormatted × ${multiplierFormatted}x"

        return YourVoteModel(
            voteTypeTitleRes = voteTypeRes,
            voteTypeColorRes = colorRes,
            votes = votesFormatted,
            votesDetails = votesDetails
        )
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
            track = referendum.track?.let { formatTrack(it, token.configuration) },
            number = formatId(referendum.id),
            voting = referendum.voting?.let { formatVoting(it, token) },
            yourVote = mapReferendumVoteToUi(referendum.referendumVote, token, chain)
        )
    }

    private fun mapReferendumNameToUi(referendum: ReferendumPreview): String {
        return referendum.offChainMetadata?.title
            ?: mapReferendumOnChainNameToUi(referendum)
            ?: formatUnknownReferendumTitle(referendum.id)
    }

    private fun mapReferendumOnChainNameToUi(referendum: ReferendumPreview): String? {
        return when (val proposal = referendum.onChainMetadata?.proposal) {
            is ReferendumProposal.Call -> formatOnChainName(proposal.call)
            else -> null
        }
    }

    private fun mapReferendumVoteToUi(
        vote: ReferendumVote?,
        token: Token,
        chain: Chain
    ): YourVotePreviewModel? {
        val isAye = vote?.vote?.isAye() ?: return null
        val votes = vote.vote.votes(token.configuration) ?: return null

        val voteTypeRes = if (isAye) R.string.referendum_vote_aye else R.string.referendum_vote_nay
        val colorRes = if (isAye) R.color.text_positive else R.color.text_negative
        val amountFormatted = votes.total.format()

        val details = when (vote) {
            is ReferendumVote.Account -> {
                val accountFormatted = vote.whoIdentity?.name ?: chain.addressOf(vote.who)

                resourceManager.getString(R.string.referendum_other_votes, amountFormatted, accountFormatted)
            }

            is ReferendumVote.User -> {
                resourceManager.getString(R.string.referendum_your_vote_format, amountFormatted)
            }
        }

        return YourVotePreviewModel(
            voteType = resourceManager.getString(voteTypeRes),
            colorRes = colorRes,
            details = details
        )
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
}
