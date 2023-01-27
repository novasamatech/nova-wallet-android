package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes.RemoveTrackVotesInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.validations.RemoteVotesValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.validations.RemoveVotesValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.validations.handleRemoveVotesValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RemoveVotesViewModel(
    private val interactor: RemoveTrackVotesInteractor,
    private val trackFormatter: TrackFormatter,
    private val payload: RemoveVotesPayload,
    private val governanceSharedState: GovernanceSharedState,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val assetUseCase: AssetUseCase,
    private val walletUiUseCase: WalletUiUseCase,
    private val accountUseCase: SelectedAccountUseCase,
    private val router: GovernanceRouter,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RemoteVotesValidationSystem,
    private val resourceManager: ResourceManager,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    override val originFeeMixin = feeLoaderMixinFactory.create(assetFlow)

    private val tracksFlow = flowOf { interactor.trackInfosOf(payload.trackIds) }
        .shareInBackground()

    private val trackModels = tracksFlow
        .map { tracks ->
            val chainAsset = governanceSharedState.chainAsset()
            tracks.map { trackFormatter.formatTrack(it, chainAsset) }
        }

    val tracksSummary = tracksFlow.map { tracks ->
        val chainAsset = governanceSharedState.chainAsset()
        trackFormatter.formatTracksSummary(tracks, chainAsset)
    }.shareInBackground()

    val walletModel = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val selectedAccount = accountUseCase.selectedAddressModelFlow { governanceSharedState.chain() }
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    init {
        loadFee()
    }

    fun confirmClicked() {
        removeVotesIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun accountClicked() = launch {
        val addressModel = selectedAccount.first()
        val type = ExternalActions.Type.Address(addressModel.address)

        externalActions.showExternalActions(type, governanceSharedState.chain())
    }

    private fun removeVotesIfValid(): Unit = requireFee { fee ->
        launch {
            val validationPayload = RemoveVotesValidationPayload(fee, assetFlow.first())

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = validationPayload,
                progressConsumer = _showNextProgress.progressConsumer(),
                validationFailureTransformer = { handleRemoveVotesValidationFailure(it, resourceManager) }
            ) {
                removeVotes()
            }
        }

    }

    private fun removeVotes() = launch {
        interactor.removeTrackVotes(payload.trackIds)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.back()
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun loadFee() {
        launch {
            originFeeMixin.loadFee(
                coroutineScope = this,
                feeConstructor = { interactor.calculateFee(payload.trackIds) },
                onRetryCancelled = {}
            )
        }
    }

    fun tracksClicked() {
        showMessage("TODO show tracks")
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = originFeeMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
