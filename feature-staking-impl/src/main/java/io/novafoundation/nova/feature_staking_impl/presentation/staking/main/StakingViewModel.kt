package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import kotlinx.coroutines.flow.launchIn

class StakingViewModel(
    selectedAccountUseCase: SelectedAccountUseCase,

    assetSelectorMixinFactory: AssetSelectorFactory,
    alertsComponentFactory: AlertsComponentFactory,
    unbondingComponentFactory: UnbondingComponentFactory,
    startStakingComponentFactory: StartStakingComponentFactory,
    stakeSummaryComponentFactory: StakeSummaryComponentFactory,
    userRewardsComponentFactory: UserRewardsComponentFactory,
    stakeActionsComponentFactory: StakeActionsComponentFactory,
    networkInfoComponentFactory: NetworkInfoComponentFactory,

    val router: StakingRouter,
    private val validationExecutor: ValidationExecutor,
    stakingUpdateSystem: UpdateSystem,
) : BaseViewModel(),
    Validatable by validationExecutor {

    val assetSelectorMixin = assetSelectorMixinFactory.create(
        scope = this,
        amountProvider = Asset::transferable
    )

    private val selectedAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val componentHostContext = ComponentHostContext(
        errorDisplayer = ::showError,
        selectedAccount = selectedAccountFlow,
        assetFlow = assetSelectorMixin.selectedAssetFlow,
        scope = this,
        validationExecutor = validationExecutor
    )

    val unbondingComponent = unbondingComponentFactory.create(componentHostContext)
    val startStakingComponent = startStakingComponentFactory.create(componentHostContext)
    val stakeSummaryComponent = stakeSummaryComponentFactory.create(componentHostContext)
    val userRewardsComponent = userRewardsComponentFactory.create(componentHostContext)
    val stakeActionsComponent = stakeActionsComponentFactory.create(componentHostContext)
    val networkInfoComponent = networkInfoComponentFactory.create(componentHostContext)
    val alertsComponent = alertsComponentFactory.create(componentHostContext)

    val selectedWalletFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    init {
        stakingUpdateSystem.start()
            .launchIn(this)
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }
}
