package io.novafoundation.nova.common.data.config

import com.google.gson.Gson
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.data.storage.Preferences
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class GlobalConfigDataSourceTest {

    private val preferences = mock(Preferences::class.java)

    private val logger = mock(Logger::class.java)

    private val gson = Gson()

    private val stored = mutableMapOf<String, String>()

    private val mirrorHost = BuildConfig.NOVA_UTILS_MIRROR_URL.toHttpUrl().host

    private val changeNowTaoUrl = "https://changenow.io/exchange?from=assethub&to=tao&link_id=59409968ba3831"

    private val onlineApi = object : GlobalConfigApi {
        override suspend fun getGlobalConfig() = GlobalConfigRemote("multisigs", "proxy", "staking", "https://infra.test/")
    }

    private val offlineApi = object : GlobalConfigApi {
        override suspend fun getGlobalConfig(): GlobalConfigRemote = throw IOException("Offline")
    }

    private val onlineTaoApi = object : GlobalConfigApi {
        override suspend fun getGlobalConfig() = GlobalConfigRemote(
            multisigsApiUrl = "multisigs",
            proxyApiUrl = "proxy",
            multiStakingApiUrl = "staking",
            infraUrl = "https://infra.test/",
            changeNowTaoUrl = changeNowTaoUrl
        )
    }

    private fun prepareStorage() {
        doAnswer { call -> stored[call.getArgument(0)] = call.getArgument(1); null }.`when`(preferences).putString(anyString(), anyString())
        `when`(preferences.getString(anyString())).thenAnswer { call -> stored[call.getArgument(0)] }
    }

    @Test
    fun `swap pair and TAO funding URL survive decoding and offline restore together`() = runBlocking {
        prepareStorage()
        val chainId = "a".repeat(64)
        val json = """
            {
                "multisigsApiUrl": "multisigs",
                "proxyApiUrl": "proxy",
                "multiStakingApiUrl": "staking",
                "infraUrl": "https://infra.test/",
                "changeNowTaoUrl": "$changeNowTaoUrl",
                "defaultSwapPair": {
                    "assetIn": {"chainId": "$chainId", "assetId": 0},
                    "assetOut": {"chainId": "$chainId", "assetId": 2}
                }
            }
        """.trimIndent()
        val api = object : GlobalConfigApi {
            override suspend fun getGlobalConfig(): GlobalConfigRemote = gson.fromJson(json, GlobalConfigRemote::class.java)
        }
        val online = RealGlobalConfigDataSource(api, preferences, gson, logger)
        val loaded = online.getGlobalConfig()
        assertEquals(changeNowTaoUrl, loaded.changeNowTaoUrl)
        assertEquals(0, loaded.defaultSwapPair!!.assetIn.assetId)
        assertEquals(2, loaded.defaultSwapPair!!.assetOut.assetId)

        val offline = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        val restored = offline.getGlobalConfig()
        assertEquals(changeNowTaoUrl, restored.changeNowTaoUrl)
        assertEquals(loaded.defaultSwapPair, restored.defaultSwapPair)
    }

    @Test
    fun `remote config decodes ChangeNOW TAO URL`() {
        val json = """
            {
                "multisigsApiUrl": "multisigs",
                "proxyApiUrl": "proxy",
                "multiStakingApiUrl": "staking",
                "infraUrl": "https://infra.test/",
                "changeNowTaoUrl": "$changeNowTaoUrl"
            }
        """.trimIndent()

        val remote = gson.fromJson(json, GlobalConfigRemote::class.java)

        assertEquals(changeNowTaoUrl, remote.changeNowTaoUrl)
    }

    @Test
    fun `remote config without ChangeNOW TAO URL decodes null`() {
        val json = """
            {
                "multisigsApiUrl": "multisigs",
                "proxyApiUrl": "proxy",
                "multiStakingApiUrl": "staking",
                "infraUrl": "https://infra.test/"
            }
        """.trimIndent()

        val remote = gson.fromJson(json, GlobalConfigRemote::class.java)

        assertNull(remote.changeNowTaoUrl)
    }

    @Test
    fun `ChangeNOW TAO URL survives a new owner with no network`() = runBlocking {
        prepareStorage()
        val online = RealGlobalConfigDataSource(onlineTaoApi, preferences, gson, logger)
        assertEquals(changeNowTaoUrl, online.getGlobalConfig().changeNowTaoUrl)

        val restarted = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        assertEquals(changeNowTaoUrl, restarted.getGlobalConfig().changeNowTaoUrl)
    }

    @Test
    fun `last successful config survives a new owner with no network`() = runBlocking {
        prepareStorage()
        val online = RealGlobalConfigDataSource(onlineApi, preferences, gson, logger)
        assertEquals("https://infra.test/", online.getGlobalConfig().infraUrl)
        val restarted = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        assertEquals("https://infra.test/", restarted.getGlobalConfig().infraUrl)
        assertNull(restarted.getGlobalConfig().changeNowTaoUrl)
    }

    @Test
    fun `infraUrl on another host than the nova-utils mirror is logged`() = runBlocking<Unit> {
        prepareStorage()
        val source = RealGlobalConfigDataSource(onlineApi, preferences, gson, logger)

        source.getGlobalConfig()

        verify(logger).log("[GlobalConfig] infraUrl host infra.test differs from the nova-utils mirror host $mirrorHost")
    }

    @Test
    fun `infraUrl on the host of the nova-utils mirror or a missing one is not logged`() = runBlocking<Unit> {
        prepareStorage()
        val infraUrls = listOf("https://$mirrorHost/", "https://$mirrorHost:443/v1/other", null)

        infraUrls.forEach { infraUrl ->
            val api = object : GlobalConfigApi {
                override suspend fun getGlobalConfig() = GlobalConfigRemote("multisigs", "proxy", "staking", infraUrl)
            }
            RealGlobalConfigDataSource(api, preferences, gson, logger).getGlobalConfig()
        }

        verify(logger, never()).log(anyString())
    }

    @Test
    fun `offline without saved config preserves failure`() = runBlocking {
        prepareStorage()
        val source = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        assertTrue(runCatching { source.getGlobalConfig() }.exceptionOrNull() is IOException)
    }

    @Test
    fun `incompatible saved configuration preserves the network failure`() = runBlocking {
        prepareStorage()
        val online = RealGlobalConfigDataSource(onlineApi, preferences, gson, logger)
        online.getGlobalConfig()
        stored[stored.keys.single()] = "{}"
        val restarted = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        assertTrue(runCatching { restarted.getGlobalConfig() }.exceptionOrNull() is IOException)
    }

    @Test
    fun `malformed network configuration cannot replace the last usable snapshot`() = runBlocking {
        prepareStorage()
        val online = RealGlobalConfigDataSource(onlineApi, preferences, gson, logger)
        online.getGlobalConfig()
        val saved = stored.toMap()
        val malformedApi = object : GlobalConfigApi {
            override suspend fun getGlobalConfig(): GlobalConfigRemote = gson.fromJson("{}", GlobalConfigRemote::class.java)
        }
        val source = RealGlobalConfigDataSource(malformedApi, preferences, gson, logger)

        assertEquals("https://infra.test/", source.getGlobalConfig().infraUrl)
        assertEquals(saved, stored)
        val restarted = RealGlobalConfigDataSource(offlineApi, preferences, gson, logger)
        assertEquals("https://infra.test/", restarted.getGlobalConfig().infraUrl)
    }

    @Test
    fun `cancellation never falls back to saved config`() = runBlocking {
        prepareStorage()
        val online = RealGlobalConfigDataSource(onlineApi, preferences, gson, logger)
        online.getGlobalConfig()
        val cancelledApi = object : GlobalConfigApi {
            override suspend fun getGlobalConfig(): GlobalConfigRemote = throw CancellationException("Cancelled")
        }
        val source = RealGlobalConfigDataSource(cancelledApi, preferences, gson, logger)
        assertTrue(runCatching { source.getGlobalConfig() }.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `default swap pair is optional and validated independently of other global settings`() = runBlocking<Unit> {
        prepareStorage()
        val chainId = "a".repeat(64)
        val pair = SwapPairRemote(SwapPairRemote.Asset(chainId, 0), SwapPairRemote.Asset(chainId, 2))
        val api = object : GlobalConfigApi {
            override suspend fun getGlobalConfig() = GlobalConfigRemote("m", "p", "s", "https://infra.test/", pair)
        }
        val config = RealGlobalConfigDataSource(api, preferences, gson, logger).getGlobalConfig()
        assertEquals(0, config.defaultSwapPair!!.assetIn.assetId)
        assertEquals(2, config.defaultSwapPair!!.assetOut.assetId)
        val oldApi = object : GlobalConfigApi {
            override suspend fun getGlobalConfig() = GlobalConfigRemote("m", "p", "s", "https://infra.test/")
        }
        org.junit.Assert.assertNull(RealGlobalConfigDataSource(oldApi, preferences, gson, logger).getGlobalConfig().defaultSwapPair)
    }

    @Test
    fun `malformed pair does not discard valid infrastructure configuration`() = runBlocking<Unit> {
        prepareStorage()
        val badPair = SwapPairRemote(SwapPairRemote.Asset("invalid", null), SwapPairRemote.Asset("invalid", 2))
        val api = object : GlobalConfigApi {
            override suspend fun getGlobalConfig() = GlobalConfigRemote("m", "p", "s", "https://infra.test/", badPair)
        }
        val config = RealGlobalConfigDataSource(api, preferences, gson, logger).getGlobalConfig()
        assertEquals("https://infra.test/", config.infraUrl)
        org.junit.Assert.assertNull(config.defaultSwapPair)
    }
}
