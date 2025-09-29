package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.AssetMigrationUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private const val INFO_SHOWN_PREFIX = "migration_info_shown_"

class RealAssetMigrationUseCase(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val toggleFeatureRepository: ToggleFeatureRepository,
    private val chainRegistry: ChainRegistry
) : AssetMigrationUseCase {

    override fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?> = flowOfAll {
        val config = migrationInfoRepository.getConfigBySource(chainId)
            ?: migrationInfoRepository.getConfigByDestination(chainId)
            ?: return@flowOfAll emptyFlow()

        chainRegistry.chainsById
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

    override fun observeSourceShouldBeHidden(chainId: String, assetId: Int): Flow<Boolean> {
        return toggleFeatureRepository.observe(getKeyFor(chainId, assetId))
    }

    override fun markRelayMigrationInfoAsHidden(chainId: String, assetId: Int) {
        toggleFeatureRepository.set(getKeyFor(chainId, assetId), true)
    }

    private fun getKeyFor(chainId: String, assetId: Int): String {
        return "$INFO_SHOWN_PREFIX-$chainId-$assetId"
    }
}
