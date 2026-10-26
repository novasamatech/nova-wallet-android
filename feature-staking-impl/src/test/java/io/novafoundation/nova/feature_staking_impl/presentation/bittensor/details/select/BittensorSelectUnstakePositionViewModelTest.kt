package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select

import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class BittensorSelectUnstakePositionViewModelTest {

    private val communicator = mock(BittensorSelectUnstakePositionCommunicator::class.java)

    private val router = mock(StakingRouter::class.java)

    private val position = BittensorExitPositionPayload(7L, 0, "validator")

    private val option = BittensorSelectUnstakePositionCommunicator.Option(position, "validator", "amount", null)

    private val request = BittensorSelectUnstakePositionCommunicator.Request(listOf(option))

    private val viewModel = BittensorSelectUnstakePositionViewModel(request, communicator, router)

    @Test
    fun `selection returns position before closing the selector`() {
        viewModel.select(option)

        val order = inOrder(communicator, router)
        order.verify(communicator).respond(BittensorSelectUnstakePositionCommunicator.Response(position))
        order.verify(router).back()
    }

    @Test
    fun `option absent from request is ignored`() {
        viewModel.select(option.copy(position = position.copy(hotkeyAddress = "other")))

        verifyNoInteractions(communicator, router)
    }

    @Test
    fun `cancel closes without returning a selection`() {
        viewModel.back()

        verify(router).back()
        verifyNoInteractions(communicator)
    }
}
