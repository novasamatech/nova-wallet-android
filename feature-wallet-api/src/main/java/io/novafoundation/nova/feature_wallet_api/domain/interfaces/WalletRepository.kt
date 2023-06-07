package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.BigInteger

interface WalletRepository {

    fun syncedAssetsFlow(metaId: Long): Flow<List<Asset>>

    suspend fun getSyncedAssets(metaId: Long): List<Asset>
    suspend fun getSupportedAssets(metaId: Long): List<Asset>

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

    suspend fun getAsset(
        accountId: AccountId,
        chainAsset: Chain.Asset
    ): Asset?

    suspend fun getAsset(
        metaId: Long,
        chainAsset: Chain.Asset
    ): Asset?

    suspend fun getContacts(
        accountId: AccountId,
        chain: Chain,
        query: String
    ): Set<String>

    suspend fun insertPendingTransfer(
        hash: String,
        assetTransfer: AssetTransfer,
        fee: BigDecimal
    )

    suspend fun clearAssets(chainAssets: List<Chain.Asset>)

    suspend fun updatePhishingAddresses()

    suspend fun isAccountIdFromPhishingList(accountId: AccountId): Boolean

    suspend fun getAccountFreeBalance(chainId: ChainId, accountId: AccountId): BigInteger
}
