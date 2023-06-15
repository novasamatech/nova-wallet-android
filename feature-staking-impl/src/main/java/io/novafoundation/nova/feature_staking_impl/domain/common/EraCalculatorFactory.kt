package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
class EraTimeCalculator(
    private val startTimeStamp: BigInteger,
    private val sessionLength: BigInteger, // Number of blocks per session
    private val eraLength: BigInteger, // Number of sessions per era
    private val blockCreationTime: BigInteger, // How long it takes to create a block
    private val currentSessionIndex: BigInteger,
    private val currentSlot: BigInteger,
    private val genesisSlot: BigInteger,
    private val eraStartSessionIndex: BigInteger,
    private val activeEra: EraIndex,
) {

    fun calculate(destinationEra: EraIndex? = null): BigInteger {
        val sessionStartSlot = currentSessionIndex * sessionLength + genesisSlot
        val sessionProgress = currentSlot - sessionStartSlot
        val eraProgress = (currentSessionIndex - eraStartSessionIndex) * sessionLength + sessionProgress
        val eraRemained = eraLength * sessionLength - eraProgress

        val finishTimeStamp = System.currentTimeMillis().toBigInteger()
        // Doing math takes very long time. By finishing all requests and calculations the time will be outdated for ~5 seconds
        val deltaTime = finishTimeStamp - startTimeStamp

        return if (destinationEra != null) {
            val leftEras = destinationEra - activeEra - 1.toBigInteger()
            val timeForLeftEras = leftEras * eraLength * sessionLength * blockCreationTime

            eraRemained * blockCreationTime + timeForLeftEras - deltaTime
        } else {
            eraRemained * blockCreationTime - deltaTime
        }
    }

    fun eraDuration(): Duration {
        val inMillis = (blockCreationTime * eraLength * sessionLength).toLong()

        return inMillis.milliseconds
    }

    fun calculateTillEraSet(destinationEra: EraIndex): BigInteger {
        val sessionDuration = sessionLength * blockCreationTime
        val tillEraStart = calculate(destinationEra)
        return tillEraStart - sessionDuration
    }
}

class EraTimeCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val sessionRepository: SessionRepository,
    private val chainStateRepository: ChainStateRepository,
    private val electionsSessionRegistry: ElectionsSessionRegistry,
) {

    suspend fun create(stakingOption: StakingOption): EraTimeCalculator {
        val chainId = stakingOption.assetWithChain.asset.chainId
        val electionsSession = electionsSessionRegistry.electionsSessionFor(stakingOption)

        val activeEra = stakingRepository.getActiveEraIndex(chainId)

        return EraTimeCalculator(
            startTimeStamp = System.currentTimeMillis().toBigInteger(),
            eraLength = stakingRepository.eraLength(chainId),
            blockCreationTime = chainStateRepository.predictedBlockTime(chainId),
            currentSessionIndex = sessionRepository.currentSessionIndex(chainId),
            sessionLength = electionsSession.sessionLength(chainId),
            currentSlot = electionsSession.currentSlot(chainId),
            genesisSlot = electionsSession.genesisSlot(chainId),
            eraStartSessionIndex = stakingRepository.eraStartSessionIndex(chainId, activeEra),
            activeEra = activeEra
        )
    }
}
