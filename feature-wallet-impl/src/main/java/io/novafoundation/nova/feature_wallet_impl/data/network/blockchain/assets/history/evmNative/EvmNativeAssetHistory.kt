package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmNative

import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Extrinsic.Content
import io.novafoundation.nova.feature_wallet_api.domain.model.convertPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.findNearestCoinRate
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.EvmAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanNormalTxResponse
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.feeUsed
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.isTransfer
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.awaitCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

class EvmNativeAssetHistory(
    private val chainRegistry: ChainRegistry,
    private val etherscanTransactionsApi: EtherscanTransactionsApi,
    private val walletRepository: WalletRepository,
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
        val accountAddress = chain.addressOf(accountId)

        val response = etherscanTransactionsApi.getNormalTxsHistory(
            baseUrl = apiUrl,
            accountAddress = accountAddress,
            pageNumber = page,
            pageSize = pageSize,
            chainId = chain.id
        )

        val earliestOperationTimestamp = response.result.minOfOrNull { it.timeStamp } ?: 0L
        val latestOperationTimestamp = response.result.maxOfOrNull { it.timeStamp } ?: 0L
        val coinPriceRange = getCoinPriceRange(chainAsset, currency, earliestOperationTimestamp, latestOperationTimestamp)

        return response.result
            .map {
                val coinRate = coinPriceRange.findNearestCoinRate(it.timeStamp)
                mapRemoteNormalTxToOperation(it, chainAsset, accountAddress, coinRate)
            }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId,
        currency: Currency
    ): Result<List<TransferExtrinsic>> = runCatching {
        val ethereumApi = chainRegistry.awaitCallEthereumApiOrThrow(chain.id)

        val block = ethereumApi.ethGetBlockByHash(blockHash, true).sendSuspend()
        val txs = block.block.transactions as List<TransactionResult<EthBlock.TransactionObject>>

        txs.mapNotNull {
            val tx = it.get()

            val isTransfer = tx.input.removeHexPrefix().isEmpty()
            val relatesToUs = tx.relatesTo(accountId)

            if (!(isTransfer && relatesToUs)) return@mapNotNull null

            val txReceipt = ethereumApi.ethGetTransactionReceipt(tx.hash).sendSuspend().transactionReceipt.getOrNull()

            TransferExtrinsic(
                senderId = chain.accountIdOf(tx.from),
                recipientId = chain.accountIdOf(tx.to),
                amountInPlanks = tx.value,
                chainAsset = chainAsset,
                status = txReceipt.extrinsicStatus(),
                hash = tx.hash
            )
        }
    }

    override fun availableOperationFilters(asset: Chain.Asset): Set<TransactionFilter> {
        return setOf(TransactionFilter.TRANSFER, TransactionFilter.EXTRINSIC)
    }

    override fun isOperationSafe(operation: Operation): Boolean {
        return true
    }

    private fun TransactionReceipt?.extrinsicStatus(): ExtrinsicStatus {
        return when (this?.isStatusOK) {
            true -> ExtrinsicStatus.SUCCESS
            false -> ExtrinsicStatus.FAILURE
            null -> ExtrinsicStatus.UNKNOWN
        }
    }

    private fun Transaction.relatesTo(accountId: AccountId): Boolean {
        return from.ethAccountIdMatches(accountId) || to.ethAccountIdMatches(accountId)
    }

    private fun String?.ethAccountIdMatches(other: AccountId): Boolean {
        return other.contentEquals(this?.ethereumAddressToAccountId())
    }

    private fun mapRemoteNormalTxToOperation(
        remote: EtherscanNormalTxResponse,
        chainAsset: Chain.Asset,
        accountAddress: String,
        coinRate: CoinRate?
    ): Operation {
        val type = if (remote.isTransfer) {
            mapNativeTransferToTransfer(remote, accountAddress, chainAsset, coinRate)
        } else {
            mapContractCallToExtrinsic(remote, chainAsset, coinRate)
        }

        return Operation(
            id = remote.hash,
            address = accountAddress,
            type = type,
            time = remote.timeStamp.seconds.inWholeMilliseconds,
            chainAsset = chainAsset
        )
    }

    private fun mapNativeTransferToTransfer(
        remote: EtherscanNormalTxResponse,
        accountAddress: String,
        chainAsset: Chain.Asset,
        coinRate: CoinRate?
    ): Operation.Type.Transfer {
        return Operation.Type.Transfer(
            hash = remote.hash,
            myAddress = accountAddress,
            amount = remote.value,
            fiatAmount = coinRate?.convertPlanks(chainAsset, remote.value),
            receiver = remote.to,
            sender = remote.from,
            status = remote.operationStatus(),
            fee = remote.feeUsed
        )
    }

    private fun mapContractCallToExtrinsic(
        remote: EtherscanNormalTxResponse,
        chainAsset: Chain.Asset,
        coinRate: CoinRate?
    ): Operation.Type.Extrinsic {
        return Operation.Type.Extrinsic(
            hash = remote.hash,
            content = Content.ContractCall(
                contractAddress = remote.to,
                function = remote.functionName,
            ),
            fee = remote.feeUsed,
            fiatFee = coinRate?.convertPlanks(chainAsset, remote.feeUsed),
            status = remote.operationStatus()
        )
    }

    private fun EtherscanNormalTxResponse.operationStatus(): Operation.Status {
        return if (txReceiptStatus == BigInteger.ONE) {
            Operation.Status.COMPLETED
        } else {
            Operation.Status.FAILED
        }
    }
}
