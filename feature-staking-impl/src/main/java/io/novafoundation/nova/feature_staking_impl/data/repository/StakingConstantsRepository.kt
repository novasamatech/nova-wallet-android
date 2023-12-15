package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger

private const val MAX_NOMINATIONS_FALLBACK = 16
private const val MAX_UNLOCK_CHUNKS_FALLBACK = 32

class StakingConstantsRepository(
    private val chainRegistry: ChainRegistry,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) {

    suspend fun maxUnlockingChunks(chainId: ChainId): BigInteger {
        return getOptionalNumberConstant(chainId, "MaxUnlockingChunks")
            ?: MAX_UNLOCK_CHUNKS_FALLBACK.toBigInteger()
    }

    /**
     * Returns maxRewardedNominatorPerValidator or null in case there is no limitation on rewarded nominators per validator
     */
    suspend fun maxRewardedNominatorPerValidator(chainId: ChainId): Int? {
        return getOptionalNumberConstant(chainId, "MaxNominatorRewardedPerValidator")?.toInt()
    }

    suspend fun lockupPeriodInEras(chainId: ChainId): BigInteger = getNumberConstant(chainId, "BondingDuration")

    suspend fun maxValidatorsPerNominator(chainId: ChainId, stake: Balance): Int {
        return getOptionalNumberConstant(chainId, "MaxNominations")?.toInt()
            ?: getMaxNominationsQuota(chainId, stake)?.toInt()
            ?: MAX_NOMINATIONS_FALLBACK
    }

    private suspend fun getMaxNominationsQuota(chainId: ChainId, stake: Balance): BigInteger? = runCatching {
        val runtimeCallApi = runtimeCallsApi.forChain(chainId)

        runtimeCallApi.call(
            section = "StakingApi",
            method = "nominations_quota",
            arguments = listOf(stake to "Balance"),
            returnType = "u32",
            returnBinding = ::bindNumber
        )
    }.getOrNull()

    private suspend fun getNumberConstant(chainId: ChainId, constantName: String): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.staking().numberConstant(constantName, runtime)
    }

    private suspend fun getOptionalNumberConstant(chainId: ChainId, constantName: String): BigInteger? {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.staking().numberConstantOrNull(constantName, runtime)
    }
}
