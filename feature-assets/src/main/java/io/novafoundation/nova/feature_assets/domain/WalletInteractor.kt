package io.novafoundation.nova.feature_assets.domain

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.OperationsPageChange
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface WalletInteractor {

    fun filterAssets(assetsFlow: Flow<List<Asset>>): Flow<List<Asset>>

    fun assetsFlow(): Flow<List<Asset>>

    suspend fun syncAssetsRates(currency: Currency)

    suspend fun syncNfts(metaAccount: MetaAccount)

    fun assetFlow(chainId: ChainId, chainAssetId: Int): Flow<Asset>

    fun commissionAssetFlow(chainId: ChainId): Flow<Asset>

    fun operationsFirstPageFlow(chainId: ChainId, chainAssetId: Int): Flow<OperationsPageChange>

    suspend fun syncOperationsFirstPage(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        filters: Set<TransactionFilter>,
    ): Result<*>

    suspend fun getOperations(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>
    ): Result<DataPage<Operation>>

    suspend fun groupAssets(
        assets: List<Asset>,
        offChainBalances: Map<FullChainAssetId, BigInteger>
    ): Map<AssetGroup, List<AssetWithOffChainBalance>>
}
