package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.parachainInfoOrNull
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapCrossChainConfigFromRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainConfigApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealCrossChainTransfersRepository(
    private val api: CrossChainConfigApi,
    private val remoteStorageSource: StorageDataSource
): CrossChainTransfersRepository {

    override suspend fun paraId(chaniId: ChainId): ParaId? {
        return remoteStorageSource.query(chaniId) {
            runtime.metadata.parachainInfoOrNull()?.storage("ParachainId")?.query(binding = ::bindNumber)
        }
    }

    override suspend fun getConfiguration(): CrossChainTransfersConfiguration = withContext(Dispatchers.Default) {
       val remote = retryUntilDone { api.getCrossChainConfig() }

        mapCrossChainConfigFromRemote(remote)
    }
}
