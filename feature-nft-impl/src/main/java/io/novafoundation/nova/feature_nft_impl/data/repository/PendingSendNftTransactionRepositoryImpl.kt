package io.novafoundation.nova.feature_nft_impl.data.repository

import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.feature_nft_api.data.repository.PendingSendNftTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PendingSendNftTransactionRepositoryImpl: PendingSendNftTransactionRepository {

    private val pendingSendTransactionsNftIds = MutableStateFlow(setOf<String>())

    override fun onNftSendTransactionSubmitted(nftId: String) {
        pendingSendTransactionsNftIds.value = pendingSendTransactionsNftIds.value.added(nftId)
    }

    override fun removeOldPendingTransactions(myNftIds: List<String>) {
        pendingSendTransactionsNftIds.value = pendingSendTransactionsNftIds.value.minus(myNftIds.toSet())
    }

    override fun getPendingSendTransactionsNftIds(): Flow<Set<String>> {
        return pendingSendTransactionsNftIds.asStateFlow()
    }
}
