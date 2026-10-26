package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator.Method
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidatorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SwapOptionsViewModelTest {

    private val resources = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") invocation.arguments.joinToString("|") else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }
    private val router = mock(SwapRouter::class.java)
    private val stateProvider = mock(SwapSettingsStateProvider::class.java)
    private val communicator = mock(SlippageSettingsCommunicator::class.java)
    private val models = mutableListOf<SwapOptionsViewModel>()

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
    fun `payload apply responds with the method and never touches the swap settings`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(selected = 0.005))

        viewModel.tipClicked(0)
        awaitEnabled(viewModel)
        viewModel.applyClicked()

        val response = captureResponse()
        assertEquals(Method.PRESET, response.method)
        assertEquals(0.001, response.fraction, 1e-12)
        verify(router, timeout(5_000)).back()
        verify(stateProvider, never()).getSwapSettingsState(any() ?: this)
    }

    @Test
    fun `reset then apply reports a reset`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(selected = 0.01))

        viewModel.resetClicked()
        awaitEnabled(viewModel)
        viewModel.applyClicked()

        val response = captureResponse()
        assertEquals(Method.RESET, response.method)
        assertEquals(0.005, response.fraction, 1e-12)
    }

    @Test
    fun `opening with the saved preset keeps apply disabled`() = runBlocking<Unit> {
        // 0.35 / 100 is not exactly 0.0035 in floating point: the saved value must still read as unchanged.
        val viewModel = createViewModel(payload(selected = 0.0035, tips = listOf(0.001, 0.0035, 0.01, 0.03)))

        // The saved preset fills the field; it must not read as a change.
        withTimeout(5_000) { viewModel.slippageInput.first { it.isNotEmpty() } }
        delay(100)
        val state = withTimeout(5_000) { viewModel.buttonState.first() }

        assertTrue(state is DescriptiveButtonState.Disabled)
        assertTrue(withTimeout(5_000) { viewModel.resetButtonEnabled.first() })
    }

    @Test
    fun `input precision and title depend on the payload`() {
        val withPayload = createViewModel(payload(selected = 0.005))
        assertTrue(withPayload.limitsInputPrecision)
        assertEquals("${R.string.common_settings}", withPayload.title)

        val swap = createViewModel(null)
        assertFalse(swap.limitsInputPrecision)
        assertEquals("${R.string.swap_settings_title}", swap.title)
    }

    private suspend fun awaitEnabled(viewModel: SwapOptionsViewModel) {
        withTimeout(5_000) { viewModel.buttonState.first { it is DescriptiveButtonState.Enabled } }
    }

    private fun captureResponse(): SlippageSettingsCommunicator.Response {
        val captor = ArgumentCaptor.forClass(SlippageSettingsCommunicator.Response::class.java)
        verify(communicator, timeout(5_000)).respond(captor.capture() ?: SlippageSettingsCommunicator.Response(0.0))
        return captor.value
    }

    private fun payload(selected: Double, tips: List<Double> = listOf(0.001, 0.005, 0.01, 0.03)) = SlippageSettingsPayload(
        selected = selected,
        default = 0.005,
        tips = tips,
        minimum = 0.0001,
        maximum = 0.5,
        small = 0.001,
        big = 0.03
    )

    private fun createViewModel(payload: SlippageSettingsPayload?) = SwapOptionsViewModel(
        swapRouter = router,
        resourceManager = resources,
        swapSettingsStateProvider = stateProvider,
        slippageFieldValidatorFactory = SlippageFieldValidatorFactory(resources),
        swapInteractor = mock(SwapInteractor::class.java),
        descriptionBottomSheetLauncher = mock(DescriptionBottomSheetLauncher::class.java),
        slippageAlertMixinFactory = SlippageAlertMixinFactory(resources),
        payload = payload,
        slippageSettingsCommunicator = communicator
    ).also(models::add)
}
