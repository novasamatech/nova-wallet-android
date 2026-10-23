package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator
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

    private val analyticsService = RecordingBittensorAnalytics()
    private val analytics = analyticsService

    private val resources = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") label(invocation.arguments) else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }
    private val logger = mock(Logger::class.java)
    private val retryController = BittensorRetryController(resources, logger, mock(AutomaticInteractionGate::class.java))
    private val interactor = mock(BittensorValidatorInteractor::class.java)
    private val icons = mock(AddressIconGenerator::class.java)
    private val icon = mock(Drawable::class.java)
    private val router = mock(StakingRouter::class.java)
    private val externalActions = mock(ExternalActions.Presentation::class.java)
    private val amountFormatter = mock(BittensorAmountFormatter::class.java)
    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)
    private val communicator = RecordingCommunicator()
    private val filtersCommunicator = RecordingFiltersCommunicator()
    private val models = mutableListOf<BittensorSelectValidatorViewModel>()
    private val directoryReady = CompletableDeferred<BittensorValidatorDirectory>()

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(interactor.yields(BittensorNetuid.Root)).thenReturn(emptyMap())
        `when`(interactor.yields(BittensorNetuid.Root, includeUnreliable = true)).thenReturn(emptyMap())
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

    @Test
    fun `header shows a loading count until the directory snapshot and never more rows than the total`() = runBlocking<Unit> {
        val rows = listOf(validator(1, "Alpha"), validator(2, "Beta"), validator(3, "Gamma"))
        val directory = directory(rows)
        `when`(interactor.directory(BittensorNetuid.Root)).thenAnswer { runBlocking { directoryReady.await() } }
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))

        assertEquals(label(arrayOf(R.string.bittensor_validator_list_count_loading)), viewModel.header.first().count)
        directoryReady.complete(directory)
        val loaded = label(arrayOf(R.string.bittensor_validator_list_count, 3, 3))
        withTimeout(5_000) { viewModel.header.first { it.count == loaded } }
        viewModel.search("Alpha")
        val searched = label(arrayOf(R.string.bittensor_validator_list_count, 1, 3))
        withTimeout(5_000) { viewModel.header.first { it.count == searched } }
    }

    @Test
    fun `preselected validator is pinned first under every sort`() = runBlocking<Unit> {
        val rows = listOf(validator(1, "Alpha", stake = "1"), validator(2, "Zulu", stake = "100"), validator(3, "Mike", stake = "50"))
        val directory = directory(rows)
        `when`(interactor.directory(BittensorNetuid.Root)).thenReturn(directory)
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, "address-3", BittensorSelectionState()))

        val expected = mapOf(
            BittensorValidatorSorting.APY to listOf("address-3", "address-2", "address-1"),
            BittensorValidatorSorting.TOTAL_STAKE to listOf("address-3", "address-2", "address-1"),
            BittensorValidatorSorting.NAME to listOf("address-3", "address-1", "address-2")
        )
        expected.forEach { (sorting, order) ->
            viewModel.applyFilters(BittensorValidatorFilters(sorting, hideUnusualApy = false))
            withTimeout(5_000) { viewModel.rows.first { rows -> rows.map { it.eligibility.address } == order } }
        }
    }

    @Test
    fun `tapping an inactive validator explains it and keeps the selection`() = runBlocking<Unit> {
        val active = validator(1, "Active")
        val inactive = validator(2, "Inactive").copy(inactive = true)
        val directory = directory(listOf(active, inactive))
        `when`(interactor.directory(BittensorNetuid.Root)).thenReturn(directory)
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, "address-1", BittensorSelectionState()))
        viewModel.applyFilters(BittensorValidatorFilters(BittensorValidatorSorting.NAME, hideUnusualApy = false))
        val inactiveRow = withTimeout(5_000) { viewModel.rows.first { it.size == 2 } }.single { it.eligibility.inactive }
        val alert = async(start = CoroutineStart.UNDISPATCHED) { viewModel.inactiveValidatorEvents.first() }

        viewModel.select(inactiveRow)

        withTimeout(5_000) { alert.await() }
        assertEquals("address-1", viewModel.selectedAddress.value)
    }

    @Test
    fun `info opens the validator info screen with the row snapshot`() = runBlocking<Unit> {
        val validator = validator(1, "Alpha", stake = "10").copy(commissionRate = BigDecimal("0.18"))
        val directory = directory(listOf(validator))
        `when`(interactor.directory(BittensorNetuid.Root)).thenReturn(directory)
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))
        val row = withTimeout(5_000) { viewModel.rows.first { it.isNotEmpty() } }.single()

        viewModel.infoClicked(row)

        val captor = org.mockito.ArgumentCaptor.forClass(BittensorValidatorInfoPayload::class.java)
        Mockito.verify(router).openBittensorValidatorInfo(captor.capture() ?: placeholderPayload)
        val payload = captor.value
        assertEquals("address-1", payload.address)
        assertEquals("Alpha", payload.detail?.name)
        assertEquals(BigDecimal("0.18"), payload.detail?.commissionRate)
        assertEquals(0, payload.subnetId)
    }

    @Test
    fun `inactive preselected validator cannot be confirmed`() = runBlocking<Unit> {
        val inactive = validator(1, "Inactive").copy(inactive = true)
        val directory = directory(listOf(inactive, validator(2, "Active")))
        `when`(interactor.directory(BittensorNetuid.Root)).thenReturn(directory)
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, "address-1", BittensorSelectionState()))
        viewModel.applyFilters(BittensorValidatorFilters(BittensorValidatorSorting.NAME, hideUnusualApy = false))
        withTimeout(5_000) { viewModel.rows.first { it.size == 2 } }

        assertEquals(ButtonState.DISABLED, viewModel.buttonState.first())
        viewModel.confirmClicked()
        assertNull(communicator.responses.tryReceive().getOrNull())
    }

    @Test
    fun `a failed directory shows an inline warning and Try again reloads it`() = runBlocking<Unit> {
        val directory = directory(listOf(validator(1, "Alpha")))
        var attempts = 0
        `when`(interactor.directory(BittensorNetuid.Root)).thenAnswer {
            attempts++
            if (attempts == 1) throw IllegalStateException("Validators unavailable") else directory
        }
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))

        val failure = withTimeout(5_000) { viewModel.listFailure.first { it != null } }
        assertEquals(label(arrayOf(R.string.bittensor_validators_fetch_failed_title)), failure?.title)
        assertEquals(label(arrayOf(R.string.bittensor_validators_fetch_failed_message)), failure?.message)
        assertEquals(false, viewModel.isEmpty.first())
        assertEquals(ButtonState.DISABLED, viewModel.buttonState.first())

        viewModel.retryList()

        withTimeout(5_000) { viewModel.rows.first { it.size == 1 } }
        assertNull(viewModel.listFailure.first())
    }

    @Test
    fun `filters open with the current filters and applied filters come back to the list`() = runBlocking<Unit> {
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))

        viewModel.filtersClicked()
        assertEquals(BittensorValidatorFilters(), filtersCommunicator.requests.single().filters)

        val applied = BittensorValidatorFilters(BittensorValidatorSorting.NAME, hideUnusualApy = false)
        filtersCommunicator.responses.emit(BittensorValidatorFiltersCommunicator.Response(applied))
        withTimeout(5_000) { viewModel.filters.first { it == applied } }
    }

    @Test
    fun `system back closes an open search before leaving`() {
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))

        viewModel.searchClicked()
        viewModel.backPressed()

        assertEquals(false, viewModel.searchVisible.value)
        Mockito.verify(router, Mockito.never()).back()
        viewModel.backPressed()
        Mockito.verify(router).back()
    }

    @Test
    fun `closing the search clears the query`() = runBlocking<Unit> {
        val directory = directory(listOf(validator(1, "Alpha"), validator(2, "Beta")))
        `when`(interactor.directory(BittensorNetuid.Root)).thenReturn(directory)
        `when`(interactor.rankDirectory(BittensorNetuid.Root, directory)).thenReturn(directory)
        val viewModel = createViewModel(BittensorSelectValidatorCommunicator.Request(0, null, BittensorSelectionState()))
        viewModel.applyFilters(BittensorValidatorFilters(BittensorValidatorSorting.NAME, hideUnusualApy = false))

        viewModel.searchClicked()
        assertEquals(true, viewModel.searchVisible.value)
        viewModel.search("Alpha")
        withTimeout(5_000) { viewModel.rows.first { it.size == 1 } }
        viewModel.searchClicked()

        assertEquals(false, viewModel.searchVisible.value)
        withTimeout(5_000) { viewModel.rows.first { it.size == 2 } }
    }

    private fun createViewModel(request: BittensorSelectValidatorCommunicator.Request): BittensorSelectValidatorViewModel {
        return BittensorSelectValidatorViewModel(
            analytics, request, communicator, interactor, icons, router, externalActions, resources, logger,
            retryController, amountFormatter, subnetInteractor, filtersCommunicator
        ).also { models += it }
    }

    private suspend fun validator(index: Int, name: String, stake: String? = null): BittensorSubnetValidator {
        val hotkey = AccountIdKey(byteArrayOf(index.toByte()))
        `when`(icons.createAddressIcon(hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT))
            .thenReturn(icon)
        val validator = BittensorValidator(hotkey, name, stake?.toBigDecimal())
        return BittensorSubnetValidator(validator, "address-$index", BigDecimal.ZERO, true, recommended = true)
    }

    private fun directory(rows: List<BittensorSubnetValidator>) =
        BittensorValidatorDirectory(rows, mock(BittensorValidatorDirectoryStatus::class.java), null)

    private val placeholderPayload = BittensorValidatorInfoPayload(0, "")

    private class RecordingFiltersCommunicator : BittensorValidatorFiltersCommunicator {
        val requests = mutableListOf<BittensorValidatorFiltersCommunicator.Request>()
        val responses = kotlinx.coroutines.flow.MutableSharedFlow<BittensorValidatorFiltersCommunicator.Response>(extraBufferCapacity = 1)
        override val responseFlow: Flow<BittensorValidatorFiltersCommunicator.Response> = responses
        override val latestResponse: BittensorValidatorFiltersCommunicator.Response? = null
        override val lastState: BittensorValidatorFiltersCommunicator.Response? = null
        override val lastInput: BittensorValidatorFiltersCommunicator.Request? = null
        override fun openRequest(request: BittensorValidatorFiltersCommunicator.Request) {
            requests += request
        }
        override fun respond(response: BittensorValidatorFiltersCommunicator.Response) = Unit
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

private fun label(arguments: Array<Any?>): String = arguments.flatMap { if (it is Array<*>) it.toList() else listOf(it) }.joinToString("|")
