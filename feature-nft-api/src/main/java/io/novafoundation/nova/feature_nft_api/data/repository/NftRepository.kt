package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface NftRepository {

    fun allNftWithMetadataFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun nftDetails(nftId: String): Flow<NftDetails>

    suspend fun initialNftSync(metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun fullNftSync(nft: Nft)

    suspend fun getAvailableChains(): List<Chain>

    fun subscribeNftOwnerAddress(nftLocal: NftLocal): Flow<String>

    suspend fun getLocalNft(nftIdentifier: String): NftLocal

    suspend fun getLocalNftOrNull(nftIdentifier: String): NftLocal?

    fun onNftSendTransactionSubmitted(nftLocal: NftLocal)

    fun removeOldPendingTransactions(myNftIds: List<NftLocal>)

    fun getPendingSendTransactionsNftLocals(): Flow<Set<NftLocal>>

    fun isNftTypeSupportedForSend(nftType: Nft.Type): Boolean
}
