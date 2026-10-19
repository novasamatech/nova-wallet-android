package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
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

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val router = mock(StakingRouter::class.java)

    private val resources = mock(ResourceManager::class.java)

    private val logger = mock(Logger::class.java)

    private val retryController = BittensorRetryController(resources, logger)

    private lateinit var viewModel: BittensorWhereToStakeViewModel

    @Before
    fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(subnetInteractor.observeAsset()).thenReturn(emptyFlow())
        viewModel = BittensorWhereToStakeViewModel(subnetInteractor, router, retryController)
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
        verifyNoMoreInteractions(router)
    }

    @Test
    fun `root opens directly without any quick pick work`() {
        viewModel.rootStaking()
        verify(router).openBittensorRootStaking()
        verifyNoMoreInteractions(router)
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
