package io.novafoundation.nova.feature_staking_impl.data.collators

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface KnownNovaCollators {

    fun getCollatorIds(chainId: ChainId): List<String>
}

class FixedKnownNovaCollators : KnownNovaCollators {

    private val novaValidators by lazy {
        mapOf(
            Chain.Geneses.POLIMEC to listOf("5A5Qgq3wn6JeH8Qtu7rakxULpBhtyqyX8iNj1XV8WFg3U58T")
        )
    }

    override fun getCollatorIds(chainId: ChainId): List<String> {
        return novaValidators[chainId].orEmpty()
    }
}
