package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.accountId
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.activateDetection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.pauseDetection
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.ConfirmStartParachainStakingViewModel.ParachainConfirmStartStakingState
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.startParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmStartParachainStakingViewModel(
    private val parachainStakingRouter: ParachainStakingRouter,
    private val startStakingRouter: StartMultiStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val validationSystem: StartParachainStakingValidationSystem,
    private val interactor: StartParachainStakingInteractor,
    feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
    private val collatorsUseCase: CollatorsUseCase,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    walletUiUseCase: WalletUiUseCase,
    private val payload: ConfirmStartParachainStakingPayload,
    private val stakingStartedDetectionService: StakingStartedDetectionService,
    hintsMixinFactory: ConfirmStartParachainStakingHintsMixinFactory,
) : ConfirmStartSingleTargetStakingViewModel<ParachainConfirmStartStakingState>(
    stateFactory = { computationalScope ->
        ParachainConfirmStartStakingState(
            computationalScope = computationalScope,
            delegatorStateUseCase = delegatorStateUseCase,
            addressIconGenerator = addressIconGenerator,
            payload = payload
        )
    },
    router = parachainStakingRouter,
    addressIconGenerator = addressIconGenerator,
    selectedAccountUseCase = selectedAccountUseCase,
    resourceManager = resourceManager,
    feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
    externalActions = externalActions,
    selectedAssetState = selectedAssetState,
    validationExecutor = validationExecutor,
    assetUseCase = assetUseCase,
    walletUiUseCase = walletUiUseCase,
    payload = payload,
) {

    override val hintsMixin = hintsMixinFactory.create(coroutineScope = this, payload.flowMode)

    override suspend fun confirmClicked(fee: Fee, amount: Balance, asset: Asset) {
        val payload = StartParachainStakingValidationPayload(
            amount = asset.token.amountFromPlanks(amount),
            fee = fee,
            collator = state.collator(),
            asset = asset,
            delegatorState = state.delegatorStateFlow.first(),
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(amount, payload.collator)
        }
    }

    override suspend fun openStakeTargetInfo() {
        val parcel = withContext(Dispatchers.Default) {
            mapCollatorToDetailsParcelModel(state.collator())
        }

        parachainStakingRouter.openCollatorDetails(StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase))
    }

    private fun sendTransaction(
        amountInPlanks: Balance,
        collator: Collator,
    ) = launch {
        stakingStartedDetectionService.pauseDetection(viewModelScope)

        interactor.delegate(
            amount = amountInPlanks,
            collator = collator.accountId()
        )
            .onFailure {
                showError(it)

                stakingStartedDetectionService.activateDetection(viewModelScope)
            }
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow()
            }

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        when (payload.flowMode) {
            StartParachainStakingMode.START -> startStakingRouter.returnToStakingDashboard()
            StartParachainStakingMode.BOND_MORE -> parachainStakingRouter.returnToStakingMain()
        }
    }

    class ParachainConfirmStartStakingState(
        computationalScope: ComputationalScope,
        private val delegatorStateUseCase: DelegatorStateUseCase,
        private val addressIconGenerator: AddressIconGenerator,
        payload: ConfirmStartParachainStakingPayload,
    ) : ConfirmStartSingleTargetStakingState,
        ComputationalScope by computationalScope {

        // Take state only once since subscribing to it might cause switch to Delegator state while waiting for tx confirmation
        val delegatorStateFlow = flowOf { delegatorStateUseCase.currentDelegatorState() }
            .shareInBackground()

        val collator by lazyAsync(Dispatchers.Default) {
            mapCollatorParcelModelToCollator(payload.collator)
        }

        override fun isStakeMoreFlow(): Flow<Boolean> {
            return delegatorStateFlow.map { it is DelegatorState.Delegator }
        }

        override suspend fun collatorAddressModel(chain: Chain): AddressModel {
            return addressIconGenerator.collatorAddressModel(collator(), chain)
        }
    }
}
