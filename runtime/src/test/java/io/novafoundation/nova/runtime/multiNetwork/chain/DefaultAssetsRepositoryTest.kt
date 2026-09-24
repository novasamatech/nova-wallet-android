package io.novafoundation.nova.runtime.multiNetwork.chain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetsRemote
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

private const val CHAIN_ID = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"

@RunWith(MockitoJUnitRunner::class)
class DefaultAssetsRepositoryTest {

    @Mock
    lateinit var chainFetcher: ChainFetcher

    private fun repository() = DefaultAssetsRepository(chainFetcher)

    private fun remote(assets: List<DefaultAssetRemote> = listOf(DefaultAssetRemote(CHAIN_ID, 0))) =
        DefaultAssetsRemote(defaultAssets = assets)

    @Test
    fun `keeps the order the config declares`() = runBlocking<Unit> {
        `when`(chainFetcher.getDefaultAssets()).thenReturn(
            remote(assets = listOf(DefaultAssetRemote(CHAIN_ID, 7), DefaultAssetRemote(CHAIN_ID, 0)))
        )

        // The list doubles as the priority sort order, so it must not be reordered on the way in
        assertEquals(
            listOf(FullChainAssetId(CHAIN_ID, 7), FullChainAssetId(CHAIN_ID, 0)),
            repository().defaultAssetIds()
        )
    }

    @Test
    fun `an unreachable config has no opinion rather than hiding everything`() = runBlocking<Unit> {
        `when`(chainFetcher.getDefaultAssets()).thenThrow(RuntimeException("no network"))

        assertTrue(repository().defaultAssetIds().isEmpty())
    }


    @Test
    fun `a fetched list is reused, a failed one is retried`() = runBlocking<Unit> {
        `when`(chainFetcher.getDefaultAssets())
            .thenThrow(RuntimeException("no network"))
            .thenReturn(remote())

        val repository = repository()

        assertTrue(repository.defaultAssetIds().isEmpty())
        assertEquals(listOf(FullChainAssetId(CHAIN_ID, 0)), repository.defaultAssetIds())
        assertEquals(listOf(FullChainAssetId(CHAIN_ID, 0)), repository.defaultAssetIds())

        verify(chainFetcher, times(2)).getDefaultAssets()
    }
}
