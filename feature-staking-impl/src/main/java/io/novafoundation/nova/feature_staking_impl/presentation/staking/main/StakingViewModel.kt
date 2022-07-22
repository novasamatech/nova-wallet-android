package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class StakingViewModel(
    selectedAccountUseCase: SelectedAccountUseCase,

    assetSelectorMixinFactory: MixinFactory<AssetSelectorMixin.Presentation>,
    alertsComponentFactory: AlertsComponentFactory,
    unbondingComponentFactory: UnbondingComponentFactory,
    startStakingComponentFactory: StartStakingComponentFactory,
    stakeSummaryComponentFactory: StakeSummaryComponentFactory,
    userRewardsComponentFactory: UserRewardsComponentFactory,
    stakeActionsComponentFactory: StakeActionsComponentFactory,
    networkInfoComponentFactory: NetworkInfoComponentFactory,

    private val addressIconGenerator: AddressIconGenerator,
    private val router: StakingRouter,

    private val validationExecutor: ValidationExecutor,
    private val singleAssetSharedState: SingleAssetSharedState,
    stakingUpdateSystem: UpdateSystem,
) : BaseViewModel(),
    Validatable by validationExecutor {

    val assetSelectorMixin = assetSelectorMixinFactory.create(scope = this)

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

    val currentAddressModelFlow = currentAddressModelFlow()
        .shareInBackground()

    init {
        stakingUpdateSystem.start()
            .launchIn(this)
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    private fun currentAddressModelFlow(): Flow<AddressModel> {
        return combine(
            selectedAccountFlow,
            singleAssetSharedState.selectedChainFlow()
        ) { account, chain ->
            val address = account.addressIn(chain)

            if (address != null) {
                addressIconGenerator.createAddressModel(
                    chain = chain,
                    address = address,
                    sizeInDp = AddressIconGenerator.SIZE_BIG,
                    accountName = account.name,
                )
            } else {
                // no address found for specified chain - fallback to main substrate address
                addressIconGenerator.createAddressModel(
                    accountAddress = account.defaultSubstrateAddress,
                    sizeInDp = AddressIconGenerator.SIZE_BIG,
                    accountName = account.name,
                )
            }
        }
    }
}
