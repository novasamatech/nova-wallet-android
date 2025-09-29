package io.novafoundation.nova.feature_ahm_api.domain

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import kotlinx.coroutines.flow.Flow

interface AssetMigrationUseCase {

    fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?>

    fun markRelayMigrationInfoAsHidden(chainId: String, assetId: Int)
    fun observeSourceShouldBeHidden(chainId: String, assetId: Int): Flow<Boolean>
}
