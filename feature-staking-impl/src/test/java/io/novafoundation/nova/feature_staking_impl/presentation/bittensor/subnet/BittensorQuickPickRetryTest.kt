package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorQuickPickRetryTest {

    private val analyticsService = RecordingBittensorAnalytics()
    private val analytics = analyticsService

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val router = mock(StakingRouter::class.java)

    private val resources = mock(ResourceManager::class.java)

    private val logger = mock(Logger::class.java)

    private val retryController = BittensorRetryController(resources, logger)

    private val validatorCommunicator = mock(BittensorSelectValidatorCommunicator::class.java)

    private val responses = MutableSharedFlow<BittensorSelectValidatorCommunicator.Response>(extraBufferCapacity = 1)

    private lateinit var viewModel: BittensorWhereToStakeViewModel

    @Before
    fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(subnetInteractor.observeAsset()).thenReturn(emptyFlow())
        `when`(validatorCommunicator.responseFlow).thenReturn(responses)
        viewModel = BittensorWhereToStakeViewModel(analytics, subnetInteractor, router, retryController, validatorCommunicator)
    }

    @After
    fun cleanUp() = runBlocking {
        viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `choose immediately navigates with the order but no preselected subnet or validator`() {
        viewModel.quickPick(BittensorQuickPickOrder.POOL_SIZE)

        val payload = ArgumentCaptor.forClass(BittensorSubnetAmountPayload::class.java)
        verify(router).openBittensorSubnetAmount(payload.capture() ?: BittensorSubnetAmountPayload(1))
        assertEquals(BittensorQuickPickOrder.POOL_SIZE, payload.value.quickPickOrder)
        assertNull(payload.value.subnetId)
        assertNull(payload.value.validatorAddress)
        assertEquals(BittensorSubnetBrowseState.fromQuickPick(BittensorQuickPickOrder.POOL_SIZE), payload.value.selectionState.browseState)
        verifyNoMoreInteractions(router)
    }

    @Test
    fun `root opens directly without any quick pick work`() {
        viewModel.rootStaking()
        val state = ArgumentCaptor.forClass(BittensorSelectionState::class.java)
        verify(router).openBittensorRootStaking(org.mockito.ArgumentMatchers.isNull(), state.capture() ?: BittensorSelectionState())
        verifyNoMoreInteractions(router)
    }

    @Test
    fun `return from a subnet preserves its filters when entering root in the same flow`() = runBlocking {
        viewModel.quickPick(BittensorQuickPickOrder.AGE)
        val payload = ArgumentCaptor.forClass(BittensorSubnetAmountPayload::class.java)
        verify(router).openBittensorSubnetAmount(payload.capture() ?: BittensorSubnetAmountPayload(1))
        val filters = BittensorValidatorFilters(
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting.NAME,
            false
        )
        val changed = payload.value.selectionState.withFilters(
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(1),
            filters
        )
        responses.emit(BittensorSelectValidatorCommunicator.Response(null, null, changed))

        viewModel.rootStaking()
        val state = ArgumentCaptor.forClass(BittensorSelectionState::class.java)
        verify(router).openBittensorRootStaking(org.mockito.ArgumentMatchers.isNull(), state.capture() ?: BittensorSelectionState())
        assertEquals(filters, state.value.subnetFilters)
        assertEquals(BittensorValidatorFilters(), state.value.rootFilters)
    }

    @Test
    fun `a foreign flow response cannot change the carousel filter state`() = runBlocking {
        val filters = BittensorValidatorFilters(
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting.NAME,
            false
        )
        val foreign = BittensorSelectionState(rootFilters = filters)
        responses.emit(BittensorSelectValidatorCommunicator.Response(0, "foreign-validator", foreign))
        viewModel.rootStaking()

        val state = ArgumentCaptor.forClass(BittensorSelectionState::class.java)
        verify(router).openBittensorRootStaking(org.mockito.ArgumentMatchers.isNull(), state.capture() ?: BittensorSelectionState())
        assertEquals(BittensorValidatorFilters(), state.value.rootFilters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `quick pick payload cannot also identify an explicit subnet`() {
        BittensorSubnetAmountPayload(1, quickPickOrder = BittensorQuickPickOrder.POOL_SIZE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `existing position cannot be randomly selected`() {
        BittensorSubnetAmountPayload(existingPosition = true, quickPickOrder = BittensorQuickPickOrder.POOL_SIZE)
    }
}
