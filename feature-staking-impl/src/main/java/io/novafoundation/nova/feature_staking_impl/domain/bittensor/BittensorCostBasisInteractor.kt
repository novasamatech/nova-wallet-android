package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class BittensorCostBasisRequest(val context: BittensorEstimateContext, val balanceRevision: BigInteger)

class BittensorCostBasisInteractor(
    private val historyRepository: BittensorOperationHistoryRepository,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository
) {

    fun observeRequests(context: Flow<BittensorEstimateContext?>): Flow<BittensorCostBasisRequest?> =
        context.distinctUntilChanged()
            .flatMapLatest { context ->
                if (context == null) {
                    flowOf(null)
                } else {
                    walletRepository.assetFlow(context.metaId, context.chain.utilityAsset)
                        .map { BittensorCostBasisRequest(context, it.transferableInPlanks) }
                }
            }
            .distinctUntilChanged()

    suspend fun costBasis(request: BittensorCostBasisRequest): BittensorEstimatedCostBasis {
        val context = request.context
        val account = accountRepository.getMetaAccount(context.metaId)
        val address = context.chain.addressOf(account.requireAccountIdIn(context.chain))
        val history = historyRepository.history(address, request.balanceRevision)
        return BittensorEstimatedCostBasis(history.operations, history.allProviderPagesRead)
    }
}
