package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMetadata
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.NominationPoolYourPoolInteractor.YourPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolDisplay
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NominationPoolYourPoolInteractor {

    class YourPool(
        val id: PoolId,
        override val icon: Icon?,
        override val metadata: PoolMetadata?,
        override val stashAccountId: AccountId,
    ) : PoolDisplay

    fun yourPoolFlow(poolId: PoolId, chainId: ChainId): Flow<YourPool>
}

class RealNominationPoolYourPoolInteractor(
    private val poolAccountDerivation: PoolAccountDerivation,
    private val poolStateRepository: NominationPoolStateRepository,
) : NominationPoolYourPoolInteractor {
    override fun yourPoolFlow(poolId: PoolId, chainId: ChainId): Flow<YourPool> {
        return flowOfAll {
            val stashAccountId = poolAccountDerivation.bondedAccountOf(poolId, chainId)
            val icon = poolStateRepository.getPoolIcon(poolId, chainId)

            poolStateRepository.observePoolMetadata(poolId, chainId).map { poolMetadata ->
                YourPool(
                    id = poolId,
                    icon = icon,
                    metadata = poolMetadata,
                    stashAccountId = stashAccountId
                )
            }
        }
    }
}
