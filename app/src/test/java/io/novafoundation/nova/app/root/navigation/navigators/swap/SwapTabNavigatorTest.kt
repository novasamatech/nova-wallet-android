package io.novafoundation.nova.app.root.navigation.navigators.swap

import android.os.Bundle
import androidx.navigation.NavAction
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapEntryPoint
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapDirectionParcel
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapTabPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.same
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SwapTabNavigatorTest {

    private val contextManager = mock(ContextManager::class.java)
    private val splitHolder = SplitScreenNavigationHolder(contextManager)
    private val rootHolder = RootNavigationHolder(contextManager)
    private val registry = NavigationHoldersRegistry(splitHolder, rootHolder)
    private val screenController = mock(NavController::class.java)
    private val tabController = mock(NavController::class.java)
    private val screenDestination = mock(NavDestination::class.java)
    private val tabDestination = mock(NavDestination::class.java)
    private val tabGraph = mock(NavGraph::class.java)
    private val navigator = SwapTabNavigator(registry)

    @Before
    fun setUp() {
        splitHolder.attach(screenController)
        `when`(screenController.currentDestination).thenReturn(screenDestination)
        `when`(screenDestination.getAction(R.id.back_to_main)).thenReturn(NavAction(0))
        `when`(tabController.currentDestination).thenReturn(tabDestination)
        `when`(tabDestination.id).thenReturn(R.id.walletFragment)
        `when`(tabController.graph).thenReturn(tabGraph)
        `when`(tabGraph.startDestination).thenReturn(R.id.walletFragment)
    }

    @Test
    fun `asset detail opens main swaps tab with its token and clears screens above main`() {
        val settings = SwapSettingsPayload.DefaultFlow(AssetPayload("chain", 0), SwapEntryPoint.ASSET_DETAILS)
        mockConstruction(Bundle::class.java).use { bundles ->
            navigator.attach(tabController)
            navigator.openSwaps(settings)

            verify(screenController).navigate(R.id.back_to_main, null, null, null)
            val saved = ArgumentCaptor.forClass(SwapTabPayload::class.java)
            val bundle = bundles.constructed().single()
            verify(bundle).putParcelable(anyString(), saved.capture())
            assertSame(settings, saved.value.settings)
            assertTrue(saved.value.requestId.isNotEmpty())

            val options = ArgumentCaptor.forClass(NavOptions::class.java)
            verify(tabController).navigate(eq(R.id.swapsTabFragment), same(bundle), options.capture())
            assertEquals(R.id.walletFragment, options.value.popUpTo)
            assertTrue(options.value.shouldLaunchSingleTop())
        }
    }

    @Test
    fun `request survives main view recreation and is delivered once`() {
        val settings = SwapSettingsPayload.RepeatOperation(
            assetIn = AssetPayload("chain", 0),
            assetOut = AssetPayload("chain", 2),
            amount = BigInteger.valueOf(123),
            direction = SwapDirectionParcel.SPECIFIED_OUT,
            source = SwapEntryPoint.OPERATION_DETAILS
        )
        mockConstruction(Bundle::class.java).use { bundles ->
            navigator.openSwaps(settings)
            verifyNoInteractions(tabController)
            navigator.attach(tabController)
            navigator.detach()
            navigator.attach(tabController)

            val saved = ArgumentCaptor.forClass(SwapTabPayload::class.java)
            verify(bundles.constructed().single()).putParcelable(anyString(), saved.capture())
            assertSame(settings, saved.value.settings)
        }
    }

    @Test
    fun `opening already selected swaps without payload preserves the draft`() {
        `when`(tabDestination.id).thenReturn(R.id.swapsTabFragment)
        mockConstruction(Bundle::class.java).use { bundles ->
            navigator.attach(tabController)
            navigator.openSwaps()

            assertTrue(bundles.constructed().isEmpty())
        }
    }
}
