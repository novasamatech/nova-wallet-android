package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisUpdate
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BittensorEstimatesMixinTest {

    @Test
    fun `position preserves settlement while loading loaded and failed then releases on fresh history`() = runTest {
        val interactor = mock(BittensorCostBasisInteractor::class.java)
        val yields = mock(BittensorAlphaYieldRepository::class.java)
        val logger = mock(Logger::class.java)
        val mixin = BittensorEstimatesMixin(interactor, yields, logger)
        val context = flowOf<BittensorEstimateContext?>(null)
        val basis = BittensorEstimatedCostBasis(emptyList(), false)
        val failure = IllegalStateException("offline")
        val states = flowOf(
            BittensorCostBasisUpdate(BittensorCostBasisState.Loading, true),
            BittensorCostBasisUpdate(BittensorCostBasisState.Loaded(basis), true),
            BittensorCostBasisUpdate(BittensorCostBasisState.Failed(failure), true),
            BittensorCostBasisUpdate(BittensorCostBasisState.Loaded(basis), false)
        )
        `when`(interactor.observeUpdates(context)).thenReturn(states)

        val actual = mixin.observePositionCostBasis(context).toList()

        assertEquals(listOf(true, true, true, false), actual.map { it.isSettling })
        assertTrue(actual[0].state is ExtendedLoadingState.Loading)
        assertTrue(actual[1].state is ExtendedLoadingState.Loaded)
        assertTrue(actual[2].state is ExtendedLoadingState.Error)
        assertTrue(actual[3].state is ExtendedLoadingState.Loaded)
        verify(logger).log(failure)
    }

    @Test
    fun `pending account and invalidation show loading while absent context is settled unavailable`() = runTest {
        val interactor = mock(BittensorCostBasisInteractor::class.java)
        val yields = mock(BittensorAlphaYieldRepository::class.java)
        val logger = mock(Logger::class.java)
        val mixin = BittensorEstimatesMixin(interactor, yields, logger)
        val context = flowOf<BittensorEstimateContext?>(null)
        val basis = BittensorEstimatedCostBasis(emptyList(), false)
        val failure = IllegalStateException("offline")
        val states = flowOf(
            BittensorCostBasisState.Loading,
            BittensorCostBasisState.Loaded(basis),
            BittensorCostBasisState.Loading,
            BittensorCostBasisState.Failed(failure),
            BittensorCostBasisState.Unavailable
        )
        `when`(interactor.observe(context)).thenReturn(states)

        val actual = mixin.observeCostBasis(context).toList()

        assertEquals(5, actual.size)
        assertTrue(actual[0] is ExtendedLoadingState.Loading)
        assertTrue(actual[1] is ExtendedLoadingState.Loaded)
        assertTrue(actual[2] is ExtendedLoadingState.Loading)
        assertTrue(actual[3] is ExtendedLoadingState.Error)
        assertEquals(ExtendedLoadingState.Loaded<BittensorEstimatedCostBasis?>(null), actual[4])
        verify(logger).log(failure)
    }
}
