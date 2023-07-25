package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.scope

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PoolScope(
    private val poolMemberUseCase: NominationPoolMemberUseCase,
) : UpdateScope<PoolId?> {

    override fun invalidationFlow(): Flow<PoolId?> {
        return poolMemberUseCase.currentPoolMemberFlow()
            .map { it?.poolId }
            .distinctUntilChanged()
    }
}
