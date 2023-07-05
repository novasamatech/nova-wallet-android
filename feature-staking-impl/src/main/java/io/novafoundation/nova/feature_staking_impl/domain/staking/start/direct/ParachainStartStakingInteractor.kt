package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.PayoutType
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.state.assetWithChain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ParachainStartStakingInteractor(
    stakingSharedState: StakingSharedState,
    stakingSharedComputation: StakingSharedComputation,
    coroutineScope: CoroutineScope
) : DirectStartStakingInteractor(stakingSharedState, stakingSharedComputation, coroutineScope) {

    override fun observeMinStake(): Flow<BigInteger> {
        return flow { BigInteger.ONE }
        /*return stakingSharedState.assetWithChain.flatMapLatest { chainWithAsset ->
            val storageName = "MinimumActiveStake"

            storageDataSource.observeNonNull(
                chainId = chainWithAsset.chain.id,
                keyBuilder = { it.storage(storageName).storageKey() },
            ) { scale, runtime ->
                val returnType = runtime.storage(storageName).returnType()
                bindNumber(returnType.fromHexOrIncompatible(scale, runtime))
            }
        }*/
    }

    override fun observePayoutType(): Flow<PayoutType> {
        return stakingSharedState.assetWithChain
            .map {
                when (it.chain.id) {
                    ChainGeneses.MOONBEAM -> PayoutType.Automatic.Payout
                    else -> PayoutType.Manual
                }
            }
    }

    override fun observeParticipationInGovernance(): Flow<Boolean> {
        return flowOf { false }
    }

    private fun RuntimeSnapshot.storage(storage: String): StorageEntry {
        return metadata.parachainStaking().storage(storage)
    }
}
