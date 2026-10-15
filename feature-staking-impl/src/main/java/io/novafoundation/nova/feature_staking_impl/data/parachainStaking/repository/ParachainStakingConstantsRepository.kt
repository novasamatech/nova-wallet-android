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
        // EWX (AvN) renames to MaxTopNominationsPerCandidate.
        return numberConstantOrNull(chainId, "MaxTopNominationsPerCandidate")
            ?: numberConstant(chainId, "MaxTopDelegationsPerCandidate")
    }

    override suspend fun minimumDelegation(chainId: ChainId): BigInteger {
        // EWX (AvN) renames to MinNominationPerCollator.
        return numberConstantOrNull(chainId, "MinNominationPerCollator")
            ?: numberConstant(chainId, "MinDelegation")
    }

    override suspend fun minimumDelegatorStake(chainId: ChainId): BigInteger {
        return numberConstantOrNull(chainId, "MinNominationPerCollator")
            ?: numberConstantOrNull(chainId, "MinDelegatorStk")
            // Starting from runtime 2500, MinDelegatorStk was removed and only MinDelegation remained
            ?: minimumDelegation(chainId)
    }

    override suspend fun delegationBondLessDelay(chainId: ChainId): BigInteger {
        // EWX (AvN) has no equivalent constant — the unbond delay is the
        // `Delay` storage value (~2 eras). The reward calculator uses this
        // delay for display only, so fall back to 2 if the constant is
        // absent. A precise on-chain read would require storage access
        // outside this repository's surface.
        return numberConstantOrNull(chainId, "DelegationBondLessDelay") ?: BigInteger.TWO
    }

    override suspend fun maxDelegationsPerDelegator(chainId: ChainId): BigInteger {
        // EWX (AvN) renames to MaxNominationsPerNominator.
        return numberConstantOrNull(chainId, "MaxNominationsPerNominator")
            ?: numberConstant(chainId, "MaxDelegationsPerDelegator")
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
