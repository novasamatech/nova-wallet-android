package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.satisfies
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

private const val FIRST_PAGE_INDEX = 1
private const val SECOND_PAGE_INDEX = 2

abstract class EvmAssetHistory(
    coinPriceRepository: CoinPriceRepository
) : BaseAssetHistory(coinPriceRepository) {

    abstract suspend fun fetchEtherscanOperations(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        apiUrl: String,
        page: Int,
        pageSize: Int,
        currency: Currency
    ): List<Operation>

    override suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        page: Result<DataPage<Operation>>
    ) {
        // nothing to do
    }

    override suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation> {
        val evmTransfersApi = chain.evmTransfersApi() ?: return DataPage.empty()

        return getOperationsEtherscan(
            pageSize,
            pageOffset,
            filters,
            accountId,
            chain,
            chainAsset,
            evmTransfersApi.url,
            currency
        )
    }

    override suspend fun getSyncedPageOffset(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ): PageOffset {
        val evmTransfersApi = chain.evmTransfersApi()

        return if (evmTransfersApi != null) {
            PageOffset.Loadable.PageNumber(page = SECOND_PAGE_INDEX)
        } else {
            PageOffset.FullData
        }
    }

    private suspend fun getOperationsEtherscan(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        apiUrl: String,
        currency: Currency
    ): DataPage<Operation> {
        val page = when (pageOffset) {
            PageOffset.Loadable.FirstPage -> FIRST_PAGE_INDEX
            is PageOffset.Loadable.PageNumber -> pageOffset.page
            else -> error("Etherscan requires page number pagination")
        }

        val operations = fetchEtherscanOperations(chain, chainAsset, accountId, apiUrl, page, pageSize, currency)

        val newPageOffset = if (operations.size < pageSize) {
            PageOffset.FullData
        } else {
            PageOffset.Loadable.PageNumber(page + 1)
        }

        val filteredOperations = operations.filter { it.type.satisfies(filters) }

        return DataPage(newPageOffset, filteredOperations)
    }

    fun Chain.evmTransfersApi(): Chain.ExternalApi.Transfers.Evm? {
        return externalApi()
    }
}
