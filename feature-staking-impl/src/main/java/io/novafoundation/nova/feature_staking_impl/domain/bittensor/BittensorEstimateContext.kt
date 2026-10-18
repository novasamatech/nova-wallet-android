package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class BittensorEstimateContext(val metaId: Long, val chain: Chain)
