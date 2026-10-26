package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSorting
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import java.util.UUID
import kotlinx.parcelize.Parcelize

/** Immutable navigation snapshot. Each screen owns its edits and returns them through the communicator. */
@Parcelize
data class BittensorSelectionState(
    val flowId: String = UUID.randomUUID().toString(),
    val rootFilters: BittensorValidatorFilters = BittensorValidatorFilters(),
    val subnetFilters: BittensorValidatorFilters = BittensorValidatorFilters(),
    val browseState: BittensorSubnetBrowseState? = null,
    /** The flow started from a Where to stake quick pick; kept when the target changes. */
    val quickPick: Boolean = false,
    /** The flow moved on to amount entry, so leaving the subnet picker is no abandonment. */
    val flowContinued: Boolean = false
) : Parcelable {

    fun filtersFor(netuid: BittensorNetuid): BittensorValidatorFilters = if (netuid.isRoot()) rootFilters else subnetFilters

    fun withFilters(netuid: BittensorNetuid, filters: BittensorValidatorFilters): BittensorSelectionState =
        if (netuid.isRoot()) copy(rootFilters = filters) else copy(subnetFilters = filters)

    fun withResponse(response: BittensorSelectValidatorCommunicator.Response): BittensorSelectionState =
        response.selectionState.takeIf { accepts(response) } ?: this

    fun accepts(response: BittensorSelectValidatorCommunicator.Response): Boolean = response.selectionState.flowId == flowId

    /** The same filters and browse state under a new flow, for a setup screen stacked on top of this one. */
    fun forkedFlow(): BittensorSelectionState = copy(flowId = UUID.randomUUID().toString())
}

@Parcelize
data class BittensorSubnetBrowseState(
    val sorting: BittensorSubnetSorting = BittensorSubnetSorting.SEVEN_DAY_CHANGE,
    val hideThinPools: Boolean = false
) : Parcelable {

    companion object {

        fun fromQuickPick(order: BittensorQuickPickOrder?): BittensorSubnetBrowseState = when (order) {
            BittensorQuickPickOrder.POOL_SIZE -> BittensorSubnetBrowseState(BittensorSubnetSorting.POOL_DEPTH, true)
            BittensorQuickPickOrder.AGE -> BittensorSubnetBrowseState(BittensorSubnetSorting.AGE, true)
            BittensorQuickPickOrder.APY -> BittensorSubnetBrowseState(BittensorSubnetSorting.APY, true)
            null -> BittensorSubnetBrowseState()
        }
    }
}
