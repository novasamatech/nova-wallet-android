package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.mapNullable
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isAye
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.GovernanceDApp
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetails
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumCallModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.GovernanceDAppModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDetailsModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline.TimelineLayout
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotersModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourVoteModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val DESCRIPTION_LENGTH_LIMIT = 180

private enum class VotersType {
    AYE, NAY
}

class ReferendumDetailsViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumDetailsPayload,
    private val interactor: ReferendumDetailsInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: SingleAssetSharedState,
    private val governanceIdentityProviderFactory: GovernanceIdentityProviderFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val referendumFormatter: ReferendumFormatter,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(), ExternalActions by externalActions {

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainFlow = selectedAssetSharedState.selectedChainFlow()

    private val referendumDetailsFlow = flowOfAll {
        val account = selectedAccount.first()
        val chain = selectedChainFlow.first()
        val voterAccountId = account.accountIdIn(chain)

        interactor.referendumDetailsFlow(payload.toReferendumId(), chain, voterAccountId)
    }.shareInBackground()

    private val proposerFlow = referendumDetailsFlow.map { it.proposer }
    private val proposerIdentityProvider = governanceIdentityProviderFactory.proposerProvider(proposerFlow)

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .shareInBackground()

    val proposerAddressModel = referendumDetailsFlow.map {
        it.proposer?.let { proposer ->
            addressIconGenerator.createIdentityAddressModel(
                chain = selectedChainFlow.first(),
                accountId = proposer.accountId,
                identityProvider = proposerIdentityProvider
            )
        }
    }

    val referendumDetailsModelFlow = referendumDetailsFlow.map(::mapReferendumDetailsToUi)
        .withLoading()
        .shareInBackground()

    val voteButtonVisible = referendumDetailsFlow.map {
        it.userVote == null && it.timeline.currentStatus is ReferendumStatus.Ongoing
    }.shareInBackground()

    private val referendumCallFlow = referendumDetailsFlow.map { details ->
        details.onChainMetadata?.preImage?.let { preImage ->
            interactor.detailsFor(preImage, selectedChainFlow.first())
        }
    }.shareInBackground()

    val referendumCallModelFlow = referendumCallFlow.mapNullable(::mapReferendumCallToUi)
        .shareInBackground()

    val governanceDApps = selectedChainFlow.map(interactor::getAvailableDApps)
        .mapList(::mapGovernanceDAppToUi)
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun proposerClicked() = launch {
        val proposer = proposerAddressModel.first()?.address ?: return@launch
        val payload = ExternalActions.Type.Address(proposer)

        externalActions.showExternalActions(payload, selectedChainFlow.first())
    }

    fun readMoreClicked() {
        showMessage("TODO - open full description")
    }

    fun positiveVotesClicked() {
        showMessage("TODO - open positive votes")
    }

    fun negativeVotesClicked() {
        showMessage("TODO - open negative votes")
    }

    fun dAppClicked(dAppModel: GovernanceDAppModel) {
        val url = dAppModel.urlConstructor.urlFor(payload.toReferendumId())

        router.openDAppBrowser(url)
    }

    fun fullDetailsClicked() {
        showMessage("TODO - open full details")
    }

    fun voteClicked() {
        showMessage("TODO - open vote")
    }

    private suspend fun mapReferendumDetailsToUi(referendumDetails: ReferendumDetails): ReferendumDetailsModel {
        val timeEstimation = referendumFormatter.formatTimeEstimation(referendumDetails.timeline.currentStatus)
        val token = tokenFlow.first()

        return ReferendumDetailsModel(
            track = referendumDetails.track?.let(referendumFormatter::formatTrack),
            number = referendumFormatter.formatId(referendumDetails.id),
            title = mapReferendumTitleToUi(referendumDetails),
            description = mapReferendumDescriptionToUi(referendumDetails),
            voting = referendumDetails.voting?.let { referendumFormatter.formatVoting(it, token) },
            statusModel = referendumFormatter.formatStatus(referendumDetails.timeline.currentStatus),
            yourVote = referendumDetails.userVote?.let { mapUserVoteToUi(it, token) },
            ayeVoters = mapVotersToUi(referendumDetails.voting, VotersType.AYE, token.configuration),
            nayVoters = mapVotersToUi(referendumDetails.voting, VotersType.NAY, token.configuration),
            timeEstimation = timeEstimation,
            timeline = mapTimelineToUi(referendumDetails.timeline, timeEstimation)
        )
    }

    private fun mapTimelineToUi(
        timeline: ReferendumTimeline,
        currentTimeEstimation: ReferendumTimeEstimation?
    ): TimelineLayout.Timeline {
        val states = buildList {
            val historical = timeline.pastEntries.map(::mapHistoricalTimelineEntryToUi)
            addAll(historical)

            currentTimeEstimation?.let {
                val current = mapCurrentStatusToTimelineEntry(timeline.currentStatus, it)
                add(current)
            }
        }

        return TimelineLayout.Timeline(
            states = states,
            finished = currentTimeEstimation == null
        )
    }

    private fun mapCurrentStatusToTimelineEntry(
        currentStatus: ReferendumStatus,
        currentTimeEstimation: ReferendumTimeEstimation
    ): TimelineLayout.TimelineState {
        val titleRes = when (currentStatus) {
            is ReferendumStatus.Ongoing.Preparing -> R.string.referendum_timeline_state_preparing
            is ReferendumStatus.Ongoing.Confirming -> R.string.referendum_timeline_state_passing
            is ReferendumStatus.Ongoing.Rejecting -> R.string.referendum_timeline_state_not_passing
            is ReferendumStatus.Ongoing.InQueue -> R.string.referendum_timeline_state_in_queue
            is ReferendumStatus.Approved -> R.string.referendum_timeline_state_approved
            else -> null
        }

        return TimelineLayout.TimelineState.Current(
            title = titleRes?.let(resourceManager::getString).orEmpty(),
            subtitle = currentTimeEstimation
        )
    }

    private fun mapHistoricalTimelineEntryToUi(entry: ReferendumTimeline.Entry): TimelineLayout.TimelineState {
        val formattedData = entry.at?.let(resourceManager::formatDate)
        val stateLabelRes = when (entry.state) {
            ReferendumTimeline.State.CREATED -> R.string.referendum_timeline_state_created
            ReferendumTimeline.State.APPROVED -> R.string.referendum_timeline_state_approved
            ReferendumTimeline.State.REJECTED -> R.string.referendum_timeline_state_rejected
            ReferendumTimeline.State.EXECUTED -> R.string.referendum_timeline_state_executed
            ReferendumTimeline.State.CANCELLED -> R.string.referendum_timeline_state_cancelled
            ReferendumTimeline.State.KILLED -> R.string.referendum_timeline_state_killed
            ReferendumTimeline.State.TIMED_OUT -> R.string.referendum_timeline_state_timed_out
        }

        return TimelineLayout.TimelineState.Historical(
            title = resourceManager.getString(stateLabelRes),
            subtitle = formattedData
        )
    }

    private fun mapVotersToUi(
        voting: ReferendumVoting?,
        type: VotersType,
        chainAsset: Chain.Asset,
    ): VotersModel? {
        if (voting == null) return null

        return when (type) {
            VotersType.AYE -> VotersModel(
                voteTypeColorRes = R.color.multicolor_green_100,
                voteTypeRes = R.string.referendum_vote_positive_type,
                votesValue = formatVotesAmount(voting.approval.ayeVotes.amount, chainAsset)
            )
            VotersType.NAY -> VotersModel(
                voteTypeColorRes = R.color.multicolor_red_100,
                voteTypeRes = R.string.referendum_vote_negative_type,
                votesValue = formatVotesAmount(voting.approval.nayVotes.amount, chainAsset)
            )
        }
    }

    private fun formatVotesAmount(planks: Balance, chainAsset: Chain.Asset): String {
        val amount = chainAsset.amountFromPlanks(planks)

        return resourceManager.getString(R.string.referendum_votes_format, amount.format())
    }

    private fun mapUserVoteToUi(vote: AccountVote, token: Token): YourVoteModel? {
        val isAye = vote.isAye() ?: return null
        val votes = vote.votes(token.configuration) ?: return null

        val voteTypeRes = if (isAye) R.string.referendum_vote_positive_type else R.string.referendum_vote_negative_type
        val colorRes = if (isAye) R.color.multicolor_green_100 else R.color.multicolor_red_100

        val votesAmountFormatted = mapAmountToAmountModel(votes.amount, token).token
        val multiplierFormatted = votes.multiplier.format()

        val votesFormatted = resourceManager.getString(R.string.referendum_votes_format, votes.total.format())
        val votesDetails = "$votesAmountFormatted × $multiplierFormatted"

        return YourVoteModel(
            voteTypeTitleRes = voteTypeRes,
            voteTypeColorRes = colorRes,
            votes = votesFormatted,
            votesDetails = votesDetails
        )
    }

    private fun mapReferendumDescriptionToUi(referendumDetails: ReferendumDetails): ShortenedTextModel? {
        return referendumDetails.offChainMetadata?.description?.let {
            ShortenedTextModel.from(it, DESCRIPTION_LENGTH_LIMIT)
        }
    }

    private fun mapReferendumTitleToUi(referendumDetails: ReferendumDetails): String {
        return referendumDetails.offChainMetadata?.title
            ?: referendumDetails.onChainMetadata?.let { referendumFormatter.formatOnChainName(it.preImage.call) }
            ?: referendumFormatter.formatUnknownReferendumTitle()
    }

    private suspend fun mapReferendumCallToUi(referendumCall: ReferendumCall): ReferendumCallModel {
        return when (referendumCall) {
            is ReferendumCall.TreasuryRequest -> {
                val token = tokenFlow.first()

                ReferendumCallModel.GovernanceRequest.AmountOnly(
                    amount = mapAmountToAmountModel(referendumCall.amount, token)
                )
            }
        }
    }

    private fun mapGovernanceDAppToUi(governanceDApp: GovernanceDApp): GovernanceDAppModel {
        return GovernanceDAppModel(
            name = governanceDApp.metadata?.name ?: governanceDApp.urlConstructor.baseUrl,
            iconUrl = governanceDApp.metadata?.iconLink,
            description = resourceManager.getString(R.string.referendum_dapp_comment_react),
            urlConstructor = governanceDApp.urlConstructor
        )
    }
}
