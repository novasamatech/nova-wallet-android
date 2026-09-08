package io.novafoundation.nova.runtime.multiNetwork.chain

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.FullAssetIdLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetsRemote
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

private const val PREF_APPLIED = "DEFAULT_ASSETS_APPLIED"

private const val CHAIN_ID = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"

private val IN_DEFAULTS = FullAssetIdLocal(CHAIN_ID, 0)
private val NOT_IN_DEFAULTS = FullAssetIdLocal(CHAIN_ID, 7)

@RunWith(MockitoJUnitRunner::class)
class DefaultAssetsRepositoryTest {

    @Mock
    lateinit var chainAssetDao: ChainAssetDao

    @Mock
    lateinit var chainFetcher: ChainFetcher

    @Mock
    lateinit var bundledDefaultAssets: BundledDefaultAssets

    @Mock
    lateinit var preferences: Preferences

    private fun repository() = DefaultAssetsRepository(chainAssetDao, chainFetcher, bundledDefaultAssets, preferences)

    private fun defaults() = DefaultAssetsRemote(defaultAssets = listOf(DefaultAssetRemote(CHAIN_ID, 0)))

    private fun bundledReturnsDefaults() {
        `when`(bundledDefaultAssets.read()).thenReturn(defaults())
    }

    private fun alreadyApplied(applied: Boolean) {
        `when`(preferences.getBoolean(PREF_APPLIED, false)).thenReturn(applied)
    }

    private suspend fun remoteReturnsDefaults() {
        `when`(chainFetcher.getDefaultAssets()).thenReturn(defaults())
    }

    @Test
    fun `fresh install shows only the default list`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        remoteReturnsDefaults()

        val enabling = repository().initialAssetEnabling()

        assertTrue(enabling.isEnabled(IN_DEFAULTS))
        assertFalse(enabling.isEnabled(NOT_IN_DEFAULTS))
        verify(preferences).putBoolean(PREF_APPLIED, true)
    }

    @Test
    fun `install that predates the feature keeps its list and hides assets arriving later`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(true)

        val enabling = repository().initialAssetEnabling()

        // Assets already stored locally never reach isEnabled - only genuinely new ones do,
        // and those must not add themselves to a list the user has already curated.
        assertFalse(enabling.isEnabled(IN_DEFAULTS))
        assertFalse(enabling.isEnabled(NOT_IN_DEFAULTS))
        verify(preferences).putBoolean(PREF_APPLIED, true)
        verify(chainFetcher, never()).getDefaultAssets()
    }

    @Test
    fun `unreachable default list falls back to the bundled one instead of showing every token`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenThrow(RuntimeException("no network"))
        bundledReturnsDefaults()

        val enabling = repository().initialAssetEnabling()

        assertTrue(enabling.isEnabled(IN_DEFAULTS))
        assertFalse(enabling.isEnabled(NOT_IN_DEFAULTS))
        verify(preferences).putBoolean(PREF_APPLIED, true)
    }

    @Test
    fun `an empty config is treated as broken rather than as hide everything`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenReturn(DefaultAssetsRemote(defaultAssets = emptyList()))
        bundledReturnsDefaults()

        val enabling = repository().initialAssetEnabling()

        assertTrue(enabling.isEnabled(IN_DEFAULTS))
        assertFalse(enabling.isEnabled(NOT_IN_DEFAULTS))
    }

    @Test
    fun `only a broken build leaves a wallet with every token`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenThrow(RuntimeException("no network"))
        `when`(bundledDefaultAssets.read()).thenThrow(RuntimeException("asset missing from the apk"))

        val enabling = repository().initialAssetEnabling()

        assertTrue(enabling.isEnabled(NOT_IN_DEFAULTS))
        // Nothing was applied, so the next launch gets another chance at the real list.
        verify(preferences, never()).putBoolean(anyString(), anyBoolean())
    }

    @Test
    fun `every sync service in one launch sees the same decision`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        remoteReturnsDefaults()

        val repository = repository()
        val first = repository.initialAssetEnabling()
        val second = repository.initialAssetEnabling()

        assertEquals(first, second)
        // The first service to sync writes assets, which would otherwise flip the answer
        // for the one running right after it.
        verify(chainFetcher, times(1)).getDefaultAssets()
    }
}
