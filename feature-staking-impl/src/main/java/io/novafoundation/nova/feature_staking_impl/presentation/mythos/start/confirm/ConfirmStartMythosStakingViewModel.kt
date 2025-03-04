package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.hints.NoHintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.hasStakedCollators
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isNotStarted
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.activateDetection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.pauseDetection
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingViewModel.MythosConfirmStartStakingState
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details.mythos
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

class ConfirmStartMythosStakingViewModel(
    private val mythosRouter: MythosStakingRouter,
    private val startStakingRouter: StartMultiStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
    private val payload: ConfirmStartMythosStakingPayload,
    private val stakingStartedDetectionService: StakingStartedDetectionService,
    private val validationSystem: StartMythosStakingValidationSystem,
    private val stakingBlockNumberUseCase: StakingBlockNumberUseCase,
    private val mythosStakingValidationFailureFormatter: MythosStakingValidationFailureFormatter,
    private val interactor: StartMythosStakingInteractor,
    mythosSharedComputation: MythosSharedComputation,
    walletUiUseCase: WalletUiUseCase,
) : ConfirmStartSingleTargetStakingViewModel<MythosConfirmStartStakingState>(
    stateFactory = { computationalScope ->
        MythosConfirmStartStakingState(
            computationalScope = computationalScope,
            mythosSharedComputation = mythosSharedComputation,
            addressIconGenerator = addressIconGenerator,
            payload = payload,
            stakingBlockNumberUseCase = stakingBlockNumberUseCase
        )
    },
    router = mythosRouter,
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

    override val hintsMixin = NoHintsMixin()

    override suspend fun confirmClicked(fee: Fee, amount: Balance, asset: Asset) {
        val payload = StartMythosStakingValidationPayload(
            amount = asset.token.amountFromPlanks(amount),
            fee = fee,
            asset = asset,
            collator = state.collator,
            delegatorState = state.currentDelegatorStateFlow.first(),
            currentBlockNumber = state.currentBlockNumberFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { reason, _ -> mythosStakingValidationFailureFormatter.formatStartStaking(reason) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(amount, it.collator, it.delegatorState)
        }
    }

    override suspend fun openStakeTargetInfo() {
        val parcel = StakeTargetDetailsPayload.mythos(state.collator)
        mythosRouter.openCollatorDetails(parcel)
    }

    private fun sendTransaction(
        amountInPlanks: Balance,
        collator: MythosCollator,
        currentState: MythosDelegatorState,
    ) = launchUnit {
        stakingStartedDetectionService.pauseDetection(viewModelScope)

        interactor.stake(
            amount = amountInPlanks,
            currentState = currentState,
            candidate = collator.accountId,
        )
            .onFailure {
                showError(it)

                stakingStartedDetectionService.activateDetection(viewModelScope)
            }
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow(currentState)
            }

        _showNextProgress.value = false
    }

    private fun finishFlow(previousState: MythosDelegatorState) {
        if (previousState.isNotStarted()) {
            startStakingRouter.returnToStakingDashboard()
        } else {
            mythosRouter.returnToStakingMain()
        }
    }

    class MythosConfirmStartStakingState(
        computationalScope: ComputationalScope,
        mythosSharedComputation: MythosSharedComputation,
        private val addressIconGenerator: AddressIconGenerator,
        payload: ConfirmStartMythosStakingPayload,
        private val stakingBlockNumberUseCase: StakingBlockNumberUseCase,
    ) : ConfirmStartSingleTargetStakingState,
        ComputationalScope by computationalScope {

        val currentDelegatorStateFlow = mythosSharedComputation.delegatorStateFlow()
            .take(1) // Take 1 to avoid changing state after tx is in block
            .shareInBackground()

        val currentBlockNumberFlow = stakingBlockNumberUseCase.currentBlockNumberFlow()
            .shareInBackground()

        val collator = payload.collator.toDomain()

        override fun isStakeMoreFlow(): Flow<Boolean> {
            return currentDelegatorStateFlow.map { it.hasStakedCollators() }
        }

        override suspend fun collatorAddressModel(chain: Chain): AddressModel {
            return addressIconGenerator.collatorAddressModel(collator, chain)
        }
    }
}
