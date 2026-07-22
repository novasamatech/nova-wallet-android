package io.novafoundation.nova.feature_staking_impl.data.notices.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.data.notices.cache.StakingNoticesDiskCache
import io.novafoundation.nova.feature_staking_impl.data.notices.network.StakingNoticesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.BDDMockito.times
import org.mockito.Mockito
import org.mockito.Mockito.verify

// Debounce window is 5 minutes; pass a time well beyond it to allow the first sync through.
private const val AFTER_DEBOUNCE = 5L * 60 * 1000 + 1   // 300_001 ms

private const val SAMPLE_JSON = """
[
  {
    "chainId": "f3c7ad88f6a80f366c4be216691411ef0622e8b809b1046ea297ef106058d4eb",
    "severity": "critical",
    "shortText": "Short",
    "longText":  "Long"
  }
]
"""

@OptIn(ExperimentalCoroutinesApi::class)
class RealStakingNoticesRepositoryTest {

    private lateinit var api: StakingNoticesApi
    private lateinit var diskCache: StakingNoticesDiskCache
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    @Before
    fun setUp() {
        api = Mockito.mock(StakingNoticesApi::class.java)
        diskCache = Mockito.mock(StakingNoticesDiskCache::class.java)
        sharedPreferences = Mockito.mock(SharedPreferences::class.java)
    }

    private fun TestScope.newRepo(now: Long = 0L) = RealStakingNoticesRepository(
        api = api,
        diskCache = diskCache,
        gson = gson,
        sharedPreferences = sharedPreferences,
        sharedPreferencesLanguageKey = "lang",
        remoteUrl = "http://x",
        timeProvider = { now },
        externalScope = this,
    )

    @Test
    fun `cold init with no disk cache emits empty map`() = runTest {
        given(diskCache.read()).willReturn(null)
        val repo = newRepo()
        advanceUntilIdle()
        val map = repo.observeStakingNotices().firstOrNull()
        assertNotNull(map)
        assertTrue(map!!.isEmpty())
    }

    @Test
    fun `cold init with disk cache decodes and emits without network call`() = runTest {
        given(diskCache.read()).willReturn(SAMPLE_JSON)
        val repo = newRepo()
        advanceUntilIdle()
        val map = repo.observeStakingNotices().firstOrNull()
        assertEquals(1, map?.size)
        verify(api, never()).fetchStakingNotices(Mockito.anyString())
    }

    @Test
    fun `syncStakingNotices fetches, writes disk, and emits new map`() = runTest {
        given(diskCache.read()).willReturn(null)
        given(api.fetchStakingNotices(Mockito.anyString())).willReturn(SAMPLE_JSON)
        // timeProvider() must exceed DEBOUNCE_MILLIS so the first sync is not short-circuited
        val repo = newRepo(now = AFTER_DEBOUNCE)
        advanceUntilIdle()
        // consume the initial empty emission
        repo.observeStakingNotices().first()
        repo.syncStakingNotices()
        val map = repo.observeStakingNotices().first()
        assertEquals(1, map.size)
        verify(diskCache).write(SAMPLE_JSON)
    }

    @Test
    fun `second sync within debounce window does not call api`() = runTest {
        given(diskCache.read()).willReturn(null)
        given(api.fetchStakingNotices(Mockito.anyString())).willReturn(SAMPLE_JSON)
        // timeProvider() always returns AFTER_DEBOUNCE, so first sync proceeds;
        // after first sync lastSuccessfulSyncAt = AFTER_DEBOUNCE, second sync: AFTER_DEBOUNCE - AFTER_DEBOUNCE = 0 < DEBOUNCE → blocked
        val repo = newRepo(now = AFTER_DEBOUNCE)
        advanceUntilIdle()
        repo.syncStakingNotices()
        repo.syncStakingNotices()
        verify(api, times(1)).fetchStakingNotices(Mockito.anyString())
    }

    @Test
    fun `api failure leaves state unchanged`() = runTest {
        given(diskCache.read()).willReturn(SAMPLE_JSON)
        given(api.fetchStakingNotices(Mockito.anyString())).willAnswer { throw RuntimeException("net fail") }
        val repo = newRepo(now = AFTER_DEBOUNCE)
        advanceUntilIdle()
        val initial = repo.observeStakingNotices().firstOrNull()
        repo.syncStakingNotices()
        val after = repo.observeStakingNotices().firstOrNull()
        assertEquals(initial, after)
    }

    @Test
    fun `decode uses SharedPreferences language code, picks ru from locale map`() = runTest {
        val localeMapJson = """
            [
              {
                "chainId": "f3c7ad88f6a80f366c4be216691411ef0622e8b809b1046ea297ef106058d4eb",
                "severity": "info",
                "shortText": { "en": "EN", "ru": "RU short" },
                "longText":  { "en": "EN long", "ru": "RU long" }
              }
            ]
        """.trimIndent()
        given(sharedPreferences.getString(eq("lang"), Mockito.isNull())).willReturn("ru")
        given(diskCache.read()).willReturn(localeMapJson)
        val repo = newRepo()
        advanceUntilIdle()
        val map = repo.observeStakingNotices().firstOrNull()
        assertEquals("RU short", map?.values?.first()?.shortText)
    }

    @Test
    fun `Android pt language code is mapped to JSON pt-PT key`() = runTest {
        val localeMapJson = """
            [
              {
                "chainId": "f3c7ad88f6a80f366c4be216691411ef0622e8b809b1046ea297ef106058d4eb",
                "severity": "info",
                "shortText": { "en": "EN", "pt-PT": "PT short" },
                "longText":  { "en": "EN long", "pt-PT": "PT long" }
              }
            ]
        """.trimIndent()
        given(sharedPreferences.getString(eq("lang"), Mockito.isNull())).willReturn("pt")
        given(diskCache.read()).willReturn(localeMapJson)
        val repo = newRepo()
        advanceUntilIdle()
        val map = repo.observeStakingNotices().firstOrNull()
        assertEquals("PT short", map?.values?.first()?.shortText)
    }

    @Test
    fun `garbage json on disk yields no emission and does not crash`() = runTest {
        given(diskCache.read()).willReturn("not valid json")
        val repo = newRepo()
        advanceUntilIdle()
        // bad cache: decode() returns null so decodeFromDiskAndEmit() returns without emitting
        // replayCache will be empty — confirm this via SharedFlow's replay cache
        val sharedFlow = repo.observeStakingNotices() as SharedFlow<*>
        assertTrue("no emission expected on garbage json", sharedFlow.replayCache.isEmpty())
    }
}
