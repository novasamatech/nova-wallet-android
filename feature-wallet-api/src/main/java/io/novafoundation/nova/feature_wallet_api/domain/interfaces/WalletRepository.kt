package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface WalletRepository {

    fun syncedAssetsFlow(metaId: Long): Flow<List<Asset>>

    suspend fun getSyncedAssets(metaId: Long): List<Asset>
    suspend fun getSupportedAssets(metaId: Long): List<Asset>

    fun supportedAssetsFlow(metaId: Long, chainAssets: List<Chain.Asset>): Flow<List<Asset>>

    suspend fun syncAssetsRates(currency: Currency)
    suspend fun syncAssetRates(asset: Chain.Asset, currency: Currency)

    fun assetFlow(
        accountId: AccountId,
        chainAsset: Chain.Asset
    ): Flow<Asset>

    fun assetFlow(
        metaId: Long,
        chainAsset: Chain.Asset
    ): Flow<Asset>

    fun assetFlowOrNull(
        metaId: Long,
        chainAsset: Chain.Asset
    ): Flow<Asset?>

    fun assetsFlow(
        metaId: Long,
        chainAssets: List<Chain.Asset>
    ): Flow<List<Asset>>

    suspend fun getAsset(
        accountId: AccountId,
        chainAsset: Chain.Asset
    ): Asset?

    suspend fun getAsset(
        metaId: Long,
        chainAsset: Chain.Asset
    ): Asset?

    suspend fun insertPendingTransfer(
        hash: String,
        assetTransfer: AssetTransfer,
        fee: SubmissionFee
    )

    suspend fun clearAssets(assetIds: List<FullChainAssetId>)

    suspend fun updatePhishingAddresses()

    suspend fun isAccountIdFromPhishingList(accountId: AccountId): Boolean
}
