package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evm

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class EvmAssetHistory : AssetHistory {

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
}
