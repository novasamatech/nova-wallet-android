package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.coingecko.PriceInfo
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.core_db.model.PhishingAddressLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.findMetaAccountOrThrow
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi.Companion.getRecentRateFieldName
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetLocalToAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.phishing.PhishingApi
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class WalletRepositoryImpl(
    private val substrateSource: SubstrateRemoteSource,
    private val operationDao: OperationDao,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val phishingApi: PhishingApi,
    private val accountRepository: AccountRepository,
    private val assetCache: AssetCache,
    private val phishingAddressDao: PhishingAddressDao,
    private val coingeckoApi: CoingeckoApi,
    private val chainRegistry: ChainRegistry,
) : WalletRepository {

    override fun syncedAssetsFlow(metaId: Long): Flow<List<Asset>> {
        return combine(
            chainRegistry.chainsById,
            assetCache.observeSyncedAssets(metaId)
        ) { chainsById, assetsLocal ->
            assetsLocal.map { asset ->
                mapAssetLocalToAsset(asset, chainsById.chainAsset(asset.assetAndChainId))
            }
        }
    }

    override suspend fun getSyncedAssets(metaId: Long): List<Asset> = withContext(Dispatchers.Default) {
        val chainsById = chainRegistry.chainsById.first()
        val assetsLocal = assetCache.getSyncedAssets(metaId)

        assetsLocal.map {
            mapAssetLocalToAsset(it, chainsById.chainAsset(it.assetAndChainId))
        }
    }

    override suspend fun getSupportedAssets(metaId: Long): List<Asset> = withContext(Dispatchers.Default) {
        val chainsById = chainRegistry.chainsById.first()
        val assetsLocal = assetCache.getSupportedAssets(metaId)

        assetsLocal.map {
            mapAssetLocalToAsset(it, chainsById.chainAsset(it.assetAndChainId))
        }
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
            val priceStats = getAssetPriceCoingecko(syncingPriceIdsToSymbols.keys, currency.coingeckoId)

            val updatedTokens = priceStats.flatMap { (priceId, tokenStats) ->
                syncingPriceIdsToSymbols[priceId]?.let { symbols ->
                    symbols.map { symbol ->
                        TokenLocal(symbol, tokenStats.price, currency.id, tokenStats.rateChange)
                    }
                } ?: emptyList()
            }

            assetCache.insertTokens(updatedTokens)
        }
    }

    override suspend fun syncAssetRates(asset: Chain.Asset, currency: Currency) {
        val priceId = asset.priceId ?: return

        val priceStats = getAssetPriceCoingecko(setOf(priceId), currency.coingeckoId)

        val updatedTokens = priceStats.map { (_, tokenStats) ->
            TokenLocal(asset.symbol, tokenStats.price, currency.id, tokenStats.rateChange)
        }

        assetCache.insertTokens(updatedTokens)
    }

    override fun assetFlow(accountId: AccountId, chainAsset: Chain.Asset): Flow<Asset> {
        return flow {
            val metaAccount = accountRepository.findMetaAccountOrThrow(accountId)

            emitAll(assetFlow(metaAccount.id, chainAsset))
        }
    }

    override fun assetFlow(metaId: Long, chainAsset: Chain.Asset): Flow<Asset> {
        return assetCache.observeAsset(metaId, chainAsset.chainId, chainAsset.id)
            .map { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getAsset(accountId: AccountId, chainAsset: Chain.Asset): Asset? {
        val assetLocal = getAsset(accountId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getAsset(metaId: Long, chainAsset: Chain.Asset): Asset? {
        val assetLocal = assetCache.getAssetWithToken(metaId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getContacts(
        accountId: AccountId,
        chain: Chain,
        query: String,
    ): Set<String> {
        return operationDao.getContacts(query, chain.addressOf(accountId), chain.id).toSet()
    }

    override suspend fun insertPendingTransfer(
        hash: String,
        assetTransfer: AssetTransfer,
        fee: BigDecimal
    ) {
        val operation = createOperation(
            hash,
            assetTransfer,
            fee,
            OperationLocal.Source.APP
        )

        operationDao.insert(operation)
    }

    override suspend fun clearAssets(fullAssetIds: List<FullChainAssetId>) {
        assetCache.clearAssets(fullAssetIds)
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

    private fun createOperation(
        hash: String,
        transfer: AssetTransfer,
        fee: BigDecimal,
        source: OperationLocal.Source,
    ): OperationLocal {
        val senderAddress = transfer.sender.addressIn(transfer.originChain)!!

        return OperationLocal.manualTransfer(
            hash = hash,
            address = senderAddress,
            chainAssetId = transfer.originChainAsset.id,
            chainId = transfer.originChainAsset.chainId,
            amount = transfer.amountInPlanks,
            senderAddress = senderAddress,
            receiverAddress = transfer.recipient,
            fee = transfer.originChain.commissionAsset.planksFromAmount(fee),
            status = OperationLocal.Status.PENDING,
            source = source
        )
    }

    private suspend fun getAssetPriceCoingecko(priceIds: Set<String>, coingeccoId: String): Map<String, PriceInfo> {
        return apiCall { coingeckoApi.getAssetPrice(priceIds.asQueryParam(), currency = coingeccoId, includeRateChange = true) }
            .mapValues {
                val price = it.value[coingeccoId]
                val recentRate = it.value[getRecentRateFieldName(coingeccoId)]
                PriceInfo(
                    price?.toBigDecimal(),
                    recentRate?.toBigDecimal()
                )
            }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T = httpExceptionHandler.wrap(block)

    private suspend fun getAsset(accountId: AccountId, chainId: String, assetId: Int) = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.findMetaAccountOrThrow(accountId)

        assetCache.getAssetWithToken(metaAccount.id, chainId, assetId)
    }

    private fun Map<ChainId, Chain>.chainAsset(ids: AssetAndChainId): Chain.Asset {
        return getValue(ids.chainId).assetsById.getValue(ids.assetId)
    }
}
