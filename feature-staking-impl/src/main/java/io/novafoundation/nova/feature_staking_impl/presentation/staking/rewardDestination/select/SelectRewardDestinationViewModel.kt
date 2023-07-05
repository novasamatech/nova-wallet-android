package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapRewardDestinationModelToRewardDestination
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.RewardDestinationParcelModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SelectRewardDestinationViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val changeRewardDestinationInteractor: ChangeRewardDestinationInteractor,
    private val validationSystem: RewardDestinationValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val rewardDestinationMixin: RewardDestinationMixin.Presentation,
    private val selectedAssetSharedState: StakingSharedState,
    private val stakingSharedComputation: StakingSharedComputation,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    RewardDestinationMixin by rewardDestinationMixin {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val rewardCalculator = viewModelScope.async {
        stakingSharedComputation.rewardCalculator(
            stakingOption = selectedAssetSharedState.selectedOption(),
            scope = viewModelScope
        )
    }

    private val rewardDestinationFlow = rewardDestinationMixin.rewardDestinationModelFlow
        .map { mapRewardDestinationModelToRewardDestination(it) }
        .inBackground()
        .share()

    private val stashStateFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val controllerAssetFlow = stashStateFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .inBackground()
        .share()

    val continueAvailable = rewardDestinationMixin.rewardDestinationChangedFlow
        .asLiveData()

    init {
        rewardDestinationFlow.combine(stashStateFlow) { rewardDestination, stashState ->
            loadFee(rewardDestination, stashState)
        }.launchIn(viewModelScope)

        controllerAssetFlow.onEach {
            rewardDestinationMixin.updateReturns(rewardCalculator(), it, it.bonded)
        }.launchIn(viewModelScope)

        stashStateFlow.onEach(rewardDestinationMixin::loadActiveRewardDestination)
            .launchIn(viewModelScope)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun loadFee(rewardDestination: RewardDestination, stashState: StakingState.Stash) {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { changeRewardDestinationInteractor.estimateFee(stashState, rewardDestination) },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val payload = RewardDestinationValidationPayload(
                availableControllerBalance = controllerAssetFlow.first().transferable,
                fee = fee,
                stashState = stashStateFlow.first()
            )

            val rewardDestination = rewardDestinationModelFlow.first()

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { rewardDestinationValidationFailure(resourceManager, it) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                _showNextProgress.value = false

                goToNextStep(rewardDestination, it.fee)
            }
        }
    }

    private fun goToNextStep(
        rewardDestination: RewardDestinationModel,
        fee: BigDecimal
    ) {
        val payload = ConfirmRewardDestinationPayload(
            fee = fee,
            rewardDestination = mapRewardDestinationModelToRewardDestinationParcelModel(rewardDestination)
        )

        router.openConfirmRewardDestination(payload)
    }

    private fun mapRewardDestinationModelToRewardDestinationParcelModel(rewardDestination: RewardDestinationModel): RewardDestinationParcelModel {
        return when (rewardDestination) {
            RewardDestinationModel.Restake -> RewardDestinationParcelModel.Restake
            is RewardDestinationModel.Payout -> RewardDestinationParcelModel.Payout(rewardDestination.destination.address)
        }
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private suspend fun rewardCalculator() = rewardCalculator.await()
}
