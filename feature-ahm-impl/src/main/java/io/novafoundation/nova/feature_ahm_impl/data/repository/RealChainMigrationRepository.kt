package io.novafoundation.nova.feature_ahm_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.runtime.ext.UTILITY_ASSET_ID
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

private const val CHAINS_WITH_ASSET_BALANCE = "CHAINS_WITH_ASSET_BALANCE"
private const val CHAIN_MIGRATION_INFO_SHOWN_PREFIX = "CHAIN_MIGRATION_INFO_SHOWN_PREFIX_"

class RealChainMigrationRepository(
    private val assetDao: AssetDao,
    private val preferences: Preferences,
    private val chainRegistry: ChainRegistry,
    private val remoteStorageSource: StorageDataSource,
) : ChainMigrationRepository {

    override suspend fun cacheBalancesForChainMigrationDetection() {
        val chainsWithAssetBalance = assetDao.getAllAssets()
            .filter { it.assetId == UTILITY_ASSET_ID && it.hasBalance() }
            .mapToSet { it.chainId }

        saveToStorage(chainsWithAssetBalance)
    }

    override suspend fun isNeededToShowInfoForChain(chainId: String, migrationStartBlock: BigInteger): Boolean {
        val isChainMigrationInfoWasShown = preferences.getBoolean(getChainInfoShownKey(chainId), false)
        if (isChainMigrationInfoWasShown) return false

        val chainsWithAssetBalance = getChainsWithAssetBalance()
        if (chainId !in chainsWithAssetBalance) return false

        val lastBlockNumber = getLastBlockNumber(chainId)
        if (lastBlockNumber < migrationStartBlock) return false

        return true
    }

    override suspend fun setInfoShownForChain(chainId: String) {
        preferences.putBoolean(getChainInfoShownKey(chainId), true)
    }

    private fun AssetLocal.hasBalance(): Boolean {
        return freeInPlanks.isPositive()
    }

    private suspend fun getLastBlockNumber(chainId: String): BigInteger {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.system().storage("Number").query {
                it.cast<BigInteger>()
            }
        }
    }

    private fun saveToStorage(chainsWithAssetBalance: Set<String>) {
        val currentChains = getChainsWithAssetBalance()
        setChainsWithAssetBalance(currentChains + chainsWithAssetBalance)
    }

    private fun getChainsWithAssetBalance() = preferences.getStringSet(CHAINS_WITH_ASSET_BALANCE)

    private fun setChainsWithAssetBalance(chains: Set<ChainId>) = preferences.putStringSet(CHAINS_WITH_ASSET_BALANCE, chains)

    private fun getChainInfoShownKey(chainId: String) = CHAIN_MIGRATION_INFO_SHOWN_PREFIX + chainId
}
