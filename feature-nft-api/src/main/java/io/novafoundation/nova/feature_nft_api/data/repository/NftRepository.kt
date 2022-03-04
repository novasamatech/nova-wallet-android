package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import kotlinx.coroutines.flow.Flow

interface NftRepository {

    fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun nftDetails(nftId: String): Flow<NftDetails>

    suspend fun initialNftSync(metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun fullNftSync(nft: Nft)
}
