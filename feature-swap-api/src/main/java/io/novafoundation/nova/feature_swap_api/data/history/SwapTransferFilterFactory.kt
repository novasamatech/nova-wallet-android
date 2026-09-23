package io.novafoundation.nova.feature_swap_api.data.history

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Produces a transfer-history filter that hides internal swap transfers, such as the Nova commission
 * and Hydration router legs. Returns null when the chain has no supported swap implementation.
 */
interface SwapTransferFilterFactory {

    fun create(chain: Chain): Filter<Operation>?
}
