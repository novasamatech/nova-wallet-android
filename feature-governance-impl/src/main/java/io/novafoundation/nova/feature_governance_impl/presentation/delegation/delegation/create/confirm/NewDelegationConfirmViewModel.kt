package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.chooseChooseDelegationAmountValidationFailure
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common.newDelegationHints
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.formatTracks
import io.novafoundation.nova.feature_governance_impl.presentation.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.voters.formatConvictionVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewDelegationConfirmViewModel(
    private val router: GovernanceRouter,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val interactor: NewDelegationChooseAmountInteractor,
    private val trackFormatter: TrackFormatter,
    private val assetUseCase: AssetUseCase,
    private val payload: NewDelegationConfirmPayload,
    private val validationSystem: ChooseDelegationAmountValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    private val votersFormatter: VotersFormatter,
    private val tracksUseCase: TracksUseCase,
    private val delegateFormatters: DelegateMappers,
    private val delegateLabelUseCase: DelegateLabelUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    WithFeeLoaderMixin,
    ExternalActions by externalActions {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    override val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val hintsMixin = resourcesHintsMixinFactory.newDelegationHints(viewModelScope)

    val walletModel: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map {
        mapAmountToAmountModel(payload.amount, it)
    }.shareInBackground()

    val currentAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { governanceSharedState.chain() }
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val delegateAssistantFlow = interactor.delegateAssistantFlow(viewModelScope)

    private val convictionVote = payload.convictionVote

    val delegationModel = flowOf {
        votersFormatter.formatConvictionVote(convictionVote, governanceSharedState.chainAsset())
    }.shareInBackground()

    val locksChangeUiFlow = combine(delegateAssistantFlow, assetFlow) { delegateAssistant, asset ->
        val amountPlanks = asset.token.planksFromAmount(payload.amount)
        val locksChange = delegateAssistant.estimateLocksAfterDelegating(amountPlanks, payload.conviction, asset)

        locksChangeFormatter.mapLocksChangeToUi(locksChange, asset, displayPeriodFromWhenSame = false)
    }
        .shareInBackground()

    val tracksModelFlow = flowOf { tracksUseCase.tracksOf(payload.trackIds) }
        .map { tracks ->
            val chainAsset = governanceSharedState.chainAsset()
            trackFormatter.formatTracks(tracks, chainAsset)
        }
        .shareInBackground()

    val delegateLabelModel = flowOf { delegateLabelUseCase.getDelegateLabel(payload.delegate) }
        .map { delegateFormatters.formatDelegateLabel(it, governanceSharedState.chain()) }
        .withSafeLoading()
        .shareInBackground()

    private val _showTracksEvent = MutableLiveData<Event<List<TrackModel>>>()
    val showTracksEvent: LiveData<Event<List<TrackModel>>> = _showTracksEvent

    init {
        setFee()
    }

    fun accountClicked() = launch {
        val addressModel = currentAddressModelFlow.first()
        val type = ExternalActions.Type.Address(addressModel.address)

        externalActions.showExternalActions(type, governanceSharedState.chain())
    }

    fun delegateClicked() = launch {
        val addressModel = delegateLabelModel.firstLoaded().addressModel
        val type = ExternalActions.Type.Address(addressModel.address)

        externalActions.showExternalActions(type, governanceSharedState.chain())
    }

    fun tracksClicked() = launch {
        val trackModels = tracksModelFlow.first()
        _showTracksEvent.value = trackModels.tracks.event()
    }

    fun confirmClicked() = launch {
        val asset = assetFlow.first()
        val amountPlanks = asset.token.planksFromAmount(payload.amount)
        val validationPayload = ChooseDelegationAmountValidationPayload(
            asset = asset,
            fee = payload.fee,
            amount = payload.amount,
            delegate = payload.delegate
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { chooseChooseDelegationAmountValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
        ) {
            performDelegate(amountPlanks)
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun setFee() = launch {
        originFeeMixin.setFee(payload.fee)
    }

    private fun performDelegate(amountPlanks: Balance) = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.delegate(
                amount = amountPlanks,
                conviction = payload.conviction,
                delegate = payload.delegate,
                tracks = payload.trackIds,
                shouldRemoveOtherTracks = payload.shouldRemoveOtherTracks
            )
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))
            router.finishDelegateFlow()
        }
            .onFailure(::showError)

        _showNextProgress.value = false
    }
}
