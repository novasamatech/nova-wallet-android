package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface CrossChainTransfersRepository {

    suspend fun paraId(chainId: ChainId): ParaId?

    suspend fun syncConfiguration()

    fun configurationFlow(): Flow<CrossChainTransfersConfiguration>

    suspend fun getConfiguration(): CrossChainTransfersConfiguration
}
