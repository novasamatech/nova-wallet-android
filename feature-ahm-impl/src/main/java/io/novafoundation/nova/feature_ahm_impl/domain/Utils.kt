package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun ChainRegistry.observeMigrationConfigWithChains(config: ChainMigrationConfig): Flow<ChainMigrationConfigWithChains> {
    return chainsById
        .map {
            val sourceChain = it.getValue(config.sourceData.chainId)
            val destinationChain = it.getValue(config.destinationData.chainId)

            ChainMigrationConfigWithChains(
                config = config,
                sourceChain = sourceChain,
                sourceAsset = sourceChain.assetsById.getValue(config.sourceData.assetId),
                destinationChain = destinationChain,
                destinationAsset = destinationChain.assetsById.getValue(config.destinationData.assetId)
            )
        }
}
