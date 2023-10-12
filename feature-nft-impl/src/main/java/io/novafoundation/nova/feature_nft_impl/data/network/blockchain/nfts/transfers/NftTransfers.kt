package io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class NftTransferModel(
    val sender: MetaAccount,
    val recipient: String,
    val nftId: String,
    val nftType: Nft.Type,
    val originChain: Chain,
    val destinationChain: Chain,
)
