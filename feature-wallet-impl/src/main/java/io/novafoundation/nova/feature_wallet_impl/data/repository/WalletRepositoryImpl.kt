package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.PhishingAddressLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.findMetaAccountOrThrow
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetLocalToAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.phishing.PhishingApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class WalletRepositoryImpl(
    private val substrateSource: SubstrateRemoteSource,
    private val operationDao: OperationDao,
    private val phishingApi: PhishingApi,
    private val accountRepository: AccountRepository,
    private val assetCache: AssetCache,
    private val phishingAddressDao: PhishingAddressDao,
    private val coinPriceRemoteDataSource: CoinPriceRemoteDataSource,
    private val chainRegistry: ChainRegistry,
) : WalletRepository {

    override fun syncedAssetsFlow(metaId: Long): Flow<List<Asset>> {
        return combine(
            chainRegistry.chainsById,
            assetCache.observeSyncedAssets(metaId)
        ) { chainsById, assetsLocal ->
            assetsLocal.mapNotNull { asset ->
                chainsById.chainAsset(asset.assetAndChainId)?.let { mapAssetLocalToAsset(asset, it) }
            }
        }
    }

    override suspend fun getSyncedAssets(metaId: Long): List<Asset> = withContext(Dispatchers.Default) {
        val chainsById = chainRegistry.chainsById.first()
        val assetsLocal = assetCache.getSyncedAssets(metaId)

        assetsLocal.mapNotNull { asset ->
            chainsById.chainAsset(asset.assetAndChainId)?.let { mapAssetLocalToAsset(asset, it) }
        }
    }

    override suspend fun getSupportedAssets(metaId: Long): List<Asset> = withContext(Dispatchers.Default) {
        val chainsById = chainRegistry.chainsById.first()
        val assetsLocal = assetCache.getSupportedAssets(metaId)

        assetsLocal.mapNotNull { asset ->
            chainsById.chainAsset(asset.assetAndChainId)?.let { mapAssetLocalToAsset(asset, it) }
        }
    }

    override fun supportedAssetsFlow(metaId: Long, chainAssets: List<Chain.Asset>): Flow<List<Asset>> = flowOfAll {
        val chainAssetsById = chainAssets.associateBy { AssetAndChainId(it.chainId, it.id) }

        assetCache.observeSupportedAssets(metaId).map { supportedAssets ->
            supportedAssets.mapNotNull { assetWithToken ->
                val chainAsset = chainAssetsById[assetWithToken.assetAndChainId] ?: return@mapNotNull null

                mapAssetLocalToAsset(assetWithToken, chainAsset)
            }
        }.distinctUntilChanged()
    }

    override suspend fun syncAssetsRates(currency: Currency) {
        val chains = chainRegistry.currentChains.first()

        val syncingPriceIdsToSymbols = chains.flatMap(Chain::assets)
            .filter { it.priceId != null }
            .groupBy(
                keySelector = { it.priceId!! },
                valueTransform = { it.symbol }
            )

        if (syncingPriceIdsToSymbols.isNotEmpty()) {
            val coinPriceChanges = getAssetPrices(syncingPriceIdsToSymbols.keys, currency)

            val newTokens = coinPriceChanges.flatMap { (priceId, coinPriceChange) ->
                syncingPriceIdsToSymbols[priceId]?.let { symbols ->
                    symbols.map { symbol ->
                        TokenLocal(symbol.value, coinPriceChange?.rate, currency.id, coinPriceChange?.recentRateChange)
                    }
                } ?: emptyList()
            }

            assetCache.updateTokens(newTokens)
        } else {
            assetCache.deleteAllTokens()
        }
    }

    override suspend fun syncAssetRates(asset: Chain.Asset, currency: Currency) {
        val priceId = asset.priceId ?: return

        val coinPriceChange = getAssetPrice(priceId, currency)

        val token = TokenLocal(asset.symbol.value, coinPriceChange?.rate, currency.id, coinPriceChange?.recentRateChange)

        assetCache.insertToken(token)
    }

    override fun assetFlow(accountId: AccountId, chainAsset: Chain.Asset): Flow<Asset> {
        return flow {
            val metaAccount = accountRepository.findMetaAccountOrThrow(accountId, chainAsset.chainId)

            emitAll(assetFlow(metaAccount.id, chainAsset))
        }
    }

    override fun assetFlow(metaId: Long, chainAsset: Chain.Asset): Flow<Asset> {
        return assetCache.observeAsset(metaId, chainAsset.chainId, chainAsset.id)
            .map { mapAssetLocalToAsset(it, chainAsset) }
            .distinctUntilChanged()
    }

    override fun assetsFlow(metaId: Long, chainAssets: List<Chain.Asset>): Flow<List<Asset>> = flowOfAll {
        val chainAssetsById = chainAssets.associateBy { AssetAndChainId(it.chainId, it.id) }

        assetCache.observeAssets(metaId, chainAssetsById.keys).map { dbAssets ->
            dbAssets.mapNotNull { assetWithToken ->
                val chainAsset = chainAssetsById[assetWithToken.assetAndChainId] ?: return@mapNotNull null

                mapAssetLocalToAsset(assetWithToken, chainAsset)
            }
        }.distinctUntilChanged()
    }

    override suspend fun getAsset(accountId: AccountId, chainAsset: Chain.Asset): Asset? {
        val assetLocal = getAsset(accountId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getAsset(metaId: Long, chainAsset: Chain.Asset): Asset? {
        val assetLocal = assetCache.getAssetWithToken(metaId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun insertPendingTransfer(
        hash: String,
        assetTransfer: AssetTransfer,
        fee: BigDecimal
    ) {
        val operation = createAppOperation(
            hash = hash,
            transfer = assetTransfer,
            fee = fee,
        )

        operationDao.insert(operation)
    }

    override suspend fun clearAssets(assetIds: List<FullChainAssetId>) {
        assetCache.clearAssets(assetIds)
    }

    // TODO adapt for ethereum chains
    override suspend fun updatePhishingAddresses() = withContext(Dispatchers.Default) {
        val accountIds = phishingApi.getPhishingAddresses().values.flatten()
            .map { it.toAccountId().toHexString(withPrefix = true) }

        val phishingAddressesLocal = accountIds.map(::PhishingAddressLocal)

        phishingAddressDao.clearTable()
        phishingAddressDao.insert(phishingAddressesLocal)
    }

    // TODO adapt for ethereum chains
    override suspend fun isAccountIdFromPhishingList(accountId: AccountId) = withContext(Dispatchers.Default) {
        val phishingAddresses = phishingAddressDao.getAllAddresses()

        phishingAddresses.contains(accountId.toHexString(withPrefix = true))
    }

    override suspend fun getAccountFreeBalance(chainId: ChainId, accountId: AccountId) =
        substrateSource.getAccountInfo(chainId, accountId).data.free

    private fun createAppOperation(
        hash: String,
        transfer: AssetTransfer,
        fee: BigDecimal,
    ): OperationLocal {
        val senderAddress = transfer.sender.requireAddressIn(transfer.originChain)

        return OperationLocal.manualTransfer(
            hash = hash,
            address = senderAddress,
            chainAssetId = transfer.originChainAsset.id,
            chainId = transfer.originChainAsset.chainId,
            amount = transfer.amountInPlanks,
            senderAddress = senderAddress,
            receiverAddress = transfer.recipient,
            fee = transfer.commissionAssetToken.planksFromAmount(fee),
            status = OperationBaseLocal.Status.PENDING,
            source = OperationBaseLocal.Source.APP
        )
    }

    private suspend fun getAssetPrices(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return coinPriceRemoteDataSource.getCoinRates(priceIds, currency)
    }

    private suspend fun getAssetPrice(priceId: String, currency: Currency): CoinRateChange? {
        return coinPriceRemoteDataSource.getCoinRate(priceId, currency)
    }

    private suspend fun getAsset(accountId: AccountId, chainId: String, assetId: Int) = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.findMetaAccountOrThrow(accountId, chainId)

        assetCache.getAssetWithToken(metaAccount.id, chainId, assetId)
    }

    private fun Map<ChainId, Chain>.chainAsset(ids: AssetAndChainId): Chain.Asset? {
        return get(ids.chainId)?.assetsById?.get(ids.assetId)
    }
}
