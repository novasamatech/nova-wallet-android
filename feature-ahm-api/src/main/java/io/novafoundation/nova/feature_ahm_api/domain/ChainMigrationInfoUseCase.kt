package io.novafoundation.nova.feature_ahm_api.domain

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import kotlinx.coroutines.flow.Flow

interface ChainMigrationInfoUseCase {

    fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?>

    fun markMigrationInfoAsHidden(key: String, chainId: String, assetId: Int)

    fun observeInfoShouldBeHidden(key: String, chainId: String, assetId: Int): Flow<Boolean>
}
