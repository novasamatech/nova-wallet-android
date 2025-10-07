package io.novafoundation.nova.feature_ahm_api.data.repository

interface ChainMigrationRepository {

    suspend fun cacheBalancesForChainMigrationDetection()

    suspend fun setInfoShownForChain(chainId: String)

    fun isMigrationDetailsWasShown(chainId: String): Boolean

    fun isChainMigrationDetailsNeeded(chainId: String): Boolean
}
