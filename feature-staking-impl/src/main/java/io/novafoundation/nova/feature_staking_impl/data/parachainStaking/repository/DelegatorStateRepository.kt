package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegatorState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow

interface DelegatorStateRepository {

    fun observeDelegatorState(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
    ): Flow<DelegatorState>
}

class RealDelegatorStateRepository(
    private val storage: StorageDataSource,
): DelegatorStateRepository {

    override fun observeDelegatorState(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<DelegatorState> {
       return storage.subscribe(chain.id) {
           runtime.metadata.parachainStaking().storage("DelegatorState").observe(
               accountId,
               binding = { bindDelegatorState(it, accountId, chain) }
           )
       }
    }
}
