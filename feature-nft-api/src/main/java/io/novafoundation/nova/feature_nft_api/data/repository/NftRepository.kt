package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

interface NftRepository {

    fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun nftDetails(nftId: String): Flow<NftDetails>

    suspend fun initialNftSync(
        metaAccount: MetaAccount,
        forceOverwrite: Boolean
    )

    suspend fun initialNftSyncForChainId(
        chainId: ChainId,
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
        skipFirstBlock: Boolean = false
    ): List<Job>

    suspend fun fullNftSync(nft: Nft)

    suspend fun getAvailableChains(): List<Chain>

    suspend fun subscribeNftOwnerAccountId(nftId: String): Flow<Pair<AccountId?, NftLocal>>

    suspend fun getLocalNft(nftIdentifier: String): NftLocal

    suspend fun getLocalNfts(nftIdentifiers: List<String>): List<NftLocal>

    fun isNftTypeSupportedForSend(nftType: Nft.Type): Boolean

    suspend fun getChainForNftId(chainId: ChainId): Chain
}
