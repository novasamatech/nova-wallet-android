package io.novafoundation.nova.feature_nft_api.data.repository

import kotlinx.coroutines.flow.Flow

interface PendingSendNftTransactionRepository {

    fun onNftSendTransactionSubmitted(nftId: String)

    fun removeOldPendingTransactions(myNftIds: List<String>)

    fun getPendingSendTransactionsNftIds(): Flow<Set<String>>
}
