package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface NftRepository {

    fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>>

    fun nftDetails(nftId: String): Flow<NftDetails>

    suspend fun initialNftSync(metaAccount: MetaAccount, forceOverwrite: Boolean)

    suspend fun fullNftSync(nft: Nft)

    suspend fun getAvailableChains(): List<Chain>

    fun subscribeNftOwnerAddress(nftLocal: NftLocal): Flow<String>

    suspend fun getLocalNft(nftIdentifier: String): NftLocal

    fun onNftSendTransactionSubmitted(nftId: String)

    fun removeOldPendingTransactions(myNftIds: List<String>)

    fun getPendingSendTransactionsNftIds(): Flow<Set<String>>
}
