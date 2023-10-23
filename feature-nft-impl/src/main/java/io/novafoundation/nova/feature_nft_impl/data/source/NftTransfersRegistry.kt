package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransfersValidationSystem
import io.novafoundation.nova.feature_nft_impl.data.source.transfers.Rmrk1NftTransfer
import io.novafoundation.nova.feature_nft_impl.data.source.transfers.Rmrk2NftTransfer
import io.novafoundation.nova.feature_nft_impl.data.source.transfers.UniquesNftTransfer
import javax.inject.Inject

@FeatureScope
class NftTransfersRegistry @Inject constructor(
    private val uniquesNftTransfer: UniquesNftTransfer,
    private val rmrk1NftTransfer: Rmrk1NftTransfer,
    private val rmrk2NftTransfer: Rmrk2NftTransfer
) {

    fun get(nftTypeKey: Nft.Type.Key): BaseNftTransfer {
        return when (nftTypeKey) {
            Nft.Type.Key.RMRKV1 -> rmrk1NftTransfer
            Nft.Type.Key.RMRKV2 -> rmrk2NftTransfer
            Nft.Type.Key.UNIQUES -> uniquesNftTransfer
        }
    }

    fun getAllTransfers(): List<BaseNftTransfer> {
        return listOf(rmrk1NftTransfer, rmrk2NftTransfer, uniquesNftTransfer)
    }

    fun defaultValidationSystem(): NftTransfersValidationSystem {
        return uniquesNftTransfer.defaultValidationSystem()
    }
}
