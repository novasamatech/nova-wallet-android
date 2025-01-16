package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.unwrapNominationPools
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.electedExposuresInActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculator
import kotlinx.coroutines.CoroutineScope

class NominationPoolRewardCalculatorFactory(
    private val sharedStakingSharedComputation: StakingSharedComputation,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
) {

    suspend fun create(stakingOption: StakingOption, sharedComputationScope: CoroutineScope): NominationPoolRewardCalculator {
        val chainId = stakingOption.chain.id

        val delegateOption = stakingOption.unwrapNominationPools()

        val delegate = sharedStakingSharedComputation.rewardCalculator(delegateOption, sharedComputationScope)
        val allPoolAccounts = nominationPoolSharedComputation.allBondedPoolAccounts(chainId, sharedComputationScope)
        val poolCommissions = nominationPoolSharedComputation.allBondedPools(chainId, sharedComputationScope)
            .mapValues { (_, pool) -> pool.commission?.current?.perbill }

        return RealNominationPoolRewardCalculator(
            directStakingDelegate = delegate,
            exposures = sharedStakingSharedComputation.electedExposuresInActiveEra(stakingOption.assetWithChain.chain.id, sharedComputationScope),
            commissions = poolCommissions,
            poolStashesById = allPoolAccounts
        )
    }
}

private class RealNominationPoolRewardCalculator(
    private val directStakingDelegate: RewardCalculator,
    private val exposures: AccountIdMap<Exposure>,
    private val commissions: Map<PoolId, Perbill?>,
    poolStashesById: Map<PoolId, AccountIdKey>,
) : NominationPoolRewardCalculator {

    private val poolIdsByStashes: AccountIdKeyMap<PoolId> = poolStashesById.reversed()

    private val apyByPoolStash: Map<PoolId, Fraction> = constructPoolsApy()

    override val maxAPY: Fraction = apyByPoolStash
        .values
        .maxOrNull()
        .orZero()

    override fun apyFor(poolId: PoolId): Fraction? {
        return apyByPoolStash[poolId]
    }

    private fun constructPoolsApy(): Map<PoolId, Fraction> {
        val activeValidatorsByPoolStash = exposures.findPoolsValidators(poolIdsByStashes.keys)

        return activeValidatorsByPoolStash.mapValuesNotNull { (poolStash, nominators) ->
            calculatePoolApy(poolStash, nominators)
        }
    }

    private fun calculatePoolApy(poolId: PoolId, poolValidatorsIdsHex: List<String>): Fraction? {
        if (poolValidatorsIdsHex.isEmpty()) return null

        val commission = commissions[poolId]?.value.orZero()

        val maxApyAcrossValidators = poolValidatorsIdsHex.maxOf { validatorIdHex ->
            directStakingDelegate.getApyFor(validatorIdHex).toDouble()
        }

        val apy = maxApyAcrossValidators * (1.0 - commission)

        return apy.fractions
    }

    private fun AccountIdMap<Exposure>.findPoolsValidators(poolStashes: Set<AccountIdKey>): Map<PoolId, List<String>> {
        val activeValidatorsByPoolStash = mutableMapOf<PoolId, MutableList<String>>()

        forEach { (validatorIdHex, exposure) ->
            exposure.others.forEach { nominator ->
                val nominatorKey = nominator.who.intoKey()

                if (nominatorKey in poolStashes) {
                    poolIdsByStashes[nominatorKey]?.let { poolId ->
                        activeValidatorsByPoolStash.addListItem(poolId, validatorIdHex)
                    }
                }
            }
        }

        return activeValidatorsByPoolStash
    }

    private fun <K, V> MutableMap<K, MutableList<V>>.addListItem(key: K, item: V) {
        val items = getOrPut(key) { mutableListOf() }
        items.add(item)
    }
}
