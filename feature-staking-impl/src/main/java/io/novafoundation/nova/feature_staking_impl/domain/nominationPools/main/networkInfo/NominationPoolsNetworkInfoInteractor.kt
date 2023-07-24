package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation.PoolAccountType
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface NominationPoolsNetworkInfoInteractor {

    fun observeShouldShowNetworkInfo(): Flow<Boolean>

    fun observeNetworkInfo(chainId: ChainId, sharedComputationScope: CoroutineScope): Flow<NetworkInfo>
}

class RealNominationPoolsNetworkInfoInteractor(
    private val relaychainStakingSharedComputation: StakingSharedComputation,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val relaychainStakingInteractor: StakingInteractor,
    private val nominationPoolMemberUseCase: NominationPoolMemberUseCase,
) : NominationPoolsNetworkInfoInteractor {

    override fun observeShouldShowNetworkInfo(): Flow<Boolean> {
        return nominationPoolMemberUseCase.currentPoolMemberFlow().map { it == null }
    }

    override fun observeNetworkInfo(
        chainId: ChainId,
        sharedComputationScope: CoroutineScope
    ): Flow<NetworkInfo> {
        return combine(
            relaychainStakingSharedComputation.electedExposuresWithActiveEraFlow(chainId, sharedComputationScope),
            nominationPoolGlobalsRepository.observeMinJoinBond(chainId),
            nominationPoolGlobalsRepository.lastPoolId(chainId),
            lockupDurationFlow()
        ) { (exposures), minJoinBond, lastPoolId, lockupDuration ->
            NetworkInfo(
                lockupPeriod = lockupDuration,
                minimumStake = minJoinBond,
                totalStake = calculateTotalStake(exposures, lastPoolId, chainId),
                stakingPeriod = StakingPeriod.Unlimited,
                nominatorsCount = null
            )
        }
    }

    private fun lockupDurationFlow() = flowOf { relaychainStakingInteractor.getLockupDuration() }

    private suspend fun calculateTotalStake(
        exposures: AccountIdMap<Exposure>,
        lastPoolId: PoolId,
        chainId: ChainId,
    ): Balance {
        val numberOfPools = lastPoolId.value.toInt()
        val allPoolAccountIds = poolAccountDerivation.derivePoolAccountsRange(numberOfPools, PoolAccountType.BONDED, chainId)
            .mapToSet { it.intoKey() }

        return exposures.values.sumOf { exposure ->
            exposure.others.sumOf { nominatorExposure ->
                if (nominatorExposure.who.intoKey() in allPoolAccountIds) {
                    nominatorExposure.value
                } else {
                    BigInteger.ZERO
                }
            }
        }
    }
}
