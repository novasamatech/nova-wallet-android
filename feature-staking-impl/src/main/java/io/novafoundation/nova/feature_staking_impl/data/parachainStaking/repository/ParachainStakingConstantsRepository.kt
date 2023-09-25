package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

interface ParachainStakingConstantsRepository {

    suspend fun maxRewardedDelegatorsPerCollator(chainId: ChainId): BigInteger

    suspend fun minimumDelegation(chainId: ChainId): BigInteger

    suspend fun minimumDelegatorStake(chainId: ChainId): BigInteger

    suspend fun delegationBondLessDelay(chainId: ChainId): BigInteger

    suspend fun maxDelegationsPerDelegator(chainId: ChainId): BigInteger
}

suspend fun ParachainStakingConstantsRepository.systemForcedMinStake(chainId: ChainId): BigInteger {
    return minimumDelegatorStake(chainId).max(minimumDelegation(chainId))
}

class RuntimeParachainStakingConstantsRepository(
    private val chainRegistry: ChainRegistry
) : ParachainStakingConstantsRepository {

    override suspend fun maxRewardedDelegatorsPerCollator(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MaxTopDelegationsPerCandidate")
    }

    override suspend fun minimumDelegation(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MinDelegation")
    }

    override suspend fun minimumDelegatorStake(chainId: ChainId): BigInteger {
        return numberConstantOrNull(chainId, "MinDelegatorStk")
            // Starting from runtime 2500, MinDelegatorStk was removed and only MinDelegation remained
            ?: minimumDelegation(chainId)
    }

    override suspend fun delegationBondLessDelay(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "DelegationBondLessDelay")
    }

    override suspend fun maxDelegationsPerDelegator(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MaxDelegationsPerDelegator")
    }

    private suspend fun numberConstant(chainId: ChainId, name: String): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.parachainStaking().numberConstant(name, runtime)
    }

    private suspend fun numberConstantOrNull(chainId: ChainId, name: String): BigInteger? {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.parachainStaking().numberConstantOrNull(name, runtime)
    }
}
