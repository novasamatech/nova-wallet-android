package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorValidatorFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorValidatorFiltersViewModelTest {

    private val router = mock(StakingRouter::class.java)
    private val communicator = RecordingCommunicator()
    private val models = mutableListOf<BittensorValidatorFiltersViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() = runBlocking {
        models.forEach { it.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `apply is enabled only after a change and reset only away from the defaults`() = runBlocking<Unit> {
        val viewModel = createViewModel(BittensorValidatorFilters())

        assertEquals(false, viewModel.isApplyEnabled.first())
        assertEquals(false, viewModel.isResetEnabled.first())

        viewModel.sortingChanged(BittensorValidatorSorting.NAME)
        withTimeout(5_000) { viewModel.isApplyEnabled.first { it } }
        withTimeout(5_000) { viewModel.isResetEnabled.first { it } }

        viewModel.sortingChanged(BittensorValidatorSorting.APY)
        withTimeout(5_000) { viewModel.isApplyEnabled.first { !it } }
    }

    @Test
    fun `reset restores the defaults without applying them`() = runBlocking<Unit> {
        val opened = BittensorValidatorFilters(BittensorValidatorSorting.TOTAL_STAKE, hideUnusualApy = false)
        val viewModel = createViewModel(opened)

        viewModel.reset()

        assertEquals(BittensorValidatorFilters(), viewModel.filters.value)
        withTimeout(5_000) { viewModel.isApplyEnabled.first { it } }
        assertEquals(emptyList<BittensorValidatorFiltersCommunicator.Response>(), communicator.responses)
    }

    @Test
    fun `apply returns the edited filters and back returns nothing`() {
        val viewModel = createViewModel(BittensorValidatorFilters())

        viewModel.hideUnusualApyChanged(false)
        viewModel.sortingChanged(BittensorValidatorSorting.TOTAL_STAKE)
        viewModel.apply()

        assertEquals(
            listOf(BittensorValidatorFiltersCommunicator.Response(BittensorValidatorFilters(BittensorValidatorSorting.TOTAL_STAKE, false))),
            communicator.responses
        )
        verify(router).back()

        val untouched = createViewModel(BittensorValidatorFilters())
        untouched.apply()
        assertEquals(1, communicator.responses.size)
    }

    @Test
    fun `back leaves without a response`() {
        val viewModel = createViewModel(BittensorValidatorFilters())
        viewModel.sortingChanged(BittensorValidatorSorting.NAME)

        viewModel.back()

        assertEquals(emptyList<BittensorValidatorFiltersCommunicator.Response>(), communicator.responses)
        verify(router).back()
    }

    private fun createViewModel(filters: BittensorValidatorFilters): BittensorValidatorFiltersViewModel {
        return BittensorValidatorFiltersViewModel(BittensorValidatorFiltersCommunicator.Request(filters, "TAO"), communicator, router)
            .also { models += it }
    }

    private class RecordingCommunicator : BittensorValidatorFiltersCommunicator {
        val responses = mutableListOf<BittensorValidatorFiltersCommunicator.Response>()
        override val responseFlow: Flow<BittensorValidatorFiltersCommunicator.Response> = emptyFlow()
        override val latestResponse: BittensorValidatorFiltersCommunicator.Response? = null
        override val lastState: BittensorValidatorFiltersCommunicator.Response? = null
        override val lastInput: BittensorValidatorFiltersCommunicator.Request? = null
        override fun openRequest(request: BittensorValidatorFiltersCommunicator.Request) = Unit
        override fun respond(response: BittensorValidatorFiltersCommunicator.Response) {
            responses += response
        }
    }
}
