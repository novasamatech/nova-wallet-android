package io.novafoundation.nova.feature_swap_impl.domain

import com.google.gson.Gson
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.domain.config.GlobalConfig
import io.novafoundation.nova.common.domain.config.SwapPairConfig
import io.novafoundation.nova.feature_swap_impl.data.SwapSelectionDataSource
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapSelectionInteractor
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.doAnswer

class SwapSelectionInteractorTest {
    private val preferences = mock(Preferences::class.java)
    private val remote = mock(GlobalConfigDataSource::class.java)
    private val registry = mock(ChainRegistry::class.java)
    private val stored = mutableMapOf<String, String>()
    private val source = SwapSelectionDataSource(preferences, Gson())
    private val interactor = SwapSelectionInteractor(source, remote, registry)
    private val chainId = Chain.Geneses.POLKADOT_ASSET_HUB
    private val dot = asset(0)
    private val usdc = asset(2)

    @Test
    fun `last selection is isolated per wallet and beats remote default`() = runBlocking<Unit> {
        prepare(SwapPairConfig(config(0), config(2)))
        interactor.remember(1, usdc, dot)
        assertEquals(SwapSelectionInteractor.Selection(usdc, dot), interactor.initialSelection(1))
        assertEquals(SwapSelectionInteractor.Selection(dot, usdc), interactor.initialSelection(2))
    }

    @Test
    fun `one sided selection remains one sided when reopened`() = runBlocking<Unit> {
        prepare(null)
        interactor.remember(1, null, usdc)
        assertEquals(SwapSelectionInteractor.Selection(null, usdc), interactor.initialSelection(1))
        verify(remote, never()).getGlobalConfig()
    }

    @Test
    fun `removed saved asset uses valid remote pair`() = runBlocking<Unit> {
        prepare(SwapPairConfig(config(2), config(0)))
        source.set(1, SwapSelectionDataSource.Selection(config(99), config(2)))
        assertEquals(SwapSelectionInteractor.Selection(usdc, dot), interactor.initialSelection(1))
    }

    @Test
    fun `missing or unresolved remote pair uses bundled DOT USDC`() = runBlocking<Unit> {
        prepare(SwapPairConfig(config(99), config(2)))
        assertEquals(SwapSelectionInteractor.Selection(dot, usdc), interactor.initialSelection(1))
        `when`(remote.getGlobalConfig()).thenReturn(GlobalConfig("m", "p", "s", null))
        assertEquals(SwapSelectionInteractor.Selection(dot, usdc), interactor.initialSelection(2))
    }

    private suspend fun prepare(pair: SwapPairConfig?) {
        doAnswer { stored[it.getArgument(0)] = it.getArgument(1); null }.`when`(preferences).putString(anyString(), anyString())
        `when`(preferences.getString(anyString())).thenAnswer { stored[it.getArgument(0)] }
        val chain = mock(Chain::class.java)
        `when`(chain.id).thenReturn(chainId)
        `when`(chain.assetsById).thenReturn(mapOf(0 to dot, 2 to usdc))
        `when`(registry.currentChains).thenReturn(MutableStateFlow(listOf(chain)))
        `when`(remote.getGlobalConfig()).thenReturn(GlobalConfig("m", "p", "s", null, pair))
    }

    private fun config(id: Int) = SwapPairConfig.Asset(chainId, id)
    private fun asset(id: Int) = mock(Chain.Asset::class.java).also {
        `when`(it.chainId).thenReturn(chainId)
        `when`(it.id).thenReturn(id)
    }
}
