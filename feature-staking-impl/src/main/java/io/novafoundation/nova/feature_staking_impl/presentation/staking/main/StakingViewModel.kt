package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class StakingViewModel(
    selectedAccountUseCase: SelectedAccountUseCase,

    assetUseCase: AssetUseCase,
    alertsComponentFactory: AlertsComponentFactory,
    unbondingComponentFactory: UnbondingComponentFactory,
    startStakingComponentFactory: StartStakingComponentFactory,
    stakeSummaryComponentFactory: StakeSummaryComponentFactory,
    userRewardsComponentFactory: UserRewardsComponentFactory,
    stakeActionsComponentFactory: StakeActionsComponentFactory,
    networkInfoComponentFactory: NetworkInfoComponentFactory,
    yourPoolComponentFactory: YourPoolComponentFactory,

    val router: StakingRouter,

    private val validationExecutor: ValidationExecutor,
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val externalActionsMixin: ExternalActions.Presentation,
    stakingUpdateSystem: UpdateSystem,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActionsMixin {

    private val selectedAssetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val titleFlow = stakingSharedState.selectedChainFlow()
        .map { resourceManager.getString(R.string.staking_title_format, it.name) }
        .shareInBackground()

    private val selectedAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val componentHostContext = ComponentHostContext(
        errorDisplayer = ::showError,
        selectedAccount = selectedAccountFlow,
        assetFlow = selectedAssetFlow,
        scope = this,
        validationExecutor = validationExecutor,
        externalActions = externalActionsMixin
    )

    val unbondingComponent = unbondingComponentFactory.create(componentHostContext)
    val startStakingComponent = startStakingComponentFactory.create(componentHostContext)
    val stakeSummaryComponent = stakeSummaryComponentFactory.create(componentHostContext)
    val userRewardsComponent = userRewardsComponentFactory.create(componentHostContext)
    val stakeActionsComponent = stakeActionsComponentFactory.create(componentHostContext)
    val networkInfoComponent = networkInfoComponentFactory.create(componentHostContext)
    val alertsComponent = alertsComponentFactory.create(componentHostContext)
    val yourPoolComponent = yourPoolComponentFactory.create(componentHostContext)

    fun backClicked() {
        router.back()
    }

    init {
        stakingUpdateSystem.start()
            .launchIn(this)
    }
}
