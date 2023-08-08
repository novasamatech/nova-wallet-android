package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools.NominationPoolsYourPoolComponentFactory

typealias YourPoolComponent = StatefullComponent<LoadingState<YourPoolComponentState>, YourPoolEvent, YourPoolAction>

class YourPoolComponentState(
    val poolAccount: AddressModel,
    val poolId: PoolId,
    val poolIcon: Icon,
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
