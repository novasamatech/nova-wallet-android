package io.novafoundation.nova.feature_staking_impl.domain.common

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class EraTimeCalculator(
    private val startTimeStamp: BigInteger,
    private val sessionLength: BigInteger, // Number of blocks per session
    private val eraLength: BigInteger, // Number of sessions per era
    private val blockCreationTime: BigInteger, // How long it takes to create a block
    private val currentSessionIndex: BigInteger,
    private val currentEpochIndex: BigInteger,
    private val currentSlot: BigInteger,
    private val genesisSlot: BigInteger,
    private val eraStartSessionIndex: BigInteger,
    val activeEra: EraIndex,
) {

    fun calculate(destinationEra: EraIndex? = null): BigInteger {
        val eraRemained = remainingEraBlocks()

        // EraTimeCalculator was initialized based on values known at startTimeStamp.
        // We need to adjust timers in case this instance is used for a long period of time
        val finishTimeStamp = System.currentTimeMillis().toBigInteger()
        val deltaTime = finishTimeStamp - startTimeStamp

        return if (destinationEra != null) {
            val leftEras = destinationEra - activeEra - 1.toBigInteger()
            val timeForLeftEras = leftEras * eraLength * sessionLength * blockCreationTime

            eraRemained * blockCreationTime + timeForLeftEras - deltaTime
        } else {
            eraRemained * blockCreationTime - deltaTime
        }
    }

    /**
     * Duration till the end of current active era
     */
    fun remainingEraDuration(): Duration {
        return (remainingEraBlocks() * blockCreationTime).toDuration()
    }

    fun eraDuration(): Duration {
        return (blockCreationTime * eraLength * sessionLength).toDuration()
    }

    fun calculateTillEraSet(destinationEra: EraIndex): BigInteger {
        val sessionDuration = sessionLength * blockCreationTime
        val tillEraStart = calculate(destinationEra)
        return tillEraStart - sessionDuration
    }

    /**
     * Returns a number that can be used to compare different instances of [EraTimeCalculator]
     * to determine how much their calculations would deffer between each other
     * This wont correspond to real timestamp and shouldn't be used as such
     */
    fun derivedTimestamp(): Duration {
        val derivedProgressInBlocks = activeEra * eraLength * sessionLength + eraProgress()

        return (derivedProgressInBlocks * blockCreationTime).toDuration()
    }

    private fun eraProgress(): BlockNumber {
        val epochStartSlot = currentEpochIndex * sessionLength + genesisSlot
        val sessionProgress = currentSlot - epochStartSlot

        return (currentSessionIndex - eraStartSessionIndex) * sessionLength + sessionProgress
    }

    private fun remainingEraBlocks(): BlockNumber {
        return eraLength * sessionLength - eraProgress()
    }

    private fun BigInteger.toDuration() = toLong().milliseconds
}

fun EraTimeCalculator.erasDuration(numberOfEras: BigInteger): Duration {
    return eraDuration() * numberOfEras.toInt()
}

fun EraTimeCalculator.calculateDurationTill(era: EraIndex): Duration {
    return calculate(era).toLong().milliseconds
}

private val ERA_DURATION_DIFFERENCE_THRESHOLD = 10.minutes

class EraTimeCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val sessionRepository: SessionRepository,
    private val chainStateRepository: ChainStateRepository,
    private val electionsSessionRegistry: ElectionsSessionRegistry,
) {

    suspend fun create(
        stakingOption: StakingOption,
        activeEraFlow: Flow<EraIndex>
    ): Flow<EraTimeCalculator> {
        val chainId = stakingOption.assetWithChain.asset.chainId
        val electionsSession = electionsSessionRegistry.electionsSessionFor(stakingOption)

        val genesisSlot = electionsSession.genesisSlot(chainId)

        val sessionLength = electionsSession.sessionLength(chainId)

        return combine(
            activeEraFlow,
            sessionRepository.observeCurrentSessionIndex(chainId),
            electionsSession.currentEpochIndexFlow(chainId),
            electionsSession.currentSlotFlow(chainId),
        ) { activeEra, currentSessionIndex, currentEpochIndex, currentSlot ->
            val eraStartSessionIndex = stakingRepository.eraStartSessionIndex(chainId, activeEra)

            EraTimeCalculator(
                startTimeStamp = System.currentTimeMillis().toBigInteger(),
                eraLength = stakingRepository.eraLength(chainId),
                blockCreationTime = chainStateRepository.predictedBlockTime(chainId),
                currentSessionIndex = currentSessionIndex,
                currentEpochIndex = currentEpochIndex ?: currentSessionIndex,
                sessionLength = sessionLength,
                currentSlot = currentSlot,
                genesisSlot = genesisSlot,
                eraStartSessionIndex = eraStartSessionIndex,
                activeEra = activeEra
            )
        }
            .distinctUntilChanged { old, new -> new.canBeIgnoredAfter(old) }
    }

    private fun EraTimeCalculator.canBeIgnoredAfter(previous: EraTimeCalculator): Boolean {
        val previousTimestamp = previous.derivedTimestamp()
        val newTimestamp = derivedTimestamp()

        val difference = (newTimestamp - previousTimestamp).absoluteValue
        val canIgnore = difference < ERA_DURATION_DIFFERENCE_THRESHOLD

        Log.d("EraTimeCalculatorFactory", "New update for RewardCalculator, difference with lastly used is ${difference}, can ignore: $canIgnore")

        return canIgnore
    }
}
