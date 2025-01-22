package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.hints.NoHintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isDelegating
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingViewModel.MythosConfirmStartStakingState
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details.mythos
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

class ConfirmStartMythosStakingViewModel(
    private val mythosRouter: MythosStakingRouter,
    private val startStakingRouter: StartMultiStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
    private val payload: ConfirmStartMythosStakingPayload,
    private val stakingStartedDetectionService: StakingStartedDetectionService,
    mythosSharedComputation: MythosSharedComputation,
    walletUiUseCase: WalletUiUseCase,
) : ConfirmStartSingleTargetStakingViewModel<MythosConfirmStartStakingState>(
    stateFactory = { computationalScope ->
        MythosConfirmStartStakingState(
            computationalScope = computationalScope,
            mythosSharedComputation = mythosSharedComputation,
            addressIconGenerator = addressIconGenerator,
            payload = payload
        )
    },
    router = mythosRouter,
    addressIconGenerator = addressIconGenerator,
    selectedAccountUseCase = selectedAccountUseCase,
    resourceManager = resourceManager,
    feeLoaderMixin = feeLoaderMixin,
    externalActions = externalActions,
    selectedAssetState = selectedAssetState,
    validationExecutor = validationExecutor,
    assetUseCase = assetUseCase,
    walletUiUseCase = walletUiUseCase,
    payload = payload,
) {

    override val hintsMixin = NoHintsMixin()

    override suspend fun confirmClicked(fee: Fee, amount: Balance, asset: Asset) {
        showMessage("TODO")
//        val payload = StartParachainStakingValidationPayload(
//            amount = asset.token.amountFromPlanks(amount),
//            fee = fee,
//            collator = state.collator(),
//            asset = asset,
//            delegatorState = state.delegatorStateFlow.first(),
//        )
//
//        validationExecutor.requireValid(
//            validationSystem = validationSystem,
//            payload = payload,
//            validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
//            progressConsumer = _showNextProgress.progressConsumer()
//        ) {
//            sendTransaction(amount, payload.collator)
//        }
    }

    override suspend fun openStakeTargetInfo() {
        val parcel = StakeTargetDetailsPayload.mythos(state.collator)
        mythosRouter.openCollatorDetails(parcel)
    }

//    private fun sendTransaction(
//        amountInPlanks: Balance,
//        collator: Collator,
//    ) = launch {
//        stakingStartedDetectionService.pauseDetection(viewModelScope)
//
//        interactor.delegate(
//            amount = amountInPlanks,
//            collator = collator.accountId()
//        )
//            .onFailure {
//                showError(it)
//
//                stakingStartedDetectionService.activateDetection(viewModelScope)
//            }
//            .onSuccess {
//                showMessage(resourceManager.getString(R.string.common_transaction_submitted))
//
//                finishFlow()
//            }
//
//        _showNextProgress.value = false
//    }

//    private fun finishFlow() {
//        when (payload.flowMode) {
//            StartParachainStakingMode.START -> startStakingRouter.returnToStakingDashboard()
//            StartParachainStakingMode.BOND_MORE -> mythosRouter.returnToStakingMain()
//        }
//    }

    class MythosConfirmStartStakingState(
        computationalScope: ComputationalScope,
        mythosSharedComputation: MythosSharedComputation,
        private val addressIconGenerator: AddressIconGenerator,
        payload: ConfirmStartMythosStakingPayload,
    ) : ConfirmStartSingleTargetStakingState,
        ComputationalScope by computationalScope {

        val currentDelegatorStateFlow = mythosSharedComputation.delegatorStateFlow()
            .take(1) // Take 1 to avoid changing state after tx is in block
            .shareInBackground()

        val collator = payload.collator.toDomain()

        override fun isStakeMoreFlow(): Flow<Boolean> {
            return currentDelegatorStateFlow.map { it.isDelegating() }
        }

        override suspend fun collatorAddressModel(chain: Chain): AddressModel {
            return addressIconGenerator.collatorAddressModel(collator, chain)
        }
    }
}
