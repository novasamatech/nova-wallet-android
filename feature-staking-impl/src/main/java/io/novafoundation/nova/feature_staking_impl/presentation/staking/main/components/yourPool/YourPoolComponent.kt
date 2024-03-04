package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayModel
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools.NominationPoolsYourPoolComponentFactory
import io.novasama.substrate_sdk_android.runtime.AccountId

typealias YourPoolComponent = StatefullComponent<LoadingState<YourPoolComponentState>, YourPoolEvent, YourPoolAction>

class YourPoolComponentState(
    val poolStash: AccountId,
    val poolId: PoolId,
    val display: PoolDisplayModel,
    val title: String,
)

typealias YourPoolEvent = Nothing

sealed class YourPoolAction {

    object PoolInfoClicked : YourPoolAction()
}

class YourPoolComponentFactory(
    private val nominationPoolsFactory: NominationPoolsYourPoolComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): YourPoolComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = UnsupportedComponent.creator(),
        parachainComponentCreator = UnsupportedComponent.creator(),
        nominationPoolsCreator = nominationPoolsFactory::create,
        hostContext = hostContext
    )
}
