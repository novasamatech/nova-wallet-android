package io.novafoundation.nova.runtime.multiNetwork.runtime

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.md5
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.TypesFetcher
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.eq
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap


private const val TEST_TYPES = "Stub"

@Ignore("Flaky tests due to concurrency issues")
@RunWith(MockitoJUnitRunner::class)
class RuntimeSyncServiceTest {

    private val testChain by lazy {
        Mocks.chain(id = "1")
    }

    @Mock
    private lateinit var socket: SocketService

    @Mock
    private lateinit var testConnection: ChainConnection

    @Mock
    private lateinit var typesFetcher: TypesFetcher

    @Mock
    private lateinit var chainDao: ChainDao

    @Mock
    private lateinit var runtimeFilesCache: RuntimeFilesCache

    @Mock
    private lateinit var runtimeMetadataFetcher: RuntimeMetadataFetcher

    @Mock
    private lateinit var cacheMigrator: RuntimeCacheMigrator

    private lateinit var syncDispatcher: SyncChainSyncDispatcher

    private lateinit var service: RuntimeSyncService

    private lateinit var syncResultFlow: Flow<SyncResult>

    @JvmField
    @Rule
    val globalTimeout: Timeout = Timeout.seconds(10)

    @Before
    fun setup() = runBlocking {
        whenever(testConnection.socketService).thenReturn(socket)
        whenever(socket.jsonMapper).thenReturn(Gson())
        whenever(typesFetcher.getTypes(any())).thenReturn(TEST_TYPES)

        whenever(runtimeMetadataFetcher.fetchRawMetadata(any(), any())).thenReturn(RawRuntimeMetadata(metadataContent = byteArrayOf(0), isOpaque = false))

        whenever(cacheMigrator.needsMetadataFetch(anyInt())).thenReturn(false)

        syncDispatcher = Mockito.spy(SyncChainSyncDispatcher())

        service = RuntimeSyncService(typesFetcher, runtimeFilesCache, chainDao, runtimeMetadataFetcher, cacheMigrator, syncDispatcher)

        syncResultFlow = service.syncResultFlow(testChain.id)
            .shareIn(GlobalScope, started = SharingStarted.Eagerly, replay = 1)
    }

    @Test
    fun `should not start syncing new chain`() {
        service.registerChain(chain = testChain, connection = testConnection)

        assertNoSyncLaunched()
    }

    @Test
    fun `should start syncing on runtime version apply`() {
        service.registerChain(chain = testChain, connection = testConnection)

        service.applyRuntimeVersion(testChain.id)

        assertSyncLaunchedOnce()
        assertSyncCancelledTimes(1)
    }

    @Test
    fun `should not start syncing the same chain`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            assertSyncLaunchedOnce()

            service.registerChain(chain = testChain, connection = testConnection)

            // No new launches
            assertSyncLaunchedOnce()

            assertFalse(service.isSyncing(testChain.id))
        }
    }

    @Test
    fun `should sync modified chain`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            val newChain = Mockito.mock(Chain::class.java)
            whenever(newChain.id).thenAnswer { testChain.id }
            whenever(newChain.types).thenReturn(Chain.Types(url = "Changed", overridesCommon = false))

            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            assertSyncLaunchedOnce()

            chainDaoReturnsSameRuntimeInfo()

            service.registerChain(chain = newChain, connection = testConnection)

            assertSyncLaunchedTimes(2)

            val syncResult = syncResultFlow.first()

            assertNull("Metadata should not sync", syncResult.metadataHash)
        }
    }

    @Test
    fun `should sync types when url is not null`() {
        runBlocking {
            chainDaoReturnsSameRuntimeInfo()

            whenever(testChain.types).thenReturn(Chain.Types("Stub", overridesCommon = false))

            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val result = syncResultFlow.first()

            assertNotNull(result.typesHash)
        }
    }

    @Test
    fun `should not sync types when url is null`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val result = syncResultFlow.first()

            assertNull(result.typesHash)
        }
    }

    @Test
    fun `should cancel syncing when chain is unregistered`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            assertSyncCancelledTimes(1)
            assertSyncLaunchedOnce()

            service.unregisterChain(testChain.id)

            assertSyncCancelledTimes(2)
        }
    }

    @Test
    fun `should broadcast sync result`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val result = syncResultFlow.first()

            assertEquals(TEST_TYPES.md5(), result.typesHash)
        }
    }

    @Test
    fun `should sync bigger version of metadata`() {
        runBlocking {
            chainDaoReturnsBiggerRuntimeVersion()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val syncResult = syncResultFlow.first()

            assertNotNull(syncResult.metadataHash)
        }
    }

    @Test
    fun `should sync lower version of metadata`() {
        runBlocking {
            chainDaoReturnsLowerRuntimeInfo()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val syncResult = syncResultFlow.first()

            assertNotNull(syncResult.metadataHash)
        }
    }

    @Test
    fun `should always sync chain info when cache is not found`() {
        runBlocking {
            chainDaoReturnsSameRuntimeInfo()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)

            assertNoSyncLaunched()

            service.cacheNotFound(testChain.id)

            assertSyncLaunchedOnce()

            val syncResult = syncResultFlow.first()
            assertNotNull(syncResult.metadataHash)
            assertNotNull(syncResult.typesHash)
        }
    }

    @Test
    fun `should not sync the same version of metadata`() {
        runBlocking {
            chainDaoReturnsSameRuntimeInfo()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val syncResult = syncResultFlow.first()

            assertNull(syncResult.metadataHash)
        }
    }

    @Test
    fun `should sync the same version of metadata when local migration required`() {
        runBlocking {
            chainDaoReturnsSameRuntimeInfo()
            requiresLocalMigration()

            whenever(testChain.types).thenReturn(Chain.Types("testUrl", overridesCommon = false))
            service.registerChain(chain = testChain, connection = testConnection)
            service.applyRuntimeVersion(testChain.id)

            val syncResult = syncResultFlow.first()

            assertNotNull(syncResult.metadataHash)
        }
    }

    private suspend fun chainDaoReturnsBiggerRuntimeVersion() {
        chainDaoReturnsRuntimeInfo(remoteVersion = 1, syncedVersion = 0)
    }

    private suspend fun chainDaoReturnsSameRuntimeInfo() {
        chainDaoReturnsRuntimeInfo(remoteVersion = 1, syncedVersion = 1)
    }

    private suspend fun chainDaoReturnsLowerRuntimeInfo() {
        chainDaoReturnsRuntimeInfo(remoteVersion = 0, syncedVersion = 1)
    }

    private suspend fun chainDaoReturnsRuntimeInfo(remoteVersion: Int, syncedVersion: Int) {
        whenever(chainDao.runtimeInfo(any())).thenReturn(ChainRuntimeInfoLocal("1", syncedVersion, remoteVersion, null, localMigratorVersion = 1))
    }

    private suspend fun RuntimeSyncService.latestSyncResult(chainId: String) = syncResultFlow(chainId).first()

    private suspend fun requiresLocalMigration() {
        whenever(cacheMigrator.needsMetadataFetch(anyInt())).thenReturn(true)
    }

    private fun socketAnswersRequest(request: RuntimeRequest, response: Any?) {
        whenever(socket.executeRequest(eq(request), deliveryType = any(), callback = any())).thenAnswer {
            (it.arguments[2] as SocketService.ResponseListener<RpcResponse>).onNext(RpcResponse(jsonrpc = "2.0", response, id = 1, error = null))

            object : SocketService.Cancellable {
                override fun cancel() {
                    // pass
                }
            }
        }
    }

    private fun assertNoSyncLaunched() {
        verify(syncDispatcher, never()).launchSync(anyString(), any())
    }

    private fun assertSyncLaunchedOnce() {
        verify(syncDispatcher, times(1)).launchSync(anyString(), any())
    }

    private fun assertSyncCancelledTimes(times: Int) {
        verify(syncDispatcher, times(times)).cancelExistingSync(anyString())
    }

    private fun assertSyncLaunchedTimes(times: Int) {
        verify(syncDispatcher, times(times)).launchSync(anyString(), any())
    }

    class SyncChainSyncDispatcher() : ChainSyncDispatcher {

        private val syncingChains = Collections.newSetFromMap(ConcurrentHashMap<ChainId, Boolean>())

        override fun isSyncing(chainId: String): Boolean {
            return syncingChains.contains(chainId)
        }

        override fun syncFinished(chainId: String) {
            syncingChains.remove(chainId)
        }

        override fun cancelExistingSync(chainId: String) {
            syncingChains.remove(chainId)
        }

        override fun launchSync(chainId: String, action: suspend () -> Unit) = runBlocking {
            syncingChains.add(chainId)

            action()
        }
    }
}
