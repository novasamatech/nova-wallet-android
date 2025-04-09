package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import kotlinx.coroutines.flow.Flow

interface CrossChainTransfersRepository {

    suspend fun syncConfiguration()

    fun configurationFlow(): Flow<CrossChainTransfersConfiguration>

    suspend fun getConfiguration(): CrossChainTransfersConfiguration
}
