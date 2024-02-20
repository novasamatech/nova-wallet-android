package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.display

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayModel
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMetadata
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolDisplay
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

internal class RealPoolDisplayUseCase(
    private val poolDisplayFormatter: PoolDisplayFormatter,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val poolStateRepository: NominationPoolStateRepository,
) : PoolDisplayUseCase {

    override suspend fun getPoolDisplay(poolId: Int, chain: Chain): PoolDisplayModel {
        val poolIdTyped = PoolId(poolId)

        val poolDisplay = SimplePoolDisplay(
            icon = poolStateRepository.getPoolIcon(poolIdTyped, chain.id),
            metadata = poolStateRepository.getAnyPoolMetadata(poolIdTyped, chain.id),
            stashAccountId = poolAccountDerivation.bondedAccountOf(poolIdTyped, chain.id)
        )

        return poolDisplayFormatter.format(poolDisplay, chain)
    }

    private class SimplePoolDisplay(
        override val icon: Icon?,
        override val metadata: PoolMetadata?,
        override val stashAccountId: AccountId
    ) : PoolDisplay
}
