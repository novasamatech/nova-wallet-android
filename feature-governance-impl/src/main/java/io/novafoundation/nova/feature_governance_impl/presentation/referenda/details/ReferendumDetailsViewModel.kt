package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Validatable
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
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetails
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions.ReferendumPreVoteValidationFailure
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions.ReferendumPreVoteValidationPayload
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions.ReferendumPreVoteValidationSystem
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.toReferendumId
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.dapp.GovernanceDAppsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumCallModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.DefaultCharacterLimit
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDAppModel
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
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.state.selectedChainFlow
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReferendumDetailsViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumDetailsPayload,
    private val interactor: ReferendumDetailsInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: GovernanceSharedState,
    private val governanceIdentityProviderFactory: GovernanceIdentityProviderFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val referendumFormatter: ReferendumFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val governanceDAppsInteractor: GovernanceDAppsInteractor,
    val markwon: Markwon,
    private val validationSystem: ReferendumPreVoteValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val updateSystem: UpdateSystem,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainFlow = selectedAssetSharedState.selectedChainFlow()

    private val optionalReferendumDetailsFlow = flowOfAll {
        val account = selectedAccount.first()
        val selectedGovernanceOption = selectedAssetSharedState.selectedOption()
        val voterAccountId = account.accountIdIn(selectedGovernanceOption.assetWithChain.chain)

        interactor.referendumDetailsFlow(payload.toReferendumId(), selectedGovernanceOption, voterAccountId)
    }.inBackground()
        .shareWhileSubscribed()

    private val referendumDetailsFlow = optionalReferendumDetailsFlow
        .filterNotNull()
        .shareInBackground()

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
            !payload.allowVoting -> DescriptiveButtonState.Gone
            it.timeline.currentStatus !is ReferendumStatus.Ongoing -> DescriptiveButtonState.Gone
            it.userVote is ReferendumVote.UserDirect -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.vote_revote))
            it.userVote is ReferendumVote.UserDelegated || it.userVote == null -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.vote_vote))
            else -> DescriptiveButtonState.Gone
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

    val referendumDApps = flowOfAll {
        val selectedGovernanceOption = selectedAssetSharedState.selectedOption()
        governanceDAppsInteractor.observeReferendumDapps(payload.toReferendumId(), selectedGovernanceOption)
    }
        .mapList(::mapGovernanceDAppToUi)
        .shareInBackground()

    val referendumNotAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    init {
        optionalReferendumDetailsFlow
            .onEach {
                if (it == null) {
                    showErrorAndCloseScreen()
                }
            }.launchIn(this)

        updateSystem.start()
            .launchIn(this)
    }

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
        val referendumDescription = mapReferendumDescriptionToUi(referendumDetailsFlow.first())
        val payload = DescriptionPayload(description = referendumDescription, title = referendumTitle)
        router.openReferendumDescription(payload)
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

    fun dAppClicked(dAppModel: ReferendumDAppModel) {
        router.openDAppBrowser(dAppModel.referendumUrl)
    }

    fun fullDetailsClicked() = launch {
        val payload = constructFullDetailsPayload()
        router.openReferendumFullDetails(payload)
    }

    fun voteClicked() = launch {
        val validationPayload = ReferendumPreVoteValidationPayload(
            metaAccount = selectedAccount.first(),
            chain = selectedChainFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformerCustom = { status, _ -> mapValidationFailureToUi(status.reason) },
        ) {
            val votePayload = SetupVoteReferendumPayload(payload.referendumId)
            router.openSetupVoteReferendum(votePayload)
        }
    }

    private suspend fun mapReferendumDetailsToUi(referendumDetails: ReferendumDetails): ReferendumDetailsModel {
        val timeEstimation = referendumFormatter.formatTimeEstimation(referendumDetails.timeline.currentStatus)
        val (chain, chainAsset) = selectedAssetSharedState.chainAndAsset()
        val token = tokenFlow.first()

        return ReferendumDetailsModel(
            track = referendumDetails.track?.let { referendumFormatter.formatReferendumTrack(it, chainAsset) },
            number = referendumFormatter.formatId(referendumDetails.id),
            title = mapReferendumTitleToUi(referendumDetails),
            description = mapShortenedMarkdownDescription(referendumDetails),
            voting = referendumDetails.voting?.let { referendumFormatter.formatVoting(it, token) },
            statusModel = referendumFormatter.formatStatus(referendumDetails.timeline.currentStatus),
            yourVote = referendumDetails.userVote?.let { referendumFormatter.formatUserVote(it, chain, chainAsset) },
            ayeVoters = mapVotersToUi(referendumDetails.voting, VoteType.AYE, chainAsset),
            nayVoters = mapVotersToUi(referendumDetails.voting, VoteType.NAY, chainAsset),
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
            is ReferendumStatus.Ongoing.Deciding -> R.string.referendum_timeline_state_deciding
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
        val formattedData = entry.at?.let(resourceManager::formatDateTime)
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
                voteTypeColorRes = R.color.aye_indicator,
                voteTypeRes = R.string.referendum_vote_aye,
                votesValue = formatVotesAmount(voting.approval.ayeVotes.amount, chainAsset)
            )

            VoteType.NAY -> VotersModel(
                voteTypeColorRes = R.color.nay_indicator,
                voteTypeRes = R.string.referendum_vote_nay,
                votesValue = formatVotesAmount(voting.approval.nayVotes.amount, chainAsset)
            )

            VoteType.ABSTAIN -> null
        }
    }

    private fun formatVotesAmount(planks: Balance, chainAsset: Chain.Asset): String {
        val amount = chainAsset.amountFromPlanks(planks)

        return resourceManager.getString(R.string.referendum_votes_format, amount.format())
    }

    private fun mapReferendumDescriptionToUi(referendumDetails: ReferendumDetails): String {
        return referendumDetails.offChainMetadata?.description
            ?: resourceManager.getString(R.string.referendum_description_fallback)
    }

    private fun mapShortenedMarkdownDescription(referendumDetails: ReferendumDetails): ShortenedTextModel {
        val referendumDescription = mapReferendumDescriptionToUi(referendumDetails)
        val markdownDescription = markwon.toMarkdown(referendumDescription)
        return ShortenedTextModel.from(markdownDescription, DefaultCharacterLimit.SHORT_PARAGRAPH)
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

    private fun mapGovernanceDAppToUi(referendumDApp: ReferendumDApp): ReferendumDAppModel {
        return ReferendumDAppModel(
            name = referendumDApp.name,
            iconUrl = referendumDApp.iconUrl,
            description = referendumDApp.details,
            referendumUrl = referendumDApp.referendumUrl
        )
    }

    private suspend fun constructFullDetailsPayload(): ReferendumFullDetailsPayload = withContext(Dispatchers.Default) {
        val referendumDetails = referendumDetailsFlow.first()
        val referendumCall = referendumCallFlow.first()

        ReferendumFullDetailsPayload(
            proposer = referendumDetails.proposer?.let {
                ReferendumProposerPayload(it.accountId, it.offChainNickname)
            },
            voteThreshold = referendumDetails.fullDetails.voteThreshold?.readableName,
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
            referendumDetails.fullDetails.voteThreshold,
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

    private fun mapValidationFailureToUi(failure: ReferendumPreVoteValidationFailure): TransformedFailure {
        return when (failure) {
            is ReferendumPreVoteValidationFailure.NoRelaychainAccount -> handleChainAccountNotFound(
                failure = failure,
                resourceManager = resourceManager,
                goToWalletDetails = { router.openWalletDetails(failure.account.id) },
                addAccountDescriptionRes = R.string.referendum_missing_account_message
            )
        }
    }

    private suspend fun showErrorAndCloseScreen() {
        val confirmationInfo = ConfirmationDialogInfo.titleAndButton(
            title = R.string.referendim_details_not_found_title,
            button = R.string.common_ok,
        )
        referendumNotAwaitableAction.awaitAction(confirmationInfo)
        router.back()
    }
}
