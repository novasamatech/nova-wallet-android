package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.model.StakingBalanceModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StakingBalanceViewModel(
    private val router: StakingRouter,
    private val redeemValidationSystem: ManageStakingValidationSystem,
    private val unbondValidationSystem: ManageStakingValidationSystem,
    private val bondMoreValidationSystem: ManageStakingValidationSystem,
    private val rebondValidationSystem: ManageStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val unbondingInteractor: UnbondInteractor,
    private val resourceManager: ResourceManager,
    private val interactor: StakingInteractor,
) : BaseViewModel(), Validatable by validationExecutor {

    private val assetFlow = interactor.currentAssetFlow()
        .share()

    val stakingBalanceModelLiveData = assetFlow.map { asset ->
        StakingBalanceModel(
            bonded = mapAmountToAmountModel(asset.bonded, asset),
            unbonding = mapAmountToAmountModel(asset.unbonding, asset),
            redeemable = mapAmountToAmountModel(asset.redeemable, asset)
        )
    }
        .inBackground()
        .asLiveData()

    fun bondMoreClicked() = requireValidManageAction(bondMoreValidationSystem) {
        router.openBondMore(SelectBondMorePayload(overrideFinishAction = null))
    }

    fun unbondClicked() = requireValidManageAction(unbondValidationSystem) {
        router.openSelectUnbond()
    }

    fun backClicked() {
        router.back()
    }

    private fun requireValidManageAction(
        validationSystem: ManageStakingValidationSystem,
        block: (ManageStakingValidationPayload) -> Unit,
    ) {
        launch {
            val stakingState = interactor.selectedAccountStakingStateFlow().first()
            require(stakingState is StakingState.Stash)

            validationExecutor.requireValid(
                validationSystem,
                ManageStakingValidationPayload(stakingState),
                validationFailureTransformer = { manageStakingActionValidationFailure(it, resourceManager) },
                block = block
            )
        }
    }
}
