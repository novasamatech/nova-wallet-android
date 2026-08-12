package io.novafoundation.nova.feature_swap_api.data.history

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Produces a transfer-history filter that hides Nova-commission and Hydration-router
 * legs of a swap, so the user only sees the swap entry itself. Returns null when the
 * chain doesn't support Hydration swaps.
 */
interface HydrationSwapTransferFilterFactory {

    fun create(chain: Chain): Filter<Operation>?
}
