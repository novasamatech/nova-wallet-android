package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.toLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R

data class NetworkInfoItem(val title: String, val content: LoadingState<Content>) {

    companion object;

    data class Content(val primary: String, val secondary: String?)
}

fun NetworkInfoItem.Companion.totalStaked(resourceManager: ResourceManager, content: NetworkInfoItem.Content?): NetworkInfoItem {
    return NetworkInfoItem(
        title = resourceManager.getString(R.string.staking_total_staked),
        content = content.toLoadingState()
    )
}

fun NetworkInfoItem.Companion.minimumStake(resourceManager: ResourceManager, content: NetworkInfoItem.Content?): NetworkInfoItem {
    return NetworkInfoItem(
        title = resourceManager.getString(R.string.staking_main_minimum_stake_title),
        content = content.toLoadingState()
    )
}

fun NetworkInfoItem.Companion.activeNominators(resourceManager: ResourceManager, content: NetworkInfoItem.Content?): NetworkInfoItem {
    return NetworkInfoItem(
        title = resourceManager.getString(R.string.staking_main_active_nominators_title),
        content = content.toLoadingState()
    )
}

fun NetworkInfoItem.Companion.stakingPeriod(resourceManager: ResourceManager, content: NetworkInfoItem.Content?): NetworkInfoItem {
    return NetworkInfoItem(
        title = resourceManager.getString(R.string.staking_staking_period),
        content = content.toLoadingState()
    )
}

fun NetworkInfoItem.Companion.unstakingPeriod(resourceManager: ResourceManager, content: NetworkInfoItem.Content?): NetworkInfoItem {
    return NetworkInfoItem(
        title = resourceManager.getString(R.string.staking_unbonding_period_v1_9_0),
        content = content.toLoadingState()
    )
}
