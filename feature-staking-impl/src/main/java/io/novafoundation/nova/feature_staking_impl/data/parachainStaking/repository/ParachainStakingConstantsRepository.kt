package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

interface ParachainStakingConstantsRepository {

    suspend fun maxRewardedDelegatorsPerCollator(chainId: ChainId): BigInteger

    suspend fun maxTotalDelegatorsPerCollator(chainId: ChainId): BigInteger

    suspend fun minimumDelegation(chainId: ChainId): BigInteger

    suspend fun minimumDelegatorStake(chainId: ChainId): BigInteger

    suspend fun delegationBondLessDelay(chainId: ChainId): BigInteger

    suspend fun defaultBlocksPerRound(chainId: ChainId): BigInteger

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

    override suspend fun maxTotalDelegatorsPerCollator(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MaxBottomDelegationsPerCandidate") + maxRewardedDelegatorsPerCollator(chainId)
    }

    override suspend fun minimumDelegation(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MinDelegatorStk")
    }

    override suspend fun minimumDelegatorStake(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MinDelegatorStk")
    }

    override suspend fun delegationBondLessDelay(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "DelegationBondLessDelay")
    }

    override suspend fun defaultBlocksPerRound(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "DefaultBlocksPerRound")
    }

    override suspend fun maxDelegationsPerDelegator(chainId: ChainId): BigInteger {
        return numberConstant(chainId, "MaxDelegationsPerDelegator")
    }

    private suspend fun numberConstant(chainId: ChainId, name: String): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.parachainStaking().numberConstant(name, runtime)
    }
}
