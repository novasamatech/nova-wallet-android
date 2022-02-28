package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.asNumber
import io.novafoundation.nova.common.utils.constantOrNull
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

private const val MAX_NOMINATIONS_FALLBACK = 16

class StakingConstantsRepository(
    private val chainRegistry: ChainRegistry,
) {

    suspend fun maxRewardedNominatorPerValidator(chainId: ChainId): Int = getNumberConstant(chainId, "MaxNominatorRewardedPerValidator").toInt()

    suspend fun lockupPeriodInEras(chainId: ChainId): BigInteger = getNumberConstant(chainId, "BondingDuration")

    suspend fun maxValidatorsPerNominator(chainId: ChainId): Int {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.staking().constantOrNull("MaxNominations")?.asNumber(runtime)?.toInt()
            ?: MAX_NOMINATIONS_FALLBACK
    }

    private suspend fun getNumberConstant(chainId: ChainId, constantName: String): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.staking().numberConstant(constantName, runtime)
    }
}
