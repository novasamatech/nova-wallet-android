package io.novafoundation.nova.feature_nft_impl.domain.nft.search

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class SendNftListItem(
    val identifier: String,
    val name: String,
    val collectionName: String,
    val media: String?,
    val chain: Chain
)
