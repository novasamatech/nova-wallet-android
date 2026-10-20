package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorSelectValidatorViewModelTest {

    private val resources = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") "label" else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }
    private val logger = mock(Logger::class.java)
    private val retryController = BittensorRetryController(resources, logger)
    private val interactor = mock(BittensorValidatorInteractor::class.java)
    private val icons = mock(AddressIconGenerator::class.java)
    private val icon = mock(Drawable::class.java)
    private val router = mock(StakingRouter::class.java)
    private val externalActions = mock(ExternalActions.Presentation::class.java)
    private val tokenFormatter = mock(TokenFormatter::class.java)
    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)
    private val communicator = RecordingCommunicator()
    private val models = mutableListOf<BittensorSelectValidatorViewModel>()
    private val directoryReady = CompletableDeferred<BittensorValidatorDirectory>()

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(interactor.yields(BittensorNetuid.Root)).thenReturn(emptyMap())
        `when`(interactor.chain()).thenThrow(IllegalStateException("Optional stake price unavailable"))
        Unit
    }

    @After
    fun teardown() = runBlocking {
        directoryReady.cancel()
        models.forEach { it.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `search hiding preselected validator does not disable confirmation`() = runBlocking {
        val hotkey = AccountIdKey(byteArrayOf(1))
        val validator = BittensorValidator(hotkey, "Alice")
        val eligible = BittensorSubnetValidator(validator, "address-A", BigDecimal.ZERO, true, recommended = true)
        val status = mock(BittensorValidatorDirectoryStatus::class.java)
        val directory = BittensorValidatorDirectory(listOf(eligible), status, null)
        `when`(icons.createAddressIcon(hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT))
            .thenReturn(icon)
        `when`(interactor.directory(BittensorNetuid.Root)).thenAnswer { runBlocking { directoryReady.await() } }
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val state = BittensorSelectionState()
        val request = BittensorSelectValidatorCommunicator.Request(0, "address-A", state)
        val viewModel = createViewModel(request)
        viewModel.search("No matching validator")
        directoryReady.complete(directory)

        withTimeout(5_000) { viewModel.buttonState.first { it == ButtonState.NORMAL } }
        assertEquals(emptyList<BittensorValidatorUi>(), viewModel.rows.first())
        viewModel.confirmClicked()
        val response = withTimeout(5_000) { communicator.responses.receive() }
        assertEquals("address-A", response.address)
        assertEquals(state.flowId, response.selectionState.flowId)
    }

    @Test
    fun `Apply followed by Back returns filters without selecting a validator and reopening restores them`() = runBlocking {
        val state = BittensorSelectionState()
        val request = BittensorSelectValidatorCommunicator.Request(0, null, state)
        val viewModel = createViewModel(request)
        val filters = BittensorValidatorFilters(BittensorValidatorSorting.NAME, false)
        viewModel.applyFilters(filters)
        viewModel.back()
        val response = communicator.responses.receive()
        assertNull(response.address)
        assertNull(response.subnetId)

        val parentState = state.withResponse(response)
        val reopenedRequest = BittensorSelectValidatorCommunicator.Request(0, null, parentState)
        val reopened = createViewModel(reopenedRequest)
        assertEquals(filters, reopened.filters.value)
        assertEquals(BittensorValidatorFilters(), parentState.filtersFor(BittensorNetuid.Subnet(1)))
    }

    @Test
    fun `remove filters followed by Back preserves the change`() = runBlocking {
        val state = BittensorSelectionState()
        val request = BittensorSelectValidatorCommunicator.Request(0, null, state)
        val viewModel = createViewModel(request)
        viewModel.removeFilters()
        viewModel.back()
        val response = communicator.responses.receive()
        assertEquals(false, response.selectionState.rootFilters.hideUnusualApy)
        assertNull(response.address)
    }

    private fun createViewModel(request: BittensorSelectValidatorCommunicator.Request): BittensorSelectValidatorViewModel {
        return BittensorSelectValidatorViewModel(
            request, communicator, interactor, icons, router, externalActions, resources, logger,
            retryController, tokenFormatter, subnetInteractor
        ).also { models += it }
    }

    private class RecordingCommunicator : BittensorSelectValidatorCommunicator {
        val responses = Channel<BittensorSelectValidatorCommunicator.Response>(Channel.UNLIMITED)
        override val responseFlow: Flow<BittensorSelectValidatorCommunicator.Response> = emptyFlow()
        override val latestResponse: BittensorSelectValidatorCommunicator.Response? = null
        override val lastState: BittensorSelectValidatorCommunicator.Response? = null
        override val lastInput: BittensorSelectValidatorCommunicator.Request? = null
        override fun openRequest(request: BittensorSelectValidatorCommunicator.Request) = Unit
        override fun respond(response: BittensorSelectValidatorCommunicator.Response) {
            responses.trySend(response).getOrThrow()
        }
    }
}
