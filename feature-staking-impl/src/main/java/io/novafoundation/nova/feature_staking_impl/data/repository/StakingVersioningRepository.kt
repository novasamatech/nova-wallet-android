package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.metadata.call

interface StakingVersioningRepository {

    suspend fun controllersDeprecationStage(chainId: ChainId): ControllersDeprecationStage
}

enum class ControllersDeprecationStage {
    NORMAL, DEPRECATED
}

class RealStakingVersioningRepository(
    private val chainRegistry: ChainRegistry,
) : StakingVersioningRepository {

    override suspend fun controllersDeprecationStage(chainId: ChainId): ControllersDeprecationStage {
        val runtime = chainRegistry.getRuntime(chainId)
        val setControllerFunction = runtime.metadata.staking().call("set_controller")

        return if (setControllerFunction.arguments.isEmpty()) {
            ControllersDeprecationStage.DEPRECATED
        } else {
            ControllersDeprecationStage.NORMAL
        }
    }
}
