package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.bondMoreValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmBondMoreViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val bondMoreInteractor: BondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val iconGenerator: AddressIconGenerator,
    private val validationSystem: BondMoreValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val payload: ConfirmBondMorePayload,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    walletUiUseCase: WalletUiUseCase,
    hintsMixinFactory: ResourcesHintsMixinFactory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val hintsMixin = hintsMixinFactory.create(
        coroutineScope = this,
        hintsRes = listOf(R.string.staking_hint_reward_bond_more_v2_2_0)
    )

    private val stashAssetFlow = interactor.assetFlow(payload.stashAddress)
        .shareInBackground()

    private val stakeableAmountFlow = stashAssetFlow.map {
        val planks = bondMoreInteractor.stakeableAmount(it)
        it.token.amountFromPlanks(planks)
    }.shareInBackground()

    val amountModelFlow = stashAssetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeStatusFlow = stashAssetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val originAddressModelFlow = flowOf {
        iconGenerator.createAccountAddressModel(selectedAssetState.chain(), payload.stashAddress)
    }
        .shareInBackground()

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        externalActions.showAddressActions(payload.stashAddress, selectedAssetState.chain())
    }

    private fun maybeGoToNext() = launch {
        val payload = BondMoreValidationPayload(
            stashAddress = payload.stashAddress,
            fee = decimalFee,
            amount = payload.amount,
            stashAsset = stashAssetFlow.first(),
            stakeable = stakeableAmountFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { bondMoreValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        val token = stashAssetFlow.first().token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        bondMoreInteractor.bondMore(payload.stashAddress, amountInPlanks)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { finishFlow() }
            }.onFailure {
                showError(it)
            }

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        router.returnToStakingMain()
    }
}
