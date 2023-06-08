package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface TransactionHistoryRepository {

    suspend fun syncOperationsFirstPage(
        pageSize: Int,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    )

    suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation>

    suspend fun operationsFirstPageFlow(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): Flow<DataPage<Operation>>
}
