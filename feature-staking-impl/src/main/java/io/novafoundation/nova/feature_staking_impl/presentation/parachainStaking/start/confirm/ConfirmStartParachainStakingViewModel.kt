package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.startParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ConfirmStartParachainStakingViewModel(
    private val parachainStakingRouter: ParachainStakingRouter,
    private val startStakingRouter: StartMultiStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val validationSystem: StartParachainStakingValidationSystem,
    private val interactor: StartParachainStakingInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
    private val collatorsUseCase: CollatorsUseCase,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    walletUiUseCase: WalletUiUseCase,
    private val payload: ConfirmStartParachainStakingPayload,
    hintsMixinFactory: ConfirmStartParachainStakingHintsMixinFactory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    // Take state only once since subscribing to it might cause switch to Delegator state while waiting for tx confirmation
    private val delegatorStateFlow = flowOf { delegatorStateUseCase.currentDelegatorState() }
        .shareInBackground()

    val hintsMixin = hintsMixinFactory.create(
        coroutineScope = this,
        delegatorStateFlow = delegatorStateFlow
    )

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val collator by lazyAsync(Dispatchers.Default) {
        mapCollatorParcelModelToCollator(payload.collator)
    }

    val currentAccountModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            account = it,
            name = null
        )
    }.shareInBackground()

    val title = delegatorStateFlow.map {
        if (it is DelegatorState.Delegator) {
            resourceManager.getString(R.string.staking_bond_more_v1_9_0)
        } else {
            resourceManager.getString(R.string.staking_start_title)
        }
    }
        .shareInBackground()

    val amountModel = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val collatorAddressModel = flowOf {
        addressIconGenerator.collatorAddressModel(collator(), selectedAssetState.chain())
    }.shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        setInitialFee()
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        parachainStakingRouter.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    fun collatorClicked() = launch {
        val parcel = withContext(Dispatchers.Default) {
            mapCollatorToDetailsParcelModel(collator())
        }

        parachainStakingRouter.openCollatorDetails(StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase))
    }

    private fun setInitialFee() = launch {
        feeLoaderMixin.setFee(payload.fee)
    }

    private fun sendTransactionIfValid() = requireFee { _ ->
        launch {
            val payload = StartParachainStakingValidationPayload(
                amount = payload.amount,
                fee = payload.fee,
                collator = collator(),
                asset = assetFlow.first(),
                delegatorState = delegatorStateFlow.first(),
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction()
            }
        }
    }

    private fun sendTransaction() = launch {
        val token = assetFlow.first().token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        interactor.delegate(
            amount = amountInPlanks,
            collator = payload.collator.accountIdHex.fromHex()
        )
            .onFailure {
                showError(it)
            }
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow()
            }

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        when(payload.flowMode) {
            StartParachainStakingMode.START -> parachainStakingRouter.returnToStakingMain()
            StartParachainStakingMode.BOND_MORE -> startStakingRouter.returnToStakingDashboard()
        }
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
