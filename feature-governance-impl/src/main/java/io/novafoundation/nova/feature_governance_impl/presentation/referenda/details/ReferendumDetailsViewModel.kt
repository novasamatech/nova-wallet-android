package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.firstOnLoad
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.mapNullable
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.GovernanceDApp
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetails
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumCallModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.GovernanceDAppModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDetailsModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.PreImagePreviewPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumCallPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumProposerPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline.TimelineLayout
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotersModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DESCRIPTION_LENGTH_LIMIT = 180

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
    private val externalActions: ExternalActions.Presentation,
    private val markwon: Markwon
) : BaseViewModel(), ExternalActions by externalActions {

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainFlow = selectedAssetSharedState.selectedChainFlow()

    private val referendumDetailsFlow = flowOfAll {
        val account = selectedAccount.first()
        val chain = selectedChainFlow.first()
        val voterAccountId = account.accountIdIn(chain)

        interactor.referendumDetailsFlow(payload.toReferendumId(), chain, voterAccountId)
    }
        .inBackground()
        .shareWhileSubscribed()

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
        .inBackground()
        .shareWhileSubscribed()

    val referendumDetailsModelFlow = referendumDetailsFlow.map(::mapReferendumDetailsToUi)
        .withLoading()
        .inBackground()
        .shareWhileSubscribed()

    val voteButtonState = referendumDetailsFlow.map {
        when {
            it.timeline.currentStatus !is ReferendumStatus.Ongoing -> DescriptiveButtonState.Gone
            it.userVote != null -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.vote_revote))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.vote_vote))
        }
    }

    val showFullDetails = flowOf { fullDetailsAccessible() }

    private val referendumCallFlow = referendumDetailsFlow.map { details ->
        details.onChainMetadata?.preImage?.let { preImage ->
            interactor.detailsFor(preImage, selectedChainFlow.first())
        }
    }
        .inBackground()
        .shareWhileSubscribed()

    val referendumCallModelFlow = referendumCallFlow.mapNullable(::mapReferendumCallToUi)
        .inBackground()
        .shareWhileSubscribed()

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

    fun readMoreClicked() = launch {
        val referendumTitle = referendumDetailsModelFlow.firstOnLoad().title
        val referendumDescription = referendumDetailsFlow.first().offChainMetadata?.description
        if (referendumDescription != null) {
            val payload = ReferendumDescriptionPayload(referendumTitle, referendumDescription)
            router.openReferendumDescription(payload)
        }
    }

    fun positiveVotesClicked() {
        val votersPayload = ReferendumVotersPayload(
            payload.referendumId,
            VoteType.AYE
        )
        router.openReferendumVoters(votersPayload)
    }

    fun negativeVotesClicked() {
        val votersPayload = ReferendumVotersPayload(
            payload.referendumId,
            VoteType.NAY
        )
        router.openReferendumVoters(votersPayload)
    }

    fun dAppClicked(dAppModel: GovernanceDAppModel) {
        val url = dAppModel.urlConstructor.urlFor(payload.toReferendumId())

        router.openDAppBrowser(url)
    }

    fun fullDetailsClicked() = launch {
        val payload = constructFullDetailsPayload()
        router.openReferendumFullDetails(payload)
    }

    fun voteClicked() {
        val votePayload = SetupVoteReferendumPayload(payload.referendumId)
        router.openSetupVoteReferendum(votePayload)
    }

    private suspend fun mapReferendumDetailsToUi(referendumDetails: ReferendumDetails): ReferendumDetailsModel {
        val timeEstimation = referendumFormatter.formatTimeEstimation(referendumDetails.timeline.currentStatus)
        val token = tokenFlow.first()

        return ReferendumDetailsModel(
            track = referendumDetails.track?.let { referendumFormatter.formatTrack(it, token.configuration) },
            number = referendumFormatter.formatId(referendumDetails.id),
            title = mapReferendumTitleToUi(referendumDetails),
            description = mapReferendumDescriptionToUi(referendumDetails),
            voting = referendumDetails.voting?.let { referendumFormatter.formatVoting(it, token) },
            statusModel = referendumFormatter.formatStatus(referendumDetails.timeline.currentStatus),
            yourVote = referendumDetails.userVote?.let { referendumFormatter.formatUserVote(it, token) },
            ayeVoters = mapVotersToUi(referendumDetails.voting, VoteType.AYE, token.configuration),
            nayVoters = mapVotersToUi(referendumDetails.voting, VoteType.NAY, token.configuration),
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
            is ReferendumStatus.Ongoing.Preparing -> {
                when (currentStatus.reason) {
                    is PreparingReason.DecidingIn -> R.string.referendum_timeline_state_preparing
                    PreparingReason.WaitingForDeposit -> R.string.referendum_timeline_state_waiting_deposit
                }
            }
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
        type: VoteType,
        chainAsset: Chain.Asset,
    ): VotersModel? {
        if (voting == null) return null

        return when (type) {
            VoteType.AYE -> VotersModel(
                voteTypeColorRes = R.color.multicolor_green_100,
                voteTypeRes = R.string.referendum_vote_aye,
                votesValue = formatVotesAmount(voting.approval.ayeVotes.amount, chainAsset)
            )
            VoteType.NAY -> VotersModel(
                voteTypeColorRes = R.color.multicolor_red_100,
                voteTypeRes = R.string.referendum_vote_nay,
                votesValue = formatVotesAmount(voting.approval.nayVotes.amount, chainAsset)
            )
        }
    }

    private fun formatVotesAmount(planks: Balance, chainAsset: Chain.Asset): String {
        val amount = chainAsset.amountFromPlanks(planks)

        return resourceManager.getString(R.string.referendum_votes_format, amount.format())
    }

    private fun mapReferendumDescriptionToUi(referendumDetails: ReferendumDetails): ShortenedTextModel? {
        return referendumDetails.offChainMetadata?.description?.let {
            val description = removeMarkdown(it)
            ShortenedTextModel.from(description, DESCRIPTION_LENGTH_LIMIT)
        }
    }

    private fun mapReferendumTitleToUi(referendumDetails: ReferendumDetails): String {
        return referendumDetails.offChainMetadata?.title
            ?: referendumDetails.onChainMetadata?.preImage?.let { referendumFormatter.formatOnChainName(it.call) }
            ?: referendumFormatter.formatUnknownReferendumTitle(referendumDetails.id)
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

    private suspend fun constructFullDetailsPayload(): ReferendumFullDetailsPayload = withContext(Dispatchers.Default) {
        val referendumDetails = referendumDetailsFlow.first()
        val referendumCall = referendumCallFlow.first()

        ReferendumFullDetailsPayload(
            proposer = referendumDetails.proposer?.let {
                ReferendumProposerPayload(it.accountId, it.offChainNickname)
            },

            voteThreshold = null,
            approveThreshold = referendumDetails.fullDetails.approvalCurve?.name,
            supportThreshold = referendumDetails.fullDetails.supportCurve?.name,
            hash = referendumDetails.onChainMetadata?.preImageHash,
            deposit = referendumDetails.fullDetails.deposit,
            turnout = referendumDetails.voting?.support?.turnout,
            electorate = referendumDetails.voting?.support?.electorate,
            referendumCall = ReferendumCallPayload(referendumCall),
            preImage = constructPreimagePreviewPayload(referendumDetails.onChainMetadata?.preImage),
        )
    }

    private suspend fun fullDetailsAccessible(): Boolean {
        val referendumDetails = referendumDetailsFlow.first()
        val referendumCall = referendumCallFlow.first()

        return checkAnyNonNull(
            referendumDetails.proposer,
            referendumDetails.fullDetails.approvalCurve,
            referendumDetails.fullDetails.supportCurve,
            referendumDetails.fullDetails.deposit,
            referendumDetails.onChainMetadata,
            referendumDetails.voting,
            referendumCall
        )
    }

    private fun checkAnyNonNull(vararg args: Any?): Boolean {
        return args.any { it != null }
    }

    private suspend fun constructPreimagePreviewPayload(preImage: PreImage?): PreImagePreviewPayload? {
        return preImage?.let {
            PreImagePreviewPayload(interactor.previewFor(preImage))
        }
    }

    private fun removeMarkdown(value: String): String {
        return markwon.toMarkdown(value)
            .toString()
    }
}
