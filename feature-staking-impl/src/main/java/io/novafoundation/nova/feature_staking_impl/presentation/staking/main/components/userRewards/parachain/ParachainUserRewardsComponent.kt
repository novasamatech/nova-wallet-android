package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain

import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards.ParachainStakingUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ParachainUserRewardsComponentFactory(
    private val interactor: ParachainStakingUserRewardsInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun create(
        assetWithChain: ChainWithAsset,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = ParachainUserRewardsComponent(
        interactor = interactor,
        delegatorStateUseCase = delegatorStateUseCase,
        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class ParachainUserRewardsComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingUserRewardsInteractor,

    private val assetWithChain: ChainWithAsset,
    private val hostContext: ComponentHostContext,
) : BaseRewardComponent(hostContext) {

    override val state: Flow<UserRewardsState?> = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = assetWithChain,
        stateProducer = ::rewardsFlow,
        onDelegatorChange = ::syncStakingRewards
    )
        .shareInBackground()

    private fun rewardsFlow(delegatorState: DelegatorState.Delegator): Flow<AmountModel> = combine(
        interactor.observeRewards(delegatorState, assetWithChain.chain, assetWithChain.asset),
        hostContext.assetFlow
    ) { totalReward, asset ->
        mapAmountToAmountModel(totalReward, asset)
    }

    private fun syncStakingRewards(delegatorState: DelegatorState.Delegator) {
        launch {
            interactor.syncRewards(delegatorState, assetWithChain.chain, assetWithChain.asset)
        }
    }
}
