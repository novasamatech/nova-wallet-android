package io.novafoundation.nova.app.root.navigation.navigators.swap

import androidx.navigation.NavAction
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.common.resources.ContextManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SwapCompletionNavigationTest {

    private val context = mock(ContextManager::class.java)

    private val splitHolder = SplitScreenNavigationHolder(context)

    private val rootHolder = RootNavigationHolder(context)

    private val registry = NavigationHoldersRegistry(splitHolder, rootHolder)

    private val commonNavigator = mock(Navigator::class.java)

    private val controller = mock(NavController::class.java)

    private val destination = mock(NavDestination::class.java)

    private val navigator = SwapNavigator(registry, commonNavigator)

    @Before
    fun setup() {
        splitHolder.attach(controller)
        `when`(controller.currentDestination).thenReturn(destination)
        `when`(destination.getAction(R.id.action_finish_swap_flow)).thenReturn(NavAction(0))
        `when`(controller.popBackStack()).thenReturn(true)
    }

    @Test
    fun `tab completion closes result and retains the main screen`() {
        navigator.finishSwap(returnToTab = true)
        verify(controller).popBackStack()
        verify(controller, never()).navigate(R.id.action_finish_swap_flow, null, null, null)
        verifyNoInteractions(commonNavigator)
    }

    @Test
    fun `standalone completion pops the swap flow to its caller`() {
        navigator.finishSwap(returnToTab = false)
        verify(controller).navigate(R.id.action_finish_swap_flow, null, null, null)
        verify(controller, never()).popBackStack()
        verifyNoInteractions(commonNavigator)
    }
}
