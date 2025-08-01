package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.data.network.runtime.binding.bindMultiAddress
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.RealExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.proxy.ProxyNode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ProxyWalkTest {

    @Mock
    private lateinit var runtimeProvider: RuntimeProvider

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    @Mock
    private lateinit var runtime: RuntimeSnapshot

    @Mock
    private lateinit var metadata: RuntimeMetadata

    @Mock
    private lateinit var proxyModule: Module

    private lateinit var extrinsicWalk: ExtrinsicWalk

    private val proxyExecutedType = Event("ProxyExecuted", index = 0 to 0, documentation = emptyList(), arguments = emptyList())

    private val proxy = byteArrayOf(0x00)
    private val proxied = byteArrayOf(0x01)

    private val testModuleMocker = TestModuleMocker()
    private val testEvent = testModuleMocker.testEvent
    private val testInnerCall = testModuleMocker.testInnerCall

    @Before
    fun setup() = runBlocking {
        whenever(proxyModule.events).thenReturn(mapOf("ProxyExecuted" to proxyExecutedType))
        whenever(metadata.modules).thenReturn(mapOf("Proxy" to proxyModule))
        whenever(runtime.metadata).thenReturn(metadata)
        whenever(runtimeProvider.get()).thenReturn(runtime)
        whenever(chainRegistry.getRuntimeProvider(any())).thenReturn(runtimeProvider)

        extrinsicWalk = RealExtrinsicWalk(chainRegistry, knownNodes = listOf(ProxyNode()))
    }

    @Test
    fun shouldVisitSucceededSimpleCall() = runBlocking {
        val events = listOf(testEvent, extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = proxied,
            call = testInnerCall,
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(true, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(events, visit.events)
    }

    @Test
    fun shouldVisitFailedSimpleCall() = runBlocking {
        val events = listOf(extrinsicFailed())

        val extrinsic = createExtrinsic(
            signer = proxied,
            call = testInnerCall,
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(events, visit.events)
    }

    @Test
    fun shouldVisitSucceededSingleProxyCall() = runBlocking {
        val innerProxyEvents = listOf(testEvent)
        val events = innerProxyEvents + listOf(proxyExecuted(success = true), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = proxy,
            call = proxyCall(
                real = proxied,
                innerCall = testInnerCall
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(true, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    @Test
    fun shouldVisitFailedSingleProxyCall() = runBlocking {
        val innerProxyEvents = emptyList<GenericEvent.Instance>()
        val events = listOf(proxyExecuted(success = false), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = proxy,
            call = proxyCall(
                real = proxied,
                innerCall = testInnerCall
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    @Test
    fun shouldVisitSucceededMultipleProxyCalls() = runBlocking {
        val innerProxyEvents = listOf(testEvent)
        val events = innerProxyEvents + listOf(proxyExecuted(success = true), proxyExecuted(success = true), proxyExecuted(success = true), extrinsicSuccess())

        val proxy1 = byteArrayOf(0x00)
        val proxy2 = byteArrayOf(0x01)
        val proxy3 = byteArrayOf(0x02)
        val proxied = byteArrayOf(0x10)

        val extrinsic = createExtrinsic(
            signer = proxy1,
            call = proxyCall(
                real = proxy2,
                innerCall = proxyCall(
                    real = proxy3,
                    innerCall = proxyCall(
                        real = proxied,
                        innerCall = testInnerCall
                    )
                )
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(true, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    @Test
    fun shouldVisitFailedMultipleProxyCalls() = runBlocking {
        val innerProxyEvents = emptyList<GenericCall.Instance>()
        val events = listOf(proxyExecuted(success = false), proxyExecuted(success = true), extrinsicSuccess()) // only outer-most proxy succeeded

        val proxy1 = byteArrayOf(0x00)
        val proxy2 = byteArrayOf(0x01)
        val proxy3 = byteArrayOf(0x02)
        val proxied = byteArrayOf(0x10)

        val extrinsic = createExtrinsic(
            signer = proxy1,
            call = proxyCall(
                real = proxy2,
                innerCall = proxyCall(
                    real = proxy3,
                    innerCall = proxyCall(
                        real = proxied,
                        innerCall = testInnerCall
                    )
                )
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    private fun proxyExecuted(success: Boolean): GenericEvent.Instance {
        val outcomeVariant = if (success) "Ok" else "Err"
        val outcome = DictEnum.Entry(name = outcomeVariant, value = null)

        return GenericEvent.Instance(proxyModule, proxyExecutedType, arguments = listOf(outcome))
    }

    private fun proxyCall(real: AccountId, innerCall: GenericCall.Instance): GenericCall.Instance {
        return mockCall(
            moduleName = "Proxy",
            callName = "proxy",
            arguments = mapOf(
                "real" to bindMultiAddress(MultiAddress.Id(real)),
                "call" to innerCall,
                // other args are not relevant
            )
        )
    }
}
