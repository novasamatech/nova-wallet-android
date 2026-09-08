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
import org.mockito.Mock
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
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
    lateinit var preferences: Preferences

    private fun repository() = DefaultAssetsRepository(chainAssetDao, chainFetcher, preferences)

    private fun defaults() = DefaultAssetsRemote(defaultAssets = listOf(DefaultAssetRemote(CHAIN_ID, 0)))

    private fun alreadyApplied(applied: Boolean) {
        `when`(preferences.getBoolean(PREF_APPLIED, false)).thenReturn(applied)
    }

    @Test
    fun `fresh install shows only the default list`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenReturn(defaults())

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
    fun `a failed fetch applies nothing and stays retriable`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenThrow(RuntimeException("no network"))

        val repository = repository()

        // Callers retry this, so failing is the whole point - what matters is that nothing was
        // marked as applied, leaving the next attempt free to apply the real list.
        assertTrue(runCatching { repository.initialAssetEnabling() }.isFailure)
        verify(preferences, never()).putBoolean(anyString(), anyBoolean())
    }

    @Test
    fun `an empty config counts as broken rather than as hide everything`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenReturn(DefaultAssetsRemote(defaultAssets = emptyList()))

        val repository = repository()

        assertTrue(runCatching { repository.initialAssetEnabling() }.isFailure)
        verify(preferences, never()).putBoolean(anyString(), anyBoolean())
    }

    @Test
    fun `a retried attempt applies the list once it arrives`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets())
            .thenThrow(RuntimeException("no network"))
            .thenReturn(defaults())

        val repository = repository()
        runCatching { repository.initialAssetEnabling() }

        val enabling = repository.initialAssetEnabling()

        assertTrue(enabling.isEnabled(IN_DEFAULTS))
        assertFalse(enabling.isEnabled(NOT_IN_DEFAULTS))
        verify(preferences).putBoolean(PREF_APPLIED, true)
    }

    @Test
    fun `every sync service in one launch sees the same decision`() = runBlocking<Unit> {
        alreadyApplied(false)
        `when`(chainAssetDao.hasAnyAsset()).thenReturn(false)
        `when`(chainFetcher.getDefaultAssets()).thenReturn(defaults())

        val repository = repository()
        val first = repository.initialAssetEnabling()
        val second = repository.initialAssetEnabling()

        assertEquals(first, second)
        // The first service to sync writes assets, which would otherwise flip the answer
        // for the one running right after it.
        verify(chainFetcher, times(1)).getDefaultAssets()
    }

    @Test
    fun `the manage screen does not wait for a config it only decorates with`() = runBlocking<Unit> {
        `when`(chainFetcher.getDefaultAssets()).thenThrow(RuntimeException("no network"))

        // No retry loop here: a missing config costs a section header, not correctness
        assertTrue(repository().defaultAssetIds().isEmpty())
        verify(chainFetcher, times(1)).getDefaultAssets()
    }
}
