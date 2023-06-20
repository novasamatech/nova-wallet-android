package io.novafoundation.nova.feature_staking_impl.presentation.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapRewardDestinationModelToRewardDestination
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.stakeable
import io.novafoundation.nova.feature_staking_impl.domain.setup.SetupStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.connectWith
import io.novafoundation.nova.feature_staking_impl.presentation.common.validation.stakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SetupStakingViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val setupStakingInteractor: SetupStakingInteractor,
    private val validationSystem: ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val rewardDestinationMixin: RewardDestinationMixin.Presentation,
    private val selectedAssetSharedState: AnySelectedAssetOptionSharedState,
    private val stakingSharedComputation: StakingSharedComputation,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    RewardDestinationMixin by rewardDestinationMixin {

    private val currentProcessState = setupStakingSharedState.get<SetupStakingProcess.Stash>()

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val assetFlow = interactor.currentAssetFlow()
        .share()

    private val rewardCalculator = viewModelScope.async {
        stakingSharedComputation.rewardCalculator(
            chainAsset = selectedAssetSharedState.chainAsset(),
            scope = viewModelScope
        )
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::stakeable,
        balanceLabel = R.string.wallet_balance_available
    )

    val title = assetFlow.map { resourceManager.getString(R.string.staking_stake_format, it.token.configuration.symbol) }
        .inBackground()
        .share()

    init {
        loadFee()

        rewardDestinationMixin.connectWith(amountChooserMixin, rewardCalculator = rewardCalculator)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        setupStakingSharedState.set(currentProcessState.previous())

        router.back()
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = {
                val address = interactor.getSelectedAccountProjection().address

                setupStakingInteractor.estimateMaxSetupStakingFee(address)
            },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val rewardDestinationModel = rewardDestinationMixin.rewardDestinationModelFlow.first()
            val rewardDestination = mapRewardDestinationModelToRewardDestination(rewardDestinationModel)
            val amount = amountChooserMixin.amount.first()
            val currentAccountAddress = interactor.getSelectedAccountProjection().address

            val payload = SetupStakingPayload(
                bondAmount = amount,
                maxFee = fee,
                stashAsset = assetFlow.first(),
                controllerAsset = assetFlow.first(),
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { stakingValidationFailure(payload, it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                _showNextProgress.value = false

                goToNextStep(amount, rewardDestination, currentAccountAddress)
            }
        }
    }

    private fun goToNextStep(
        newAmount: BigDecimal,
        rewardDestination: RewardDestination,
        currentAccountAddress: String
    ) {
        setupStakingSharedState.set(currentProcessState.next(newAmount, rewardDestination, currentAccountAddress))

        router.openStartChangeValidators()
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
