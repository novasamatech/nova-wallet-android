package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.DelegatorVote
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.AddDelegationValidationFailure
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.AddDelegationValidationPayload
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetails
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.description
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.formatDelegationsOverviewOrNull
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsModel.Metadata
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsModel.Stats
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsModel.VotesModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.view.YourDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.DefaultCharacterLimit
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DelegateDetailsViewModel(
    private val interactor: DelegateDetailsInteractor,
    private val payload: DelegateDetailsPayload,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val identityMixinFactory: IdentityMixin.Factory,
    private val router: GovernanceRouter,
    private val delegateMappers: DelegateMappers,
    private val governanceSharedState: GovernanceSharedState,
    private val trackFormatter: TrackFormatter,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    val markwon: Markwon,
) : BaseViewModel(), ExternalActions.Presentation by externalActions, Validatable by validationExecutor {

    val identityMixin = identityMixinFactory.create()

    private val _showTracksEvent = MutableLiveData<Event<List<TrackDelegationModel>>>()
    val showTracksEvent: LiveData<Event<List<TrackDelegationModel>>> = _showTracksEvent

    private val delegateDetailsFlow = interactor.delegateDetailsFlow(payload.accountId)
        .withLoadingShared()
        .shareWhileSubscribed()

    val delegateDetailsLoadingState = delegateDetailsFlow.mapLoading { delegateDetails ->
        val (chain, chainAsset) = governanceSharedState.chainAndAsset()

        mapDelegateDetailsToUi(delegateDetails, chain, chainAsset)
    }
        .shareWhileSubscribed()

    private val trackDelegationModels = delegateDetailsFlow.mapLoading {
        val chainAsset = governanceSharedState.chainAsset()

        it.userDelegations.map { (track, delegation) ->
            delegateMappers.formatTrackDelegation(delegation, track, chainAsset)
        }
    }.shareWhileSubscribed()

    val addDelegationButtonState = delegateDetailsFlow.map { state ->
        val data = state.dataOrNull

        when {
            data == null -> DescriptiveButtonState.Gone
            data.userDelegations.isNotEmpty() -> DescriptiveButtonState.Gone
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.delegation_add_delegation))
        }
    }.shareWhileSubscribed()

    init {
        useIdentity()
    }

    fun backClicked() {
        router.back()
    }

    fun tracksClicked() = launch {
        val trackDelegationModels = trackDelegationModels.firstLoaded()

        _showTracksEvent.value = trackDelegationModels.event()
    }

    fun editDelegationClicked() {
        openNewDelegation(editMode = true)
    }

    fun revokeDelegationClicked() {
        val nextPayload = RevokeDelegationChooseTracksPayload(payload.accountId)
        router.openRevokeDelegationChooseTracks(nextPayload)
    }

    fun accountActionsClicked() = launch {
        val address = delegateDetailsLoadingState.firstLoaded().addressModel.address
        val chain = governanceSharedState.chain()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }

    fun delegationsClicked() {
        router.openDelegateDelegators(DelegateDelegatorsPayload(payload.accountId))
    }

    fun recentVotesClicked() {
        openVotedReferenda(onlyRecentVotes = true, title = delegateMappers.formattedRecentVotesPeriod(R.string.delegation_recent_votes_format))
    }

    fun allVotesClicked() {
        openVotedReferenda(onlyRecentVotes = false)
    }

    fun addDelegationClicked() {
        openNewDelegation(editMode = false)
    }

    fun readMoreClicked() = launch {
        val delegateMetadata = delegateDetailsFlow.first().dataOrNull?.metadata
        val description = delegateMetadata?.description ?: return@launch

        val descriptionPayload = DescriptionPayload(
            description = description,
            toolbarTitle = delegateMetadata.name
        )

        router.openDelegateFullDescription(descriptionPayload)
    }

    private fun useIdentity() = launch {
        val identity = delegateDetailsFlow.firstLoaded().onChainIdentity
        identityMixin.setIdentity(identity)
    }

    private suspend fun mapDelegateDetailsToUi(
        delegateDetails: DelegateDetails,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): DelegateDetailsModel {
        return DelegateDetailsModel(
            addressModel = createDelegateAddressModel(delegateDetails, chain),
            metadata = createDelegateMetadata(delegateDetails, chain),
            stats = formatDelegationStats(delegateDetails.stats, chainAsset),
            userDelegation = formatYourDelegation(delegateDetails.userDelegations, chainAsset)
        )
    }

    private suspend fun createDelegateAddressModel(delegateDetails: DelegateDetails, chain: Chain): AddressModel {
        val willShowNameOnTop = delegateDetails.metadata != null

        val addressModelName = if (willShowNameOnTop) {
            null
        } else {
            delegateDetails.onChainIdentity?.display
        }

        return iconGenerator.createAccountAddressModel(chain, delegateDetails.accountId, addressModelName)
    }

    private suspend fun createDelegateMetadata(delegateDetails: DelegateDetails, chain: Chain): Metadata {
        return Metadata(
            name = delegateMappers.formatDelegateName(delegateDetails.metadata, delegateDetails.onChainIdentity?.display, delegateDetails.accountId, chain),
            icon = delegateMappers.mapDelegateIconToUi(delegateDetails.accountId, delegateDetails.metadata),
            accountType = delegateMappers.mapDelegateTypeToUi(delegateDetails.metadata?.accountType),
            description = createDelegateDescription(delegateDetails.metadata)
        )
    }

    private suspend fun formatYourDelegation(votes: Map<Track, Voting.Delegating>, chainAsset: Chain.Asset): YourDelegationModel? {
        if (votes.isEmpty()) return null

        val delegatorVote = DelegatorVote(votes.values, chainAsset)

        return YourDelegationModel(
            trackSummary = trackFormatter.formatTracksSummary(votes.keys, chainAsset),
            vote = delegateMappers.formatDelegationsOverviewOrNull(delegatorVote, chainAsset)
        )
    }

    private suspend fun formatDelegationStats(stats: DelegateDetails.Stats?, chainAsset: Chain.Asset): Stats? {
        if (stats == null) return null

        return Stats(
            delegations = VotesModel(
                votes = stats.delegationsCount.format(),
                extraInfoAvalable = stats.delegationsCount > 0,
                customLabel = null
            ),
            delegatedVotes = chainAsset.amountFromPlanks(stats.delegatedVotes).format(),
            recentVotes = VotesModel(
                votes = stats.recentVotes.format(),
                extraInfoAvalable = stats.recentVotes > 0,
                customLabel = delegateMappers.formattedRecentVotesPeriod(R.string.delegation_recent_votes_format),
            ),
            allVotes = VotesModel(
                votes = stats.allVotes.format(),
                extraInfoAvalable = stats.allVotes > 0,
                customLabel = null
            )
        )
    }

    private fun createDelegateDescription(metadata: DelegateDetails.Metadata?): ShortenedTextModel? {
        val description = metadata?.description ?: return null
        val markdownParsed = markwon.toMarkdown(description)
        return ShortenedTextModel.from(markdownParsed, DefaultCharacterLimit.SHORT_PARAGRAPH)
    }

    private fun openNewDelegation(editMode: Boolean) = launch {
        val chain = governanceSharedState.chain()
        val metaAccount = selectedAccountUseCase.getSelectedMetaAccount()
        val validationPayload = AddDelegationValidationPayload(chain, metaAccount)

        validationExecutor.requireValid(
            validationSystem = interactor.validationSystemFor(),
            payload = validationPayload,
            validationFailureTransformerCustom = { status, _ -> mapValidationFailureToUi(status.reason) }
        ) {
            val nextPayload = NewDelegationChooseTracksPayload(payload.accountId, editMode)
            router.openNewDelegationChooseTracks(nextPayload)
        }
    }

    private fun openVotedReferenda(onlyRecentVotes: Boolean, title: String? = null) {
        val votedReferendaPayload = VotedReferendaPayload(payload.accountId, onlyRecentVotes, overriddenTitle = title)
        router.openVotedReferenda(votedReferendaPayload)
    }

    private fun mapValidationFailureToUi(failure: AddDelegationValidationFailure): TransformedFailure {
        return when (failure) {
            is AddDelegationValidationFailure.NoChainAccountFailure -> handleChainAccountNotFound(
                failure = failure,
                resourceManager = resourceManager,
                goToWalletDetails = { router.openWalletDetails(failure.account.id) },
                addAccountDescriptionRes = R.string.add_delegation_missing_account_message
            )
        }
    }
}
