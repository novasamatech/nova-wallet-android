package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.ParachainStakingRebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.ParachainStakingRebondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.ParachainStakingRebondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ParachainStakingRebondViewModel(
    private val router: ParachainStakingRouter,
    private val resourceManager: ResourceManager,
    private val validationSystem: ParachainStakingRebondValidationSystem,
    private val interactor: ParachainStakingRebondInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val payload: ParachainStakingRebondPayload,
    private val collatorsUseCase: CollatorsUseCase,
    resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val delegatorState = delegatorStateUseCase.currentDelegatorStateFlow()
        .shareInBackground()

    val rebondAmount = delegatorState.flatMapLatest { state ->
        val amount = interactor.rebondAmount(state, payload.collatorId)

        assetFlow.map { mapAmountToAmountModel(amount, it) }
    }
        .withLoading()
        .shareInBackground()

    val hintsMixin = resourcesHintsMixinFactory.create(
        coroutineScope = this,
        hintsRes = listOf(R.string.staking_parachain_rebond_hint)
    )

    private val collator = flowOf {
        collatorsUseCase.getCollator(payload.collatorId)
    }.shareInBackground()

    val collatorAddressModel = collator.map(collatorsUseCase::collatorAddressModel)
        .shareInBackground()

    val currentAccountModelFlow = selectedAccountUseCase.selectedAddressModelFlow(selectedAssetState::chain)
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        feeLoaderMixin.loadFee(
            coroutineScope = this,
            feeConstructor = { interactor.estimateFee(payload.collatorId) },
            onRetryCancelled = ::backClicked
        )
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    fun collatorClicked() = launch {
        val parcel = withContext(Dispatchers.Default) {
            mapCollatorToDetailsParcelModel(collator.first())
        }

        router.openCollatorDetails(StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase))
    }

    private fun sendTransactionIfValid() = requireFee { fee ->
        launch {
            val payload = ParachainStakingRebondValidationPayload(
                fee = fee,
                asset = assetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { parachainStakingRebondValidationFailure(it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction()
            }
        }
    }

    private fun sendTransaction() = launch {
        interactor.rebond(payload.collatorId)
            .onFailure(::showError)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.returnToStakingMain()
            }

        _showNextProgress.value = false
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
