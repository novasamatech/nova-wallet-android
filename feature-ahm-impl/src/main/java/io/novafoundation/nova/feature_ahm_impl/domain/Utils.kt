package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.runtime.repository.ChainStateRepository


suspend fun ChainStateRepository.isMigrationBlockPassed(config: ChainMigrationConfig): Boolean {
    return currentRemoteBlock(config.originData.chainId) > config.blockNumberStartAt
}

suspend fun ChainStateRepository.isMigrationBlockNotPassed(config: ChainMigrationConfig): Boolean {
    return !isMigrationBlockPassed(config)
}
