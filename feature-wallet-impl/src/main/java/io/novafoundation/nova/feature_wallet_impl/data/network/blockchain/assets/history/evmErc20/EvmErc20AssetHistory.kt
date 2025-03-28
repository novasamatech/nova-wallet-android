package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmErc20

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.convertPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.findNearestCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.isZeroTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.EvmAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.feeUsed
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlin.time.Duration.Companion.seconds

class EvmErc20AssetHistory(
    private val etherscanTransactionsApi: EtherscanTransactionsApi,
    coinPriceRepository: CoinPriceRepository
) : EvmAssetHistory(coinPriceRepository) {

    override suspend fun fetchEtherscanOperations(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        apiUrl: String,
        page: Int,
        pageSize: Int,
        currency: Currency
    ): List<Operation> {
        val erc20Config = chainAsset.requireErc20()
        val accountAddress = chain.addressOf(accountId)

        val response = etherscanTransactionsApi.getErc20Transfers(
            baseUrl = apiUrl,
            contractAddress = erc20Config.contractAddress,
            accountAddress = accountAddress,
            pageNumber = page,
            pageSize = pageSize,
            chainId = chain.id
        )

        val priceHistory = getPriceHistory(chainAsset, currency)

        return response.result.map {
            val coinRate = priceHistory.findNearestCoinRate(it.timeStamp)
            mapRemoteTransferToOperation(it, chainAsset, accountAddress, coinRate)
        }
    }

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId,
    ): List<RealtimeHistoryUpdate> {
        // we fetch transfers alongside with balance updates in EvmAssetBalance
        return emptyList()
    }

    override fun availableOperationFilters(chain: Chain, asset: Chain.Asset): Set<TransactionFilter> {
        return setOf(TransactionFilter.TRANSFER)
    }

    override fun isOperationSafe(operation: Operation): Boolean {
        return !operation.isZeroTransfer()
    }

    private fun mapRemoteTransferToOperation(
        remote: EtherscanAccountTransfer,
        chainAsset: Chain.Asset,
        accountAddress: String,
        coinRate: CoinRate?
    ): Operation {
        return Operation(
            id = remote.hash,
            address = accountAddress,
            extrinsicHash = remote.hash,
            type = Operation.Type.Transfer(
                myAddress = accountAddress,
                amount = remote.value,
                fiatAmount = coinRate?.convertPlanks(chainAsset, remote.value),
                receiver = remote.to,
                sender = remote.from,
                fee = remote.feeUsed
            ),
            time = remote.timeStamp.seconds.inWholeMilliseconds,
            chainAsset = chainAsset,
            status = Operation.Status.COMPLETED,
        )
    }
}
