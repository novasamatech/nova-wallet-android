package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator.Method
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class SwapSettingsSheetViewModelTest {

    private val resources = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") invocation.arguments.joinToString("|") else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }

    private val router = mock(SwapRouter::class.java)

    private val communicator = mock(SlippageSettingsCommunicator::class.java)

    private val alertFactory = SlippageAlertMixinFactory(resources)

    private val models = mutableListOf<SwapSettingsSheetViewModel>()

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
    fun `default preset can be applied unchanged and keeps the request identity`() = runBlocking {
        val viewModel = createViewModel()

        assertEquals(1, viewModel.selectedTip.first())
        assertTrue(viewModel.selection.value is SwapSlippageSelection.Preset)
        awaitEnabled(viewModel)
        verifyNoInteractions(communicator, router)

        viewModel.applyClicked()

        val response = captureResponse()
        assertEquals(0.005, response.fraction, 1e-12)
        assertEquals("form-request", response.requestId)
        verify(router, timeout(5_000)).back()
    }

    @Test
    fun `custom input deselects presets and is only published by apply`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("0.75")

        assertEquals(-1, viewModel.selectedTip.first())
        assertNull(viewModel.slippageWarningState.first())
        awaitEnabled(viewModel)
        verifyNoInteractions(communicator, router)

        viewModel.applyClicked()
        val response = captureResponse()
        assertEquals(0.0075, response.fraction, 1e-12)
        assertEquals(Method.CUSTOM, response.method)
    }

    @Test
    fun `high valid slippage warns but can be applied`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("15")

        assertEquals("${R.string.swap_slippage_warning_too_big}", viewModel.slippageWarningState.first())
        awaitEnabled(viewModel)
        viewModel.applyClicked()
        assertEquals(0.15, captureResponse().fraction, 1e-12)
    }

    @Test
    fun `out of range value shows an error and cannot be applied even before UI updates`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("60")
        viewModel.applyClicked()

        withTimeout(5_000) { viewModel.slippageInputValidationResult.first { it is FieldValidationResult.Error } }
        withTimeout(5_000) { viewModel.buttonState.first { it is DescriptiveButtonState.Disabled } }
        assertNull(viewModel.slippageWarningState.first())
        verifyNoInteractions(communicator, router)
    }

    @Test
    fun `preset replaces invalid custom input and clears validation`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("60")
        withTimeout(5_000) { viewModel.slippageInputValidationResult.first { it is FieldValidationResult.Error } }

        viewModel.tipClicked(0)

        assertTrue(viewModel.selection.value is SwapSlippageSelection.Preset)
        assertEquals(0, viewModel.selectedTip.first())
        withTimeout(5_000) { viewModel.slippageInputValidationResult.first { it is FieldValidationResult.Ok } }
        awaitEnabled(viewModel)
        viewModel.applyClicked()
        assertEquals(0.001, captureResponse().fraction, 1e-12)
    }

    @Test
    fun `reset restores default without publishing until apply`() = runBlocking {
        val viewModel = createViewModel(selected = 0.02)
        assertEquals(SwapSlippageSelection.Custom("2"), viewModel.selection.value)

        viewModel.resetClicked()

        assertEquals(1, viewModel.selectedTip.first())
        verifyNoInteractions(communicator, router)
        viewModel.applyClicked()
        val response = captureResponse()
        assertEquals(0.005, response.fraction, 1e-12)
        assertEquals(Method.RESET, response.method)
    }

    @Test
    fun `clearing custom input restores default and comma input is parsed as percent`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("0,25")
        assertEquals(SwapSlippageSelection.Custom("0.25"), viewModel.selection.value)

        viewModel.selection.value = viewModel.selectionFromInput("")
        assertEquals(1, viewModel.selectedTip.first())
        assertTrue(viewModel.selection.value is SwapSlippageSelection.Preset)
    }

    @Test
    fun `incomplete custom input cannot be applied`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput(".")
        withTimeout(5_000) { viewModel.buttonState.first { it is DescriptiveButtonState.Disabled } }
        viewModel.applyClicked()
        verifyNoInteractions(communicator, router)
    }

    @Test
    fun `configured lower boundary is valid`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("0.01")
        awaitEnabled(viewModel)
        viewModel.applyClicked()
        assertEquals(0.0001, captureResponse().fraction, 1e-12)
    }

    @Test
    fun `configured upper boundary is valid`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("50")
        awaitEnabled(viewModel)
        viewModel.applyClicked()
        assertEquals(0.5, captureResponse().fraction, 1e-12)
    }

    @Test
    fun `below lower boundary cannot be applied`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.selection.value = viewModel.selectionFromInput("0.001")
        viewModel.applyClicked()
        withTimeout(5_000) { viewModel.slippageInputValidationResult.first { it is FieldValidationResult.Error } }
        verifyNoInteractions(communicator, router)
    }

    private suspend fun awaitEnabled(viewModel: SwapSettingsSheetViewModel) {
        withTimeout(5_000) { viewModel.buttonState.first { it is DescriptiveButtonState.Enabled } }
    }

    private fun captureResponse(): SlippageSettingsCommunicator.Response {
        val captor = ArgumentCaptor.forClass(SlippageSettingsCommunicator.Response::class.java)
        verify(communicator, timeout(5_000)).respond(captor.capture() ?: SlippageSettingsCommunicator.Response(0.0))
        return captor.value
    }

    private fun createViewModel(selected: Double = 0.005): SwapSettingsSheetViewModel {
        val payload = SlippageSettingsPayload(
            selected = selected,
            default = 0.005,
            tips = listOf(0.001, 0.005, 0.01),
            minimum = 0.0001,
            maximum = 0.5,
            small = 0.0005,
            big = 0.01,
            requestId = "form-request"
        )
        val validator = SlippageFieldValidator(payload.config(), resources)
        return SwapSettingsSheetViewModel(router, resources, validator, alertFactory, payload, communicator).also(models::add)
    }
}
