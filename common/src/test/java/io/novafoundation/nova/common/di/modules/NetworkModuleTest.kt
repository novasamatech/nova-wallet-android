package io.novafoundation.nova.common.di.modules

import android.content.Context
import io.novafoundation.nova.common.data.network.DebugHttpLoggingInterceptor
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorInterceptor
import io.novafoundation.nova.common.data.network.TimeHeaderInterceptor
import io.novafoundation.nova.common.data.storage.DebugSettings
import io.novafoundation.nova.common.data.storage.Preferences
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class NetworkModuleTest {

    private val cacheDirectory: File = Files.createTempDirectory("network-module-test").toFile()

    private val context = mock(Context::class.java)

    private val preferences = mock(Preferences::class.java)

    private val logger = mock(Logger::class.java)

    private val debugSettings = DebugSettings(preferences)

    private val loggingInterceptor = DebugHttpLoggingInterceptor(debugSettings)

    private val module = NetworkModule()

    @After
    fun tearDown() {
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun `shared client reads nova-utils through the mirror, added last and once`() {
        `when`(context.cacheDir).thenReturn(cacheDirectory)
        val novaUtilsMirror = module.provideNovaUtilsMirror(logger)

        val client = module.provideOkHttpClient(context, novaUtilsMirror, loggingInterceptor)

        val interceptors = client.interceptors
        val mirrorInterceptor = interceptors.last()
        val earlierInterceptors = interceptors.dropLast(1)
        assertTrue(mirrorInterceptor is NovaUtilsMirrorInterceptor)
        assertEquals(0, earlierInterceptors.count { it is NovaUtilsMirrorInterceptor })
        assertEquals("the header timeouts must be applied before the mirror reads them", 1, earlierInterceptors.count { it is TimeHeaderInterceptor })
    }
}
