package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.formatting.remainingTime
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isAye
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason.DecidingIn
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason.WaitingForDeposit
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.passes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimationStyleRefresher
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVoteModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class ReferendaListViewModel(
    assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
    private val referendaListInteractor: ReferendaListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: SingleAssetSharedState,
    private val resourceManager: ResourceManager,
    private val updateSystem: UpdateSystem,
) : BaseViewModel(), WithAssetSelector {

    private val oneDay = 1.days

    override val assetSelectorMixin = assetSelectorFactory.create(scope = this)

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainFlow = selectedAssetSharedState.selectedChainFlow()

    private val accountAndChainFlow = combineToPair(selectedAccount, selectedChainFlow)

    private val referendaFlow = accountAndChainFlow.withLoading { (account, chain) ->
        referendaListInteractor.referendaFlow(account, chain)
    }

    val referendaUiFlow = referendaFlow.mapLoading { groupedReferenda ->
        val asset = assetSelectorMixin.selectedAssetFlow.first()

        groupedReferenda.toListWithHeaders(
            keyMapper = { group, referenda -> mapReferendumGroupToUi(group, referenda.size) },
            valueMapper = { mapReferendumPreviewToUi(it, asset.token) }
        )
    }
        .shareInBackground()

    init {
        updateSystem.start()
            .launchIn(this)
    }

    private fun mapReferendumGroupToUi(referendumGroup: ReferendumGroup, groupSize: Int): ReferendaGroupModel {
        val nameRes = when (referendumGroup) {
            ReferendumGroup.ONGOING -> R.string.common_ongoing
            ReferendumGroup.COMPLETED -> R.string.common_completed
        }

        return ReferendaGroupModel(
            name = resourceManager.getString(nameRes),
            badge = groupSize.format()
        )
    }

    private fun mapReferendumPreviewToUi(referendum: ReferendumPreview, token: Token): ReferendumModel {
        return ReferendumModel(
            id = referendum.id,
            status = mapReferendumStatusToUi(referendum.status),
            name = referendum.offChainMetadata?.title
                ?: referendum.onChainMetadata?.proposalHash
                ?: resourceManager.getString(R.string.referendum_name_unknown),
            timeEstimation = maReferendumTimeEstimationToUi(referendum.status),
            track = referendum.track?.let(::mapReferendumTrackToUi),
            number = mapReferendumIdToUi(referendum.id),
            voting = referendum.voting?.let { mapReferendumVotingToUi(it, token) },
            yourVote = mapUserVoteToUi(referendum.userVote, token)
        )
    }

    private fun mapReferendumIdToUi(id: ReferendumId): String {
        return "#${id.value.format()}"
    }

    private fun mapUserVoteToUi(vote: AccountVote?, token: Token): YourVoteModel? {
        val isAye = vote?.isAye() ?: return null
        val votes = vote.votes(token.configuration) ?: return null

        val voteTypeRes = if (isAye) R.string.referendum_vote_positive_type else R.string.referendum_vote_negative_type
        val colorRes = if (isAye) R.color.multicolor_green_100 else R.color.multicolor_red_100

        return YourVoteModel(
            voteType = resourceManager.getString(voteTypeRes),
            colorRes = colorRes,
            details = resourceManager.getString(R.string.referendum_your_vote_format, votes.format())
        )
    }

    private fun mapReferendumVotingToUi(voting: ReferendumVoting, token: Token): ReferendumVotingModel {
        return ReferendumVotingModel(
            positiveFraction = voting.approval.ayeFraction.toFloat(),
            thresholdFraction = voting.approval.threshold.toFloat(),
            votingResultIcon = if (voting.support.passes()) R.drawable.ic_checkmark else R.drawable.ic_close,
            votingResultIconColor = if (voting.support.passes()) R.color.multicolor_green_100 else R.color.multicolor_red_100,
            thresholdInfo = formatThresholdInfo(voting.support, token),
            positivePercentage = resourceManager.getString(
                R.string.referendum_aye_format,
                voting.approval.ayeFraction.formatFractionAsPercentage()
            ),
            negativePercentage = resourceManager.getString(
                R.string.referendum_nay_format,
                voting.approval.nayFraction.formatFractionAsPercentage()
            ),
            thresholdPercentage = resourceManager.getString(
                R.string.referendum_to_pass_format,
                voting.approval.threshold.formatFractionAsPercentage()
            )
        )
    }

    private fun formatThresholdInfo(
        support: ReferendumVoting.Support,
        token: Token
    ): String {
        val thresholdFormatted = mapAmountToAmountModel(support.threshold, token).token
        val turnoutFormatted = mapAmountToAmountModel(support.turnout, token).token

        return resourceManager.getString(R.string.referendum_support_threshold_format, turnoutFormatted, thresholdFormatted)
    }

    private fun mapReferendumTrackToUi(track: ReferendumPreview.Track): ReferendumTrackModel {
        return when (track.name) {
            "root" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_root),
                iconRes = R.drawable.ic_block
            )
            "whitelisted_caller" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_whitelisted_caller),
                iconRes = R.drawable.ic_users
            )
            "staking_admin" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_staking_admin),
                iconRes = R.drawable.ic_staking_filled
            )
            "treasurer" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_treasurer),
                iconRes = R.drawable.ic_gem
            )
            "lease_admin" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_lease_admin),
                iconRes = R.drawable.ic_governance_check_to_slot
            )
            "fellowship_admin" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_fellowship_admin),
                iconRes = R.drawable.ic_users
            )
            "general_admin" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_general_admin),
                iconRes = R.drawable.ic_governance_check_to_slot
            )
            "auction_admin" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_auction_admin),
                iconRes = R.drawable.ic_rocket
            )
            "referendum_canceller" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_canceller),
                iconRes = R.drawable.ic_governance_check_to_slot
            )
            "referendum_killer" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_referendum_killer),
                iconRes = R.drawable.ic_governance_check_to_slot
            )
            "small_tipper" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_tipper),
                iconRes = R.drawable.ic_gem
            )
            "big_tipper" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_tipper),
                iconRes = R.drawable.ic_gem
            )
            "small_spender" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_small_spender),
                iconRes = R.drawable.ic_gem
            )
            "medium_spender" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_medium_spender),
                iconRes = R.drawable.ic_gem
            )
            "big_spender" -> ReferendumTrackModel(
                name = resourceManager.getString(R.string.referendum_track_big_spender),
                iconRes = R.drawable.ic_gem
            )
            else -> ReferendumTrackModel(
                name = mapUnknownTrackNameToUi(track.name),
                iconRes = R.drawable.ic_block
            )
        }
    }

    private fun mapUnknownTrackNameToUi(name: String): String {
        return name.replace("_", " ")
    }

    private fun maReferendumTimeEstimationToUi(status: ReferendumStatus): ReferendumTimeEstimation? {
        return when (status) {
            is ReferendumStatus.Preparing -> {
                when (val reason = status.reason) {
                    is DecidingIn -> ReferendumTimeEstimation.Timer(
                        time = reason.timeLeft,
                        timeFormat = R.string.referendum_status_deciding_in,
                        textStyleRefresher = reason.timeLeft.referendumStatusStyleRefresher()
                    )
                    WaitingForDeposit -> ReferendumTimeEstimation.Text(
                        text = resourceManager.getString(R.string.referendum_status_waiting_deposit),
                        textStyle = ReferendumTimeEstimation.TextStyle.regular()
                    )
                }
            }
            is ReferendumStatus.InQueue -> ReferendumTimeEstimation.Timer(
                time = status.timeOutIn,
                timeFormat = R.string.referendum_status_deciding_in,
                textStyleRefresher = status.timeOutIn.referendumStatusStyleRefresher()
            )
            is ReferendumStatus.Deciding -> ReferendumTimeEstimation.Timer(
                time = status.rejectIn,
                timeFormat = R.string.referendum_status_time_reject_in,
                textStyleRefresher = status.rejectIn.referendumStatusStyleRefresher()
            )
            is ReferendumStatus.Confirming -> ReferendumTimeEstimation.Timer(
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

    private fun mapReferendumStatusToUi(status: ReferendumStatus): ReferendumStatusModel {
        return when (status) {
            is ReferendumStatus.Preparing -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_preparing),
                colorRes = R.color.white_64
            )
            is ReferendumStatus.InQueue -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_in_queue),
                colorRes = R.color.white_64
            )
            is ReferendumStatus.Deciding -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_not_passing),
                colorRes = R.color.multicolor_red_100
            )
            is ReferendumStatus.Confirming -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_passing),
                colorRes = R.color.multicolor_green_100
            )
            is ReferendumStatus.Approved -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_approved),
                colorRes = R.color.multicolor_green_100
            )
            ReferendumStatus.Executed -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_executed),
                colorRes = R.color.multicolor_green_100
            )
            ReferendumStatus.NotExecuted.Rejected -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_rejected),
                colorRes = R.color.multicolor_red_100
            )
            ReferendumStatus.NotExecuted.Cancelled -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_cancelled),
                colorRes = R.color.white_64
            )
            ReferendumStatus.NotExecuted.TimedOut -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_timeout),
                colorRes = R.color.white_64
            )
            ReferendumStatus.NotExecuted.Killed -> ReferendumStatusModel(
                name = resourceManager.getString(R.string.referendum_status_killed),
                colorRes = R.color.multicolor_red_100
            )
        }
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
        colorRes = R.color.multicolor_yellow_100
    )

    private fun ReferendumTimeEstimation.TextStyle.Companion.regular() = ReferendumTimeEstimation.TextStyle(
        iconRes = R.drawable.ic_time_16,
        colorRes = R.color.white_64
    )

    private fun TimerValue.referendumStatusIsHot(): Boolean {
        return remainingTime().milliseconds < oneDay
    }
}
