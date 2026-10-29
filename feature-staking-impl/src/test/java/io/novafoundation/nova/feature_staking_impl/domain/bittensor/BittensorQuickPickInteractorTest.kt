package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetTradingAvailabilityRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorQuickPickInteractorTest {

    private val repository = mock(BittensorSubnetRepository::class.java)

    private val availability = mock(BittensorSubnetTradingAvailabilityRepository::class.java)

    private val swaps = mock(BittensorSwapSource::class.java)

    private val random = Random(1)

    private val selector = BittensorQuickPickSelector(random)

    private val recommendedYieldInteractor = mock(BittensorRecommendedYieldInteractor::class.java)

    private val interactor = BittensorQuickPickInteractor(repository, availability, swaps, selector, recommendedYieldInteractor)

    private val subnetId = BittensorNetuid.Subnet(1)

    private val subnet = BittensorSubnet(subnetId, "Subnet", "α", logoUrl = null, registrationBlock = BigInteger.TEN)

    private val reference = BittensorSubnetReference(subnetId, BigInteger.TEN)

    @Test
    fun `selection exposes loading and retry keeps the selected subnet`() = runTest {
        val eligible = subnet.copy(taoReserveInPlanks = BigInteger.TEN.pow(15))
        `when`(repository.quickPickSubnets()).thenReturn(listOf(eligible))
        `when`(availability.isEnabled(subnetId)).thenReturn(false, true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.ONE)
        val retries = MutableStateFlow(0)
        val emitted = mutableListOf<ExtendedLoadingState<BittensorSubnet>>()
        val job = backgroundScope.launch { interactor.observeSelection(BittensorQuickPickOrder.POOL_SIZE, retries).collect(emitted::add) }

        runCurrent()
        assertTrue(emitted.first() is ExtendedLoadingState.Loading)
        assertTrue(emitted.last() is ExtendedLoadingState.Error)
        retries.value += 1
        runCurrent()
        assertEquals(subnetId, emitted.last().dataOrNull?.id)
        assertEquals(2, emitted.count { it is ExtendedLoadingState.Loading })
        job.cancel()
    }

    @Test
    fun `retry preserves the selected registration until a new visit`() = runTest {
        val eligible = subnet.copy(taoReserveInPlanks = BigInteger.TEN.pow(15))
        val registeredAgain = eligible.copy(registrationBlock = BigInteger.valueOf(11))
        `when`(repository.quickPickSubnets()).thenReturn(listOf(eligible), listOf(registeredAgain), listOf(registeredAgain))
        `when`(availability.isEnabled(subnetId)).thenReturn(false, true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.ONE)
        val retries = MutableStateFlow(0)
        val emitted = mutableListOf<ExtendedLoadingState<BittensorSubnet>>()
        val job = backgroundScope.launch { interactor.observeSelection(BittensorQuickPickOrder.POOL_SIZE, retries).collect(emitted::add) }
        runCurrent()
        retries.value += 1
        runCurrent()
        assertTrue(emitted.last() is ExtendedLoadingState.Error)
        retries.value += 1
        runCurrent()
        assertTrue(emitted.last() is ExtendedLoadingState.Error)
        job.cancel()

        val nextVisit = mutableListOf<ExtendedLoadingState<BittensorSubnet>>()
        val nextRetries = MutableStateFlow(0)
        val nextJob = backgroundScope.launch {
            interactor.observeSelection(BittensorQuickPickOrder.POOL_SIZE, nextRetries).collect(nextVisit::add)
        }
        runCurrent()
        assertEquals(registeredAgain.registrationBlock, nextVisit.last().dataOrNull?.registrationBlock)
        nextJob.cancel()
    }

    @Test
    fun `retry does not replace a removed subnet with another candidate`() = runTest {
        val eligible = subnet.copy(taoReserveInPlanks = BigInteger.TEN.pow(15))
        val replacementId = BittensorNetuid.Subnet(2)
        val replacement = eligible.copy(id = replacementId)
        `when`(repository.quickPickSubnets()).thenReturn(listOf(eligible), listOf(replacement), listOf(replacement))
        `when`(availability.isEnabled(subnetId)).thenReturn(false)
        `when`(availability.isEnabled(replacementId)).thenReturn(true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, replacementId)).thenReturn(BigInteger.ONE)
        val retries = MutableStateFlow(0)
        val emitted = mutableListOf<ExtendedLoadingState<BittensorSubnet>>()
        val job = backgroundScope.launch { interactor.observeSelection(BittensorQuickPickOrder.POOL_SIZE, retries).collect(emitted::add) }
        runCurrent()

        repeat(2) {
            retries.value += 1
            runCurrent()
            assertTrue(emitted.last() is ExtendedLoadingState.Error)
        }

        assertTrue(emitted.none { it is ExtendedLoadingState.Loaded })
        job.cancel()
    }

    @Test
    fun `cancelled selection stops retries`() = runTest {
        val eligible = subnet.copy(taoReserveInPlanks = BigInteger.TEN.pow(15))
        `when`(repository.quickPickSubnets()).thenReturn(listOf(eligible))
        `when`(availability.isEnabled(subnetId)).thenReturn(true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.ONE)
        val retries = MutableStateFlow(0)
        val emitted = mutableListOf<ExtendedLoadingState<BittensorSubnet>>()
        val job = backgroundScope.launch { interactor.observeSelection(BittensorQuickPickOrder.POOL_SIZE, retries).collect(emitted::add) }
        runCurrent()
        job.cancel()
        runCurrent()
        val count = emitted.size
        retries.value += 1
        runCurrent()
        assertEquals(count, emitted.size)
        assertFalse(emitted.any { it is ExtendedLoadingState.Error })
    }

    @Test
    fun `resolve uses current chain price`() = runBlocking<Unit> {
        `when`(availability.isEnabled(subnetId)).thenReturn(true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.ONE)

        assertEquals(BigInteger.ONE, interactor.resolve(listOf(subnet), reference).priceInRao)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `re-registration does not silently select a replacement`() = runBlocking<Unit> {
        interactor.resolve(listOf(subnet.copy(registrationBlock = BigInteger.ONE)), reference)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `removed subnet is unavailable`() = runBlocking<Unit> {
        interactor.resolve(subnets = emptyList(), reference)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `disabled trading is unavailable`() = runBlocking<Unit> {
        `when`(availability.isEnabled(subnetId)).thenReturn(false)
        interactor.resolve(listOf(subnet), reference)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero price is unavailable`() = runBlocking<Unit> {
        `when`(availability.isEnabled(subnetId)).thenReturn(true)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.ZERO)
        interactor.resolve(listOf(subnet), reference)
    }
}
