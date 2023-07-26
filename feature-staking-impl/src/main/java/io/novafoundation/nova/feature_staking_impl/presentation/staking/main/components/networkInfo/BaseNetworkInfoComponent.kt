package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.RoundingMode

abstract class BaseNetworkInfoComponent(
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope,
) : NetworkInfoComponent,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    override val state = MutableStateFlow(initialState())

    abstract fun initialItems(): List<NetworkInfoItem>

    override fun onAction(action: NetworkInfoAction) {
        when (action) {
            NetworkInfoAction.ChangeExpendedStateClicked -> updateExpanded { !it.expanded }
        }
    }

    protected fun createNetworkInfoItems(
        asset: Asset,
        networkInfo: NetworkInfo,
        @StringRes nominatorsLabel: Int?
    ): List<NetworkInfoItem> {
        val unstakingPeriod = resourceManager.formatDuration(networkInfo.lockupPeriod)

        val stakingPeriod = when (networkInfo.stakingPeriod) {
            StakingPeriod.Unlimited -> resourceManager.getString(R.string.common_unlimited)
        }

        return createNetworkInfoItems(
            totalStaked = mapAmountToAmountModel(networkInfo.totalStake, asset).asLoaded(),
            minimumStake = mapAmountToAmountModel(networkInfo.minimumStake, asset, roundingMode = RoundingMode.CEILING).asLoaded(),
            activeNominators = networkInfo.nominatorsCount?.format()?.asLoaded(),
            unstakingPeriod = unstakingPeriod.asLoaded(),
            stakingPeriod = stakingPeriod.asLoaded(),
            nominatorsLabel = nominatorsLabel
        )
    }

    protected fun updateState(update: (NetworkInfoState) -> NetworkInfoState) {
        state.value = update(state.value)
    }

    private fun initialState(): NetworkInfoState {
        return NetworkInfoState(
            actions = initialItems(),
            expanded = false
        )
    }

    protected fun createNetworkInfoItems(
        totalStaked: ExtendedLoadingState<AmountModel> = ExtendedLoadingState.Loading,
        minimumStake: ExtendedLoadingState<AmountModel> = ExtendedLoadingState.Loading,
        activeNominators: ExtendedLoadingState<String>? = ExtendedLoadingState.Loading,
        stakingPeriod: ExtendedLoadingState<String> = ExtendedLoadingState.Loading,
        unstakingPeriod: ExtendedLoadingState<String> = ExtendedLoadingState.Loading,
        @StringRes nominatorsLabel: Int?
    ): List<NetworkInfoItem> {
        val nominatorsItem = if (nominatorsLabel != null && activeNominators != null) {
            NetworkInfoItem(
                title = resourceManager.getString(nominatorsLabel),
                content = activeNominators.toNetworkInfoContent()
            )
        } else {
            null
        }

        return listOfNotNull(
            NetworkInfoItem.totalStaked(resourceManager, totalStaked.toNetworkInfoContent()),
            NetworkInfoItem.minimumStake(resourceManager, minimumStake.toNetworkInfoContent()),
            nominatorsItem,
            NetworkInfoItem.stakingPeriod(resourceManager, stakingPeriod.toNetworkInfoContent()),
            NetworkInfoItem.unstakingPeriod(resourceManager, unstakingPeriod.toNetworkInfoContent())
        )
    }

    protected fun updateExpandedState(with: Flow<Boolean>) {
        with
            .onEach { expanded -> updateExpanded { expanded } }
            .inBackground()
            .catch { Log.e(LOG_TAG, "Error while updating expanded state", it) }
            .launchIn(this)
    }

    private fun updateExpanded(expanded: (NetworkInfoState) -> Boolean) = updateState {
        it.copy(expanded = expanded(it))
    }

    @JvmName("toNetworkInfoContentString")
    private fun ExtendedLoadingState<String>.toNetworkInfoContent() = map { NetworkInfoItem.Content(primary = it, secondary = null) }
    private fun ExtendedLoadingState<AmountModel>.toNetworkInfoContent() = map { NetworkInfoItem.Content(primary = it.token, secondary = it.fiat) }
}
