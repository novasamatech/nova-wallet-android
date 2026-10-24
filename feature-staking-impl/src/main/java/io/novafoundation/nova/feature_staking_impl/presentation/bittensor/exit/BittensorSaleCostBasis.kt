package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import android.os.Parcelable
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.parcelize.Parcelize

/** What a sale screen can say about the cost basis of one subnet. */
sealed interface BittensorSaleCostBasis {

    /** [settled] is false while an own trade may still be missing from the indexed history. */
    data class Known(val totals: BittensorEstimatedPurchaseTotals, val settled: Boolean) : BittensorSaleCostBasis

    /** Complete, settled history without purchases on this subnet. */
    data object NoPurchases : BittensorSaleCostBasis

    /** Missing, incomplete, unclassified or still-settling history: never claimed to be "no purchases". */
    data object Unknown : BittensorSaleCostBasis
}

/** Settled totals the sale setup showed, carried to the review so it does not lose them while reloading. */
@Parcelize
data class BittensorSaleCostBasisPayload(val paidTao: String, val receivedAlpha: String) : Parcelable {

    fun totals() = BittensorEstimatedPurchaseTotals(paidTao.toBigInteger(), receivedAlpha.toBigInteger())
}

fun BittensorEstimatedPurchaseTotals.toPayload() = BittensorSaleCostBasisPayload(paidTao.toString(), receivedAlpha.toString())

fun BittensorSaleCostBasis.carriedPayload(): BittensorSaleCostBasisPayload? = (this as? BittensorSaleCostBasis.Known)
    ?.takeIf { it.settled }
    ?.totals
    ?.toPayload()

/** The cost basis of a sale from [positions]; as iOS loads it with the sale, it reads as loading until a position arrives, never as unknown. */
internal fun BittensorEstimatesMixin.observeSaleCostBasis(
    positions: Flow<BittensorExitPosition>,
    netuid: Int,
    carried: BittensorEstimatedPurchaseTotals? = null
): Flow<ExtendedLoadingState<BittensorSaleCostBasis>> {
    val context = positions.map { position -> position.takeUnless { it.isRoot }?.let { BittensorEstimateContext(it.metaId, it.chain) } }
        .distinctUntilChanged()
    return observePositionCostBasis(context)
        .onStart { emit(BittensorPositionCostBasis(ExtendedLoadingState.Loading, isSettling = false)) }
        .map { resolveSaleCostBasis(it, netuid, carried) }
}

fun resolveSaleCostBasis(
    update: BittensorPositionCostBasis,
    netuid: Int,
    carried: BittensorEstimatedPurchaseTotals? = null
): ExtendedLoadingState<BittensorSaleCostBasis> {
    val carriedBasis = carried?.let { BittensorSaleCostBasis.Known(it, settled = true) }
    return when (val state = update.state) {
        is ExtendedLoadingState.Loading -> carriedBasis?.let { ExtendedLoadingState.Loaded(it) } ?: ExtendedLoadingState.Loading
        is ExtendedLoadingState.Error -> ExtendedLoadingState.Loaded(carriedBasis ?: BittensorSaleCostBasis.Unknown)
        is ExtendedLoadingState.Loaded -> {
            val basis = state.data
            val live = basis?.forSubnet(netuid)?.let { BittensorSaleCostBasis.Known(it, settled = !update.isSettling) }
            val resolved = when {
                live != null && live.settled -> live
                carriedBasis != null -> carriedBasis
                live != null -> live
                basis?.canEstimate(netuid) == true && !update.isSettling -> BittensorSaleCostBasis.NoPurchases
                else -> BittensorSaleCostBasis.Unknown
            }
            ExtendedLoadingState.Loaded(resolved)
        }
    }
}
