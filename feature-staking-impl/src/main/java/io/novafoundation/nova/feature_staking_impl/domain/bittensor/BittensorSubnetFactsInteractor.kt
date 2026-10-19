package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRankingRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class BittensorSubnetFacts(val monthsOld: Int?, val deepPool: Boolean, val steady: Boolean)

private val FACT_MONTH = 30.days
private const val SAFER_AGE_MONTHS = 6
private val DEEP_POOL_TAO = BigDecimal(25000)
private val CALMER_VOLATILITY_SCORE = BigDecimal(50)

class BittensorSubnetFactsInteractor(
    private val backend: BittensorValidatorBackend,
    private val chainStateRepository: ChainStateRepository
) {

    suspend fun facts(subnet: BittensorSubnet): BittensorSubnetFacts {
        val ranking = backend.rankedSubnet(subnet.id, subnet.registrationBlock)
        val blockTime = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
        return mapBittensorSubnetFacts(ranking, blockTime)
    }
}

internal fun mapBittensorSubnetFacts(ranking: BittensorSubnetRankingRemote?, blockTime: Duration): BittensorSubnetFacts {
    val ageMillis = ranking?.ageBlocks?.takeIf { it.isNonNegative && blockTime.isPositive() }
        ?.toBigDecimal()?.times(blockTime.inWholeMilliseconds.toBigDecimal())
    val monthMillis = FACT_MONTH.inWholeMilliseconds.toBigDecimal()
    val months = ageMillis?.takeIf { it > monthMillis * SAFER_AGE_MONTHS.toBigDecimal() }
        ?.divide(monthMillis, 0, RoundingMode.DOWN)
        ?.takeIf { it <= Int.MAX_VALUE.toBigDecimal() }
        ?.toInt()
    val pool = ranking?.taoIn?.toBigDecimalOrNull()
    val volatility = ranking?.breakdown?.volatility
    val rawVolatility = volatility?.raw?.toBigDecimalOrNull()
    val score = volatility?.normalized?.toBigDecimalOrNull()
    return BittensorSubnetFacts(
        monthsOld = months,
        deepPool = pool != null && pool > DEEP_POOL_TAO,
        steady = rawVolatility?.isNonNegative == true && score != null && score.isNonNegative && score < CALMER_VOLATILITY_SCORE
    )
}
