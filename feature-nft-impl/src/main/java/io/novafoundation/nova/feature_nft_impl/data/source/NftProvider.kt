package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface NftProvider {

    val requireFullChainSync: Boolean

    suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun nftFullSync(nft: Nft)

    fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails>
}
