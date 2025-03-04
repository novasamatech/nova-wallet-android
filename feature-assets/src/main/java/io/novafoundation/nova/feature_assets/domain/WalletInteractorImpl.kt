package io.novafoundation.nova.feature_assets.domain

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_assets.data.repository.TransactionHistoryRepository
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.AssetFiltersRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftSyncTrigger
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.OperationsPageChange
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.enabledChainByIdFlow
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withContext

class WalletInteractorImpl(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val assetFiltersRepository: AssetFiltersRepository,
    private val chainRegistry: ChainRegistry,
    private val nftRepository: NftRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val currencyRepository: CurrencyRepository
) : WalletInteractor {

    override fun isFiltersEnabledFlow(): Flow<Boolean> {
        return assetFiltersRepository.assetFiltersFlow()
            .map { it.isNotEmpty() }
    }

    override fun filterAssets(assetsFlow: Flow<List<Asset>>): Flow<List<Asset>> {
        return combine(assetsFlow, assetFiltersRepository.assetFiltersFlow()) { assets, filters ->
            assets.applyFilters(filters)
        }
    }

    override fun assetsFlow(): Flow<List<Asset>> {
        val assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        val enabledChains = chainRegistry.enabledChainByIdFlow()

        return combine(assetsFlow, enabledChains) { assets, chainsById ->
            assets.filter { chainsById.containsKey(it.token.configuration.chainId) }
        }
    }

    override suspend fun syncAssetsRates(currency: Currency) {
        runCatching {
            walletRepository.syncAssetsRates(currency)
        }
    }

    override fun nftSyncTrigger(): Flow<NftSyncTrigger> {
        return nftRepository.initialNftSyncTrigger()
    }

    override suspend fun syncAllNfts(metaAccount: MetaAccount) {
        nftRepository.initialNftSync(metaAccount, forceOverwrite = false)
    }

    override suspend fun syncChainNfts(metaAccount: MetaAccount, chain: Chain) {
        nftRepository.initialNftSync(metaAccount, chain)
    }

    override fun chainFlow(chainId: ChainId): Flow<Chain> {
        return chainRegistry.enabledChainByIdFlow()
            .map { it.getValue(chainId) }
    }

    override fun assetFlow(chainId: ChainId, chainAssetId: Int): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val (_, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)

            walletRepository.assetFlow(metaAccount.id, chainAsset)
        }
    }

    override fun assetFlow(chainAsset: Chain.Asset): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            walletRepository.assetFlow(metaAccount.id, chainAsset)
        }
    }

    override fun commissionAssetFlow(chainId: ChainId): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val chain = chainRegistry.getChain(chainId)

            walletRepository.assetFlow(metaAccount.id, chain.commissionAsset)
        }
    }

    override fun commissionAssetFlow(chain: Chain): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            walletRepository.assetFlow(metaAccount.id, chain.commissionAsset)
        }
    }

    override fun operationsFirstPageFlow(chainId: ChainId, chainAssetId: Int): Flow<OperationsPageChange> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaAccount ->
                val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
                val accountId = metaAccount.accountIdIn(chain)!!
                val currency = currencyRepository.getSelectedCurrency()
                transactionHistoryRepository.operationsFirstPageFlow(accountId, chain, chainAsset, currency).withIndex().map { (index, cursorPage) ->
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
            val currency = currencyRepository.getSelectedCurrency()

            transactionHistoryRepository.syncOperationsFirstPage(pageSize, filters, accountId, chain, chainAsset, currency)
        }
    }

    override suspend fun getOperations(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
    ): Result<DataPage<Operation>> {
        return runCatching {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val accountId = metaAccount.requireAccountIdIn(chain)
            val currency = currencyRepository.getSelectedCurrency()

            transactionHistoryRepository.getOperations(
                pageSize = pageSize,
                pageOffset = pageOffset,
                filters = filters,
                accountId = accountId,
                chain = chain,
                chainAsset = chainAsset,
                currency = currency
            )
        }
    }

    override suspend fun groupAssetsByNetwork(
        assets: List<Asset>,
        externalBalances: List<ExternalBalance>
    ): Map<NetworkAssetGroup, List<AssetWithOffChainBalance>> {
        val chains = chainRegistry.enabledChainByIdFlow().first()

        return groupAndSortAssetsByNetwork(assets, externalBalances.aggregatedBalanceByAsset(), chains)
    }

    override suspend fun groupAssetsByToken(
        assets: List<Asset>,
        externalBalances: List<ExternalBalance>
    ): Map<TokenAssetGroup, List<AssetWithNetwork>> {
        val chains = chainRegistry.enabledChainByIdFlow().first()

        return groupAndSortAssetsByToken(assets, externalBalances.aggregatedBalanceByAsset(), chains)
    }
}
