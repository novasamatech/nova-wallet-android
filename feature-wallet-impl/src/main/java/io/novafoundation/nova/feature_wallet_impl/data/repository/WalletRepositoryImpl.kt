package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.model.CursorPage
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.coingecko.PriceInfo
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.utils.defaultOnNull
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetWithToken
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
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetLocalToAsset
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapNodeToOperation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationLocalToOperation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationToOperationLocalDb
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_impl.data.network.coingecko.CoingeckoApi.Companion.getRecentRateFieldName
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryHistoryRequest
import io.novafoundation.nova.feature_wallet_impl.data.network.phishing.PhishingApi
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.historySupported
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class WalletRepositoryImpl(
    private val substrateSource: SubstrateRemoteSource,
    private val operationDao: OperationDao,
    private val walletOperationsApi: SubQueryOperationsApi,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val phishingApi: PhishingApi,
    private val accountRepository: AccountRepository,
    private val assetCache: AssetCache,
    private val walletConstants: WalletConstants,
    private val phishingAddressDao: PhishingAddressDao,
    private val cursorStorage: TransferCursorStorage,
    private val coingeckoApi: CoingeckoApi,
    private val chainRegistry: ChainRegistry,
    private val tokenDao: TokenDao,
) : WalletRepository {

    override fun assetsFlow(metaId: Long): Flow<List<Asset>> {
        return combine(
            chainRegistry.chainsById,
            assetCache.observeAssets(metaId)
        ) { chainsById, assetsLocal ->
            assetsLocal.map { asset ->
                mapAssetToLocalAsset(chainsById, asset)
            }
        }
    }

    override suspend fun getAssets(metaId: Long): List<Asset> = withContext(Dispatchers.Default) {
        val chainsById = chainRegistry.chainsById.first()
        val assetsLocal = assetCache.getAssets(metaId)

        assetsLocal.map {
            mapAssetToLocalAsset(chainsById, it)
        }
    }

    private fun mapAssetToLocalAsset(
        chainsById: Map<ChainId, Chain>,
        assetLocal: AssetWithToken,
    ): Asset {
        val chainAsset = chainsById.getValue(assetLocal.asset.chainId)
            .assetsBySymbol
            .getValue(assetLocal.token.tokenSymbol)

        return mapAssetLocalToAsset(assetLocal, chainAsset = chainAsset)
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

    override fun assetFlow(accountId: AccountId, chainAsset: Chain.Asset): Flow<Asset> {
        return flow {
            val metaAccount = accountRepository.findMetaAccountOrThrow(accountId)

            emitAll(assetFlow(metaAccount.id, chainAsset))
        }
    }

    override fun assetFlow(metaId: Long, chainAsset: Chain.Asset): Flow<Asset> {
        return assetCache.observeAsset(metaId, chainAsset.chainId, chainAsset.id)
            .map {
                it.defaultOnNull {
                    val asset = AssetLocal.createEmpty(chainAsset.id, chainAsset.chainId, metaId)
                    val tokenWithCurrency = tokenDao.getToken(chainAsset.symbol)
                    var token = tokenWithCurrency.token
                    val currency = tokenWithCurrency.currency
                    if (token == null) {
                        token = TokenLocal.createEmpty(chainAsset.symbol, currency.id)
                    }
                    AssetWithToken(asset, token, currency)
                }
            }
            .map { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getAsset(accountId: AccountId, chainAsset: Chain.Asset): Asset? {
        val assetLocal = getAsset(accountId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun getAsset(metaId: Long, chainAsset: Chain.Asset): Asset? {
        val assetLocal = assetCache.getAsset(metaId, chainAsset.chainId, chainAsset.id)

        return assetLocal?.let { mapAssetLocalToAsset(it, chainAsset) }
    }

    override suspend fun syncOperationsFirstPage(
        pageSize: Int,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
    ) {
        val accountAddress = chain.addressOf(accountId)
        val page = getOperations(pageSize, cursor = null, filters, accountId, chain, chainAsset)

        val elements = page.map { mapOperationToOperationLocalDb(it, chainAsset, OperationLocal.Source.SUBQUERY) }

        cursorStorage.saveCursor(chain.id, chainAsset.id, accountId, page.nextCursor)
        operationDao.insertFromSubquery(accountAddress, chain.id, chainAsset.id, elements)
    }

    override suspend fun getOperations(
        pageSize: Int,
        cursor: String?,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): CursorPage<Operation> {
        return withContext(Dispatchers.Default) {
            if (!chain.historySupported) {
                return@withContext CursorPage(nextCursor = null, items = emptyList())
            }

            val request = SubqueryHistoryRequest(
                accountAddress = chain.addressOf(accountId),
                pageSize = pageSize,
                cursor = cursor,
                filters = filters,
                assetType = chainAsset.type
            )
            val response = walletOperationsApi.getOperationsHistory(
                url = chain.externalApi!!.history!!.url,
                request
            ).data.query

            val pageInfo = response.historyElements.pageInfo

            val operations = response.historyElements.nodes.map { mapNodeToOperation(it, chainAsset) }

            CursorPage(pageInfo.endCursor, operations)
        }
    }

    override fun operationsFirstPageFlow(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): Flow<CursorPage<Operation>> {
        val accountAddress = chain.addressOf(accountId)

        return operationDao.observe(accountAddress, chain.id, chainAsset.id)
            .mapList {
                mapOperationLocalToOperation(it, chainAsset)
            }
            .mapLatest { operations ->
                val cursor = if (chain.historySupported) {
                    cursorStorage.awaitCursor(chain.id, chainAsset.id, accountId)
                } else {
                    null
                }

                CursorPage(cursor, operations)
            }
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

        assetCache.getAsset(metaAccount.id, chainId, assetId)
    }
}
