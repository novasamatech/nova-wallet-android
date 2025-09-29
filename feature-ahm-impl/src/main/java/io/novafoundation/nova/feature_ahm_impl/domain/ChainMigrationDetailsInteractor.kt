package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainFlow
import kotlinx.coroutines.flow.Flow

class ChainMigrationDetailsInteractor(
    private val chainRegistry: ChainRegistry,
    private val chainMigrationRepository: ChainMigrationRepository,
    private val migrationInfoRepository: MigrationInfoRepository
) {

    fun chainFlow(chainId: String): Flow<Chain> {
        return chainRegistry.chainFlow(chainId)
    }

    suspend fun getChain(chainId: String): Chain {
        return chainRegistry.getChain(chainId)
    }

    suspend fun getChainMigrationConfig(chainId: String): ChainMigrationConfig? {
        return migrationInfoRepository.getConfigBySource(chainId)
    }

    suspend fun markMigrationInfoAlreadyShown(chainId: String) {
        chainMigrationRepository.setInfoShownForChain(chainId)
    }
}
