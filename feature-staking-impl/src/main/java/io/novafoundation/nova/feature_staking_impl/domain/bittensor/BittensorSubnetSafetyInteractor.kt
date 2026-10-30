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

    /** The exact time since the registration block; only the age label rounds it to days. */
    suspend fun age(subnet: BittensorSubnet): Duration? {
        val registration = subnet.registrationBlock ?: return null
        val header = rpcCalls.getBlockHeader(ChainGeneses.BITTENSOR)
        val latest = BigInteger.valueOf(header.number.toLong())
        val blockTime = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
        return bittensorSubnetAge(registration, latest, blockTime)
    }
}

internal fun bittensorSubnetAge(registration: BigInteger, latest: BigInteger, blockTime: Duration): Duration {
    require(latest >= registration) { "Registration block is ahead of the chain" }
    require(blockTime.isPositive())
    val elapsedMillis = (latest - registration) * blockTime.inWholeMilliseconds.toBigInteger()
    // BigInteger.longValueExact() exists only from API 31, so the overflow check is done by hand.
    if (elapsedMillis.bitLength() >= Long.SIZE_BITS) {
        throw ArithmeticException("Subnet age does not fit a Long: $elapsedMillis ms")
    }
    return elapsedMillis.toLong().milliseconds
}
