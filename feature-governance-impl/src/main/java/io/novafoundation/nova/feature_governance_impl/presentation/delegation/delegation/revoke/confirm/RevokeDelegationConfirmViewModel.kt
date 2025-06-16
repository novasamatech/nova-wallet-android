package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm

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
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.submissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.RevokeDelegationsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations.RevokeDelegationValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations.RevokeDelegationValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations.handleRevokeDelegationValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.formatDelegationsOverviewOrNull
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common.revokeDelegationHints
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RevokeDelegationConfirmViewModel(
    private val router: GovernanceRouter,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val interactor: RevokeDelegationsInteractor,
    private val trackFormatter: TrackFormatter,
    private val assetUseCase: AssetUseCase,
    private val payload: RevokeDelegationConfirmPayload,
    private val validationSystem: RevokeDelegationValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    private val delegateFormatters: DelegateMappers,
    private val delegateLabelUseCase: DelegateLabelUseCase,
    private val partialRetriableMixinFactory: PartialRetriableMixin.Factory,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
) : BaseViewModel(),
    Validatable by validationExecutor,
    WithFeeLoaderMixin,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    val partialRetriableMixin = partialRetriableMixinFactory.create(this)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    override val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val hintsMixin = resourcesHintsMixinFactory.revokeDelegationHints(viewModelScope)

    val walletModel: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val currentAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { governanceSharedState.chain() }
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    val delegateLabelModel = flowOf { delegateLabelUseCase.getDelegateLabel(payload.delegateId) }
        .map { delegateFormatters.formatDelegateLabel(it.accountId, it.metadata, it.onChainIdentity?.display, governanceSharedState.chain()) }
        .withSafeLoading()
        .shareInBackground()

    private val _showTracksEvent = MutableLiveData<Event<List<TrackDelegationModel>>>()
    val showTracksEvent: LiveData<Event<List<TrackDelegationModel>>> = _showTracksEvent

    private val revokeDelegationData = interactor.revokeDelegationDataFlow(payload.trackIds)
        .shareInBackground()

    private val trackDelegationModelsFlow = revokeDelegationData
        .map { data ->
            val chainAsset = governanceSharedState.chainAsset()
            data.delegations.map { (track, delegation) ->
                delegateFormatters.formatTrackDelegation(delegation, track, chainAsset)
            }
        }
        .shareInBackground()

    val tracksSummary = revokeDelegationData.map {
        val chainAsset = governanceSharedState.chainAsset()
        trackFormatter.formatTracksSummary(it.delegations.keys, chainAsset)
    }.shareInBackground()

    val undelegatingPeriod = revokeDelegationData
        .map { resourceManager.formatDuration(it.undelegatingPeriod, estimated = false) }
        .withSafeLoading()
        .shareInBackground()

    val userDelegation = revokeDelegationData.map {
        val chainAsset = governanceSharedState.chainAsset()
        delegateFormatters.formatDelegationsOverviewOrNull(it.delegationsOverview, chainAsset)
    }.shareInBackground()

    init {
        loadFee()
    }

    fun accountClicked() = launch {
        val addressModel = currentAddressModelFlow.first()

        externalActions.showAddressActions(addressModel.address, governanceSharedState.chain())
    }

    fun delegateClicked() = launch {
        val address = delegateLabelModel.firstLoaded().address

        externalActions.showAddressActions(address, governanceSharedState.chain())
    }

    fun tracksClicked() = launch {
        val trackModels = trackDelegationModelsFlow.first()
        _showTracksEvent.value = trackModels.event()
    }

    fun confirmClicked() = launch {
        _showNextProgress.value = true

        val validationPayload = RevokeDelegationValidationPayload(
            fee = originFeeMixin.awaitFee(),
            asset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { handleRevokeDelegationValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
        ) {
            performDelegate()
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun loadFee() = launch {
        originFeeMixin.loadFee(
            coroutineScope = coroutineScope,
            feeConstructor = { interactor.calculateFee(payload.trackIds) },
            onRetryCancelled = {}
        )
    }

    private fun performDelegate() = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.revokeDelegations(payload.trackIds)
        }

        partialRetriableMixin.handleMultiResult(
            multiResult = result,
            onSuccess = {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy()) { router.backToYourDelegations() }
            },
            progressConsumer = _showNextProgress.progressConsumer(),
            onRetryCancelled = { router.backToYourDelegations() }
        )
    }
}
