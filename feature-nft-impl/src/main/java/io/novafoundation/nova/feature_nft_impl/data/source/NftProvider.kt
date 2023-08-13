package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface NftProvider {

    suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun subscribeNftOwnerAddress(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<String>

    suspend fun nftFullSync(nft: Nft)

    suspend fun getCollectionName(
        collectionId: String,
        chainId: ChainId?
    ): String?

    fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails>
}
