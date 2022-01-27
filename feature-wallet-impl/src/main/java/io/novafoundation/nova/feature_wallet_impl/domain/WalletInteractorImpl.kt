package io.novafoundation.nova.feature_wallet_impl.domain

import io.novafoundation.nova.common.data.model.CursorPage
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.OperationsPageChange
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withContext

class WalletInteractorImpl(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : WalletInteractor {

    override fun assetsFlow(): Flow<List<Asset>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.assetsFlow(it.id) }
            .filter { it.isNotEmpty() }
            .map { assets ->
                val chains = chainRegistry.chainsById.first()

                val fiatByChain = assets.groupBy { it.token.configuration.chainId }
                    .mapValues { (_, assets) -> assets.sumByBigDecimal { it.token.fiatAmount(it.total) } }

                assets.sortedWith(
                    compareByDescending<Asset> { fiatByChain.getValue(it.token.configuration.chainId) }
                        .thenBy { chains.getValue(it.token.configuration.chainId).name }
                        .thenByDescending { it.token.fiatAmount(it.total) }
                        .thenBy { it.token.configuration.id }
                )
            }
    }

    override suspend fun syncAssetsRates(): Result<Unit> {
        return runCatching {
            walletRepository.syncAssetsRates()
        }
    }

    override fun assetFlow(chainId: ChainId, chainAssetId: Int): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val (_, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)

            walletRepository.assetFlow(metaAccount.id, chainAsset)
        }
    }

    override fun commissionAssetFlow(chainId: ChainId): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val chain = chainRegistry.getChain(chainId)

            walletRepository.assetFlow(metaAccount.id, chain.commissionAsset)
        }
    }

    override suspend fun getCurrentAsset(chainId: ChainId, chainAssetId: Int): Asset {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)

        return walletRepository.getAsset(metaAccount.accountIdIn(chain)!!, chainAsset)!!
    }

    override fun operationsFirstPageFlow(chainId: ChainId, chainAssetId: Int): Flow<OperationsPageChange> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaAccount ->
                val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
                val accountId = metaAccount.accountIdIn(chain)!!

                walletRepository.operationsFirstPageFlow(accountId, chain, chainAsset).withIndex().map { (index, cursorPage) ->
                    OperationsPageChange(cursorPage, accountChanged = index == 0)
                }
            }
    }

    override suspend fun syncOperationsFirstPage(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        filters: Set<TransactionFilter>,
    ) = withContext(Dispatchers.Default) {
        runCatching {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val accountId = metaAccount.accountIdIn(chain)!!

            walletRepository.syncOperationsFirstPage(pageSize, filters, accountId, chain, chainAsset)
        }
    }

    override suspend fun getOperations(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        cursor: String?,
        filters: Set<TransactionFilter>,
    ): Result<CursorPage<Operation>> {
        return runCatching {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val accountId = metaAccount.accountIdIn(chain)!!

            walletRepository.getOperations(
                pageSize,
                cursor,
                filters,
                accountId,
                chain,
                chainAsset
            )
        }
    }
}
