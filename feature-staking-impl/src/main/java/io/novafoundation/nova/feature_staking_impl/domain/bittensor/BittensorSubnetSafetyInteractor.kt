package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BittensorSubnetSafetyInteractor(
    private val rpcCalls: RpcCalls,
    private val chainStateRepository: ChainStateRepository
) {

    /** The exact time since the registration block, as on iOS; only the age label rounds it to days. */
    suspend fun age(subnet: BittensorSubnet): Duration? {
        val registration = subnet.registrationBlock ?: return null
        val header = rpcCalls.getBlockHeader(ChainGeneses.BITTENSOR)
        val latest = BigInteger.valueOf(header.number.toLong())
        require(latest >= registration) { "Registration block is ahead of the chain" }
        val blockTime = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
        require(blockTime.isPositive())
        val elapsedMillis = (latest - registration) * blockTime.inWholeMilliseconds.toBigInteger()
        return elapsedMillis.longValueExact().milliseconds
    }
}
