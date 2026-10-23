package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.time.Duration.Companion.days

class BittensorSubnetSafetyInteractor(
    private val rpcCalls: RpcCalls,
    private val chainStateRepository: ChainStateRepository
) {

    suspend fun ageInDays(subnet: BittensorSubnet): BigDecimal? {
        val registration = subnet.registrationBlock ?: return null
        val header = rpcCalls.getBlockHeader(ChainGeneses.BITTENSOR)
        val latest = BigInteger.valueOf(header.number.toLong())
        require(latest >= registration) { "Registration block is ahead of the chain" }
        val blockTime = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
        require(blockTime.isPositive())
        val elapsedMillis = (latest - registration).toBigDecimal() * blockTime.inWholeMilliseconds.toBigDecimal()
        // Keeps the fraction of a day: young subnets are shown in hours/minutes and the chart is clipped to the hour
        return elapsedMillis.divide(1.days.inWholeMilliseconds.toBigDecimal(), AGE_DAYS_SCALE, RoundingMode.DOWN)
    }
}

private const val AGE_DAYS_SCALE = 6
