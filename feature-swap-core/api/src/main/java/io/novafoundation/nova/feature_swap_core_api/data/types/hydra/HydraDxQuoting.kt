package io.novafoundation.nova.feature_swap_core_api.data.types.hydra

import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface HydraDxQuoting : SwapQuoting {

    interface Factory {

        fun create(chain: Chain): HydraDxQuoting
    }

    fun getSource(id: String): HydraDxQuotingSource<*>
}
