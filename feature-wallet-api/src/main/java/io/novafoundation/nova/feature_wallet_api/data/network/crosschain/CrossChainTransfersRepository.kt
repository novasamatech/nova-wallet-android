package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface CrossChainTransfersRepository {

    suspend fun paraId(chaniId: ChainId): ParaId?

    suspend fun syncConfiguration()

    suspend fun getConfiguration(): CrossChainTransfersConfiguration
}
