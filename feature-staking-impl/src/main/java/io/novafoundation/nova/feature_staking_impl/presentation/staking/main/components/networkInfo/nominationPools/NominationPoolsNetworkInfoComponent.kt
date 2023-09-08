package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools

import android.util.Log
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.NominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.BaseNetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class NominationPoolsNetworkInfoComponentFactory(
    private val interactor: NominationPoolsNetworkInfoInteractor,
    private val resourceManager: ResourceManager,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): NetworkInfoComponent = NominationPoolsNetworkInfoComponent(
        interactor = interactor,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
    )
}

private class NominationPoolsNetworkInfoComponent(
    private val interactor: NominationPoolsNetworkInfoInteractor,
    resourceManager: ResourceManager,

    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
) : BaseNetworkInfoComponent(resourceManager, hostContext.scope, titleRes = R.string.nomination_pools_info) {

    init {
        updateContentState()

        updateExpandedState(with = shouldBeExpandedFlow())
    }

    override fun initialItems(): List<NetworkInfoItem> {
        return createNetworkInfoItems(activeNominators = null, nominatorsLabel = null)
    }

    private fun shouldBeExpandedFlow(): Flow<Boolean> {
        return interactor.observeShouldShowNetworkInfo()
    }

    private fun updateContentState() {
        combine(
            hostContext.assetFlow,
            interactor.observeNetworkInfo(stakingOption.assetWithChain.chain.id, hostContext.scope)
        ) { asset, networkInfo ->
            val items = createNetworkInfoItems(asset, networkInfo, nominatorsLabel = null)

            updateState { it.copy(actions = items) }
        }
            .catch { Log.e(LOG_TAG, "Error while updating content state", it) }
            .inBackground()
            .launchIn(this)
    }
}
