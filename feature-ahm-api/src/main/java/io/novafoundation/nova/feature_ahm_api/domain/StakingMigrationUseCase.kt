package io.novafoundation.nova.feature_ahm_api.domain

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import kotlinx.coroutines.flow.Flow

interface StakingMigrationUseCase {

    fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?>

    fun markMigrationInfoAsHidden(chainId: String, assetId: Int)

    fun observeAlertShouldBeHidden(chainId: String, assetId: Int): Flow<Boolean>
}
