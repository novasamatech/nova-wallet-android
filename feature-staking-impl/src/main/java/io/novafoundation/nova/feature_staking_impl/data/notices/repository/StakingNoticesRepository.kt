package io.novafoundation.nova.feature_staking_impl.data.notices.repository

import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface StakingNoticesRepository {

    fun observeStakingNotices(): Flow<Map<ChainId, StakingNotice>>

    suspend fun syncStakingNotices()
}
