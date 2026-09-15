package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.runtime.network.updaters.SampledBlockTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockTimePredictionTest {

    private val constants = 2000.toBigInteger()

    @Test
    fun `without samples the constants value is used`() {
        val predicted = predictBlockTime(SampledBlockTime.initial(), blockTimeFromConstants = constants, configuredBlockTime = null)

        assertEquals(constants, predicted)
    }

    @Test
    fun `few samples are blended with constants`() {
        val sampled = sampled(size = 5, average = 6000)

        val predicted = predictBlockTime(sampled, blockTimeFromConstants = constants, configuredBlockTime = null)

        assertEquals(4000, predicted.toInt())
    }

    @Test
    fun `enough samples fully replace constants when no config is present`() {
        val sampled = sampled(size = 10, average = 6000)

        val predicted = predictBlockTime(sampled, blockTimeFromConstants = constants, configuredBlockTime = null)

        assertEquals(6000, predicted.toInt())
    }

    @Test
    fun `samples far from configured block time are ignored in favour of config`() {
        val sampled = sampled(size = 10, average = 6000)

        val predicted = predictBlockTime(sampled, blockTimeFromConstants = constants, configuredBlockTime = 2000.toBigInteger())

        assertEquals(2000, predicted.toInt())
    }

    @Test
    fun `samples close to configured block time refine it`() {
        val sampled = sampled(size = 10, average = 2200)

        val predicted = predictBlockTime(sampled, blockTimeFromConstants = constants, configuredBlockTime = 2000.toBigInteger())

        assertEquals(2200, predicted.toInt())
    }

    @Test
    fun `samples much faster than configured block time are ignored too`() {
        val sampled = sampled(size = 10, average = 900)

        val predicted = predictBlockTime(sampled, blockTimeFromConstants = constants, configuredBlockTime = 2000.toBigInteger())

        assertEquals(2000, predicted.toInt())
    }

    private fun sampled(size: Int, average: Int) = SampledBlockTime(
        sampleSize = size.toBigInteger(),
        averageBlockTime = average.toBigInteger(),
    )
}
