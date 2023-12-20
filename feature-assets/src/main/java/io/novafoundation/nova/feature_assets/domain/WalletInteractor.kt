package io.novafoundation.nova.feature_assets.domain

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_nft_api.data.repository.NftSyncTrigger
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.OperationsPageChange
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface WalletInteractor {

    fun isFiltersEnabledFlow(): Flow<Boolean>

    fun filterAssets(assetsFlow: Flow<List<Asset>>): Flow<List<Asset>>

    fun assetsFlow(): Flow<List<Asset>>

    suspend fun syncAssetsRates(currency: Currency)

    fun nftSyncTrigger(): Flow<NftSyncTrigger>

    suspend fun syncAllNfts(metaAccount: MetaAccount)

    suspend fun syncChainNfts(metaAccount: MetaAccount, chain: Chain)

    fun assetFlow(chainId: ChainId, chainAssetId: Int): Flow<Asset>

    fun assetFlow(chainAsset: Chain.Asset): Flow<Asset>

    fun commissionAssetFlow(chainId: ChainId): Flow<Asset>

    fun commissionAssetFlow(chain: Chain): Flow<Asset>

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
        externalBalances: List<ExternalBalance>
    ): Map<AssetGroup, List<AssetWithOffChainBalance>>
}
