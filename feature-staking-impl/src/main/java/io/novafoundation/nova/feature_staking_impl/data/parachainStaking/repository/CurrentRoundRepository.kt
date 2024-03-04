package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.RoundInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindCollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindRoundInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow

interface CurrentRoundRepository {

    fun currentRoundInfoFlow(chainId: ChainId): Flow<RoundInfo>

    suspend fun currentRoundInfo(chainId: ChainId): RoundInfo

    suspend fun collatorsSnapshot(chainId: ChainId, roundIndex: RoundIndex): AccountIdMap<CollatorSnapshot>

    suspend fun collatorSnapshot(chainId: ChainId, collatorId: AccountId, roundIndex: RoundIndex): CollatorSnapshot?

    fun totalStakedFlow(chainId: ChainId): Flow<Balance>

    suspend fun totalStaked(chainId: ChainId): Balance
}

suspend fun CurrentRoundRepository.collatorsSnapshotInCurrentRound(chainId: ChainId): AccountIdMap<CollatorSnapshot> {
    val roundIndex = currentRoundInfo(chainId).current

    return collatorsSnapshot(chainId, roundIndex)
}

suspend fun CurrentRoundRepository.collatorSnapshotInCurrentRound(
    chainId: ChainId,
    collatorId: AccountId,
): CollatorSnapshot? {
    val roundIndex = currentRoundInfo(chainId).current

    return collatorSnapshot(chainId, collatorId, roundIndex)
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

    override suspend fun collatorSnapshot(chainId: ChainId, collatorId: AccountId, roundIndex: RoundIndex): CollatorSnapshot? {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("AtStake").query(
                roundIndex,
                collatorId,
                binding = ::bindCollatorSnapshot
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
