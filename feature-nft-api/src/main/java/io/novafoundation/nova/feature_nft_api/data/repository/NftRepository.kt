package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface NftRepository {

    fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun nftDetails(nftId: String): Flow<NftDetails>

    fun initialNftSyncTrigger(): Flow<NftSyncTrigger>

    suspend fun initialNftSync(metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun initialNftSync(metaAccount: MetaAccount, chain: Chain)

    suspend fun fullNftSync(nft: Nft)
}


class NftSyncTrigger(val chain: Chain)
