package io.novafoundation.nova.feature_ahm_impl.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.domain.balance.totalBalance
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.runtime.ext.UTILITY_ASSET_ID
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.hash.isPositive

private const val CHAINS_WITH_ASSET_BALANCE = "CHAINS_WITH_ASSET_BALANCE"
private const val CHAIN_MIGRATION_INFO_SHOWN_PREFIX = "CHAIN_MIGRATION_INFO_SHOWN_PREFIX_"

class RealChainMigrationRepository(
    private val assetDao: AssetDao,
    private val preferences: Preferences
) : ChainMigrationRepository {

    override suspend fun cacheBalancesForChainMigrationDetection() {
        val chainsWithAssetBalance = assetDao.getAssetsById(id = UTILITY_ASSET_ID)
            .filter { it.hasBalance() }
            .mapToSet { it.chainId }

        saveToStorage(chainsWithAssetBalance)
    }

    override fun isMigrationDetailsWasShown(chainId: String): Boolean {
        return preferences.getBoolean(getChainInfoShownKey(chainId), false)
    }

    override fun isChainMigrationDetailsNeeded(chainId: String): Boolean {
        return chainId in getChainsWithAssetBalance()
    }

    override suspend fun setInfoShownForChain(chainId: String) {
        preferences.putBoolean(getChainInfoShownKey(chainId), true)
    }

    private fun AssetLocal.hasBalance(): Boolean {
        return totalBalance(freeInPlanks, reservedInPlanks).isPositive()
    }

    private fun saveToStorage(chainsWithAssetBalance: Set<String>) {
        val currentChains = getChainsWithAssetBalance()
        setChainsWithAssetBalance(currentChains + chainsWithAssetBalance)
    }

    private fun getChainsWithAssetBalance() = preferences.getStringSet(CHAINS_WITH_ASSET_BALANCE)

    private fun setChainsWithAssetBalance(chains: Set<ChainId>) = preferences.putStringSet(CHAINS_WITH_ASSET_BALANCE, chains)

    private fun getChainInfoShownKey(chainId: String) = CHAIN_MIGRATION_INFO_SHOWN_PREFIX + chainId
}
