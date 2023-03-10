package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmNative

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class EvmNativeAssetHistory : AssetHistory {

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> {
        // TODO Evm native asset tx history
        return Result.success(emptyList())
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
}
