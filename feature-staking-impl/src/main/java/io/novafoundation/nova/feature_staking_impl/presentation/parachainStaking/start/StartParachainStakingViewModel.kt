package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.rewards.RealParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.rewards.connectWith
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StartParachainStakingViewModel(
    private val router: StakingRouter,
    private val interactor: StartParachainStakingInteractor,
    private val rewardsComponentFactory: RealParachainStakingRewardsComponentFactory,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::transferable,
        balanceLabel = R.string.wallet_balance_transferable
    )

    private val selectedCollatorId = MutableStateFlow<AccountId?>(null)

    val rewardsComponent = rewardsComponentFactory.create(
        parentScope = this,
        assetFlow = assetFlow
    )

    val title = assetFlow.map { resourceManager.getString(R.string.staking_stake_format, it.token.configuration.symbol) }
        .inBackground()
        .share()

    init {
        rewardsComponent connectWith amountChooserMixin
        rewardsComponent connectWith selectedCollatorId

        feeLoaderMixin.connectWith(
            amountMixin = amountChooserMixin,
            scope = this,
            feeConstructor = interactor::estimateFee,
            onRetryCancelled = ::backClicked
        )
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
//            val rewardDestinationModel = rewardDestinationMixin.rewardDestinationModelFlow.first()
//            val rewardDestination = mapRewardDestinationModelToRewardDestination(rewardDestinationModel)
//            val amount = amountChooserMixin.amount.first()
//            val currentAccountAddress = interactor.getSelectedAccountProjection().address
//
//            val payload = SetupStakingPayload(
//                bondAmount = amount,
//                controllerAddress = currentAccountAddress,
//                maxFee = fee,
//                asset = assetFlow.first(),
//                isAlreadyNominating = false // on setup staking screen => not nominator
//            )
//
//            validationExecutor.requireValid(
//                validationSystem = validationSystem,
//                payload = payload,
//                validationFailureTransformer = { stakingValidationFailure(payload, it, resourceManager) },
//                progressConsumer = _showNextProgress.progressConsumer()
//            ) {
//                _showNextProgress.value = false
//
//                goToNextStep(amount, rewardDestination, currentAccountAddress)
//            }
        }
    }

//    private fun goToNextStep(
//        newAmount: BigDecimal,
//        rewardDestination: RewardDestination,
//        currentAccountAddress: String
//    ) {
//
//        router.openStartChangeValidators()
//    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
