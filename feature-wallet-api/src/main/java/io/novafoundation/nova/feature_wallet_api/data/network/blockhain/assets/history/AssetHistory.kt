package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface AssetHistory {

    suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId,
    ): List<RealtimeHistoryUpdate>

    fun availableOperationFilters(chain: Chain, asset: Chain.Asset): Set<TransactionFilter>

    suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        page: Result<DataPage<Operation>>
    )

    suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency,
    ): DataPage<Operation>

    suspend fun getSyncedPageOffset(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ): PageOffset

    /**
     * Checks if operation is not a phishing one
     */
    fun isOperationSafe(operation: Operation): Boolean
}
