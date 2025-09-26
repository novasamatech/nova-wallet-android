package io.novafoundation.nova.feature_ahm_api.data.repository

import java.math.BigInteger

interface ChainMigrationRepository {

    suspend fun cacheBalancesForChainMigrationDetection()

    suspend fun isNeededToShowInfoForChain(chainId: String, migrationStartBlock: BigInteger): Boolean

    suspend fun setInfoShownForChain(chainId: String)
}
