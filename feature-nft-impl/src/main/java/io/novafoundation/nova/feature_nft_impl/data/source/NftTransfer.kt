package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransfersValidationSystem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

interface NftTransfer {
    fun areTransfersSupported(chain: Chain): Boolean

    suspend fun performTransfer(transfer: NftTransferModel): Result<String>

    suspend fun calculateFee(transfer: NftTransferModel): BigInteger

    suspend fun nftDetails(nftId: String): NftDetails

    fun defaultValidationSystem(): NftTransfersValidationSystem
}
