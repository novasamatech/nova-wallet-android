package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.RoundInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindCollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindRoundInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow

interface CurrentRoundRepository {

    fun currentRoundInfoFlow(chainId: ChainId): Flow<RoundInfo>

    suspend fun currentRoundInfo(chainId: ChainId): RoundInfo

    suspend fun collatorsSnapshot(chainId: ChainId, roundIndex: RoundIndex): AccountIdMap<CollatorSnapshot>

    fun totalStakedFlow(chainId: ChainId): Flow<Balance>

    suspend fun totalStaked(chainId: ChainId): Balance
}

class RealCurrentRoundRepository(
    private val storageDataSource: StorageDataSource,
) : CurrentRoundRepository {

    override fun currentRoundInfoFlow(chainId: ChainId): Flow<RoundInfo> {
        return storageDataSource.subscribe(chainId) {
            runtime.metadata.parachainStaking().storage("Round").observe(binding = ::bindRoundInfo)
        }
    }

    override suspend fun currentRoundInfo(chainId: ChainId): RoundInfo {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("Round").query(binding = ::bindRoundInfo)
        }
    }

    override suspend fun collatorsSnapshot(chainId: ChainId, roundIndex: RoundIndex): AccountIdMap<CollatorSnapshot> {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("AtStake").entries(
                roundIndex,
                keyExtractor = { (_: RoundIndex, collatorId: AccountId) -> collatorId.toHexString() },
                binding = { instance, _ -> bindCollatorSnapshot(instance) }
            )
        }
    }

    override fun totalStakedFlow(chainId: ChainId): Flow<Balance> {
        return storageDataSource.subscribe(chainId) {
            runtime.metadata.parachainStaking().storage("Total").observe(binding = ::bindNumber)
        }
    }

    override suspend fun totalStaked(chainId: ChainId): Balance {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("Total").query(binding = ::bindNumber)
        }
    }
}
