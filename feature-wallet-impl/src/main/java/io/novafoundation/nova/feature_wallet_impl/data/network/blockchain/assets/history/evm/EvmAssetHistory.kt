package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evm

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.feeUsed
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.Section
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.TransferHistoryApi
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlin.time.Duration.Companion.seconds

private const val FIRST_PAGE_INDEX = 1
private const val SECOND_PAGE_INDEX = 2

class EvmAssetHistory(
    private val etherscanApi: EtherscanTransactionsApi
) : AssetHistory {

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> {
        // we fetch transfers alongside with balance updates in EvmAssetBalance
        return Result.success(emptyList())
    }

    override fun availableOperationFilters(asset: Chain.Asset): Set<TransactionFilter> {
        return setOf(TransactionFilter.TRANSFER)
    }

    override suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        page: DataPage<Operation>
    ) {
        // we don't need anything extra
    }

    override suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ): DataPage<Operation> {
        val evmTransfersApi = chain.evmTransfersApi()

        return when (evmTransfersApi?.apiType) {
            Section.Type.ETHERSCAN -> getOperationsEtherscan(
                pageSize = pageSize,
                pageOffset = pageOffset,
                accountId = accountId,
                chain = chain,
                chainAsset = chainAsset,
                apiUrl = evmTransfersApi.url
            )

            else -> DataPage.empty()
        }
    }

    override suspend fun getSyncedPageOffset(accountId: AccountId, chain: Chain, chainAsset: Chain.Asset): PageOffset {
        val evmTransfersApi = chain.evmTransfersApi()

        return when (evmTransfersApi?.apiType) {
            Section.Type.ETHERSCAN -> {
                PageOffset.Loadable.PageNumber(page = SECOND_PAGE_INDEX)
            }

            else -> PageOffset.FullData
        }
    }

    private fun Chain.evmTransfersApi(): TransferHistoryApi? {
        return externalApi?.history?.firstOrNull {
            it.assetType == TransferHistoryApi.AssetType.EVM
        }
    }

    private suspend fun getOperationsEtherscan(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        apiUrl: String
    ): DataPage<Operation> {
        val page = when(pageOffset) {
            PageOffset.Loadable.FistPage -> FIRST_PAGE_INDEX
            is PageOffset.Loadable.PageNumber -> pageOffset.page
            else -> error("Etherscan requires page number pagination")
        }
        val erc20Config = chainAsset.requireErc20()
        val accountAddress = chain.addressOf(accountId)

        val response = etherscanApi.getOperationsHistory(
            baseUrl = apiUrl,
            contractAddress = erc20Config.contractAddress,
            accountAddress = accountAddress,
            pageNumber = page,
            pageSize = pageSize,
        )

        val operations = response.result.map { mapRemoteTransferToOperation(it, chainAsset, accountAddress) }
        val newPageOffset = if (response.result.size < pageSize) {
            PageOffset.FullData
        } else {
            PageOffset.Loadable.PageNumber(page + 1)
        }

        return DataPage(newPageOffset, operations)
    }

    private fun mapRemoteTransferToOperation(
        remote: EtherscanAccountTransfer,
        chainAsset: Chain.Asset,
        accountAddress: String,
    ): Operation {
        return Operation(
            id = remote.hash,
            address = accountAddress,
            type = Operation.Type.Transfer(
                hash = remote.hash,
                myAddress = accountAddress,
                amount = remote.value,
                receiver = remote.to,
                sender = remote.from,
                status = Operation.Status.COMPLETED,
                fee = remote.feeUsed,
            ),
            time = remote.timeStamp.seconds.inWholeMilliseconds,
            chainAsset = chainAsset
        )
    }
}
