package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmNative

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.ethereumApi
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import kotlin.jvm.optionals.getOrNull

class EvmNativeAssetHistory(
    private val chainRegistry: ChainRegistry,
) : AssetHistory {

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> = runCatching {
        val web3Api = chainRegistry.ethereumApi(chain.id)
        val block = web3Api.ethGetBlockByHash(blockHash, true).sendSuspend()
        val txs = block.block.transactions as List<TransactionResult<EthBlock.TransactionObject>>

        txs.mapNotNull {
            val tx = it.get()

            val isTransfer = tx.input.removeHexPrefix().isEmpty()
            val relatesToUs = tx.relatesTo(accountId)

            if (!(isTransfer && relatesToUs)) return@mapNotNull null

            val txReceipt = web3Api.ethGetTransactionReceipt(tx.hash).sendSuspend().transactionReceipt.getOrNull()

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

    override suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        page: DataPage<Operation>
    ) {
        // TODO Evm native asset tx history
    }

    override suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ): DataPage<Operation> {
        // TODO Evm native asset tx history
        return DataPage.empty()
    }

    override suspend fun getSyncedPageOffset(accountId: AccountId, chain: Chain, chainAsset: Chain.Asset): PageOffset {
        // TODO Evm native asset tx history
        return PageOffset.FullData
    }

    private fun TransactionReceipt?.extrinsicStatus(): ExtrinsicStatus {
        return when(this?.isStatusOK){
            true -> ExtrinsicStatus.SUCCESS
            false -> ExtrinsicStatus.FAILURE
            null -> ExtrinsicStatus.UNKNOWN
        }
    }

    private fun Transaction.relatesTo(accountId: AccountId): Boolean {
        return from.ethAccountIdMatches(accountId) || to.ethAccountIdMatches(accountId)
    }

    private fun String.ethAccountIdMatches(other: AccountId): Boolean {
        return ethereumAddressToAccountId().contentEquals(other)
    }
}
