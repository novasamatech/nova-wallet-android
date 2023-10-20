package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain.ParachainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain.RelaychainNetworkInfoComponentFactory

typealias NetworkInfoComponent = StatefullComponent<NetworkInfoState, NetworkInfoEvent, NetworkInfoAction>

data class NetworkInfoState(
    val title: String,
    val actions: List<NetworkInfoItem>,
    val expanded: Boolean,
)

typealias NetworkInfoEvent = Nothing

sealed class NetworkInfoAction {

    object ChangeExpendedStateClicked : NetworkInfoAction()
}

class NetworkInfoComponentFactory(
    private val relaychainComponentFactory: RelaychainNetworkInfoComponentFactory,
    private val parachainComponentFactory: ParachainNetworkInfoComponentFactory,
    private val nominationPoolsComponentFactory: NominationPoolsNetworkInfoComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): NetworkInfoComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = parachainComponentFactory::create,
        nominationPoolsCreator = nominationPoolsComponentFactory::create,
        hostContext = hostContext
    )
}
