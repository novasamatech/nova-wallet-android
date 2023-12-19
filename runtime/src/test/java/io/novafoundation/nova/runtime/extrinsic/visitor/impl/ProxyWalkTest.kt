package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.data.network.runtime.binding.bindMultiAddress
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.api.walkToList
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.proxy.ProxyNode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.MetadataFunction
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigInteger

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

    private val testModule = createTestModuleWithCall(moduleName = "Test", callName = "test")
    private val testInnerCall = GenericCall.Instance(
        module = testModule,
        function = testModule.call("test"),
        arguments = emptyMap()
    )

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
        val events = listOf(testEvent(), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = proxied,
            call = testInnerCall,
            events = events
        )

        val visit = extrinsicWalk.walkSingle(extrinsic)
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

        val visit = extrinsicWalk.walkSingle(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(events, visit.events)
    }

    @Test
    fun shouldVisitSucceededSingleProxyCall() = runBlocking {
        val innerProxyEvents = listOf(testEvent())
        val events = innerProxyEvents + listOf(proxyExecuted(success = true), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = proxy,
            call = proxyCall(
                real = proxied,
                innerCall = testInnerCall
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingle(extrinsic)
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

        val visit = extrinsicWalk.walkSingle(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    @Test
    fun shouldVisitSucceededMultipleProxyCalls() = runBlocking {
        val innerProxyEvents = listOf(testEvent())
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

        val visit = extrinsicWalk.walkSingle(extrinsic)
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

        val visit = extrinsicWalk.walkSingle(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(proxied, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerProxyEvents, visit.events)
    }

    private suspend fun ExtrinsicWalk.walkToList(extrinsicWithEvents: ExtrinsicWithEvents): List<ExtrinsicVisit> {
        return walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT)
    }

    private suspend fun ExtrinsicWalk.walkSingle(extrinsicWithEvents: ExtrinsicWithEvents): ExtrinsicVisit {
        val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT)
        assertEquals(1, visits.size)

        return visits.single()
    }

    private fun createExtrinsic(
        signer: AccountId,
        call: GenericCall.Instance,
        events: List<GenericEvent.Instance>
    ) = ExtrinsicWithEvents(
        extrinsic = Extrinsic.DecodedInstance(
            signature = Extrinsic.Signature(
                accountIdentifier = bindMultiAddress(MultiAddress.Id(signer)),
                signature = null,
                signedExtras = emptyMap()
            ),
            call = call,
        ),
        extrinsicHash = "0x",
        events = events
    )

    private fun createTestModuleWithCall(
        moduleName: String,
        callName: String
    ): Module {
        return Module(
            name = moduleName,
            storage = null,
            calls = mapOf(
                callName to MetadataFunction(
                    name = callName,
                    arguments = emptyList(),
                    documentation = emptyList(),
                    index = 0 to 0
                )
            ),
            events = emptyMap(),
            constants = emptyMap(),
            errors = emptyMap(),
            index = BigInteger.ZERO
        )
    }

    private fun extrinsicSuccess(): GenericEvent.Instance {
        return mockEvent("System", "ExtrinsicSuccess")
    }

    private fun extrinsicFailed(): GenericEvent.Instance {
        return mockEvent("System", "ExtrinsicFailed")
    }

    private fun proxyExecuted(success: Boolean): GenericEvent.Instance {
        val outcomeVariant = if (success) "Ok" else "Err"
        val outcome = DictEnum.Entry(name = outcomeVariant, value = null)

        return GenericEvent.Instance(proxyModule, proxyExecutedType, arguments = listOf(outcome))
    }


    private fun testEvent(): GenericEvent.Instance {
        return mockEvent(testModule.name, "test")
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

    private fun mockEvent(moduleName: String, eventName: String, arguments: List<Any?> = emptyList()): GenericEvent.Instance {
        val module = Mockito.mock(Module::class.java)
        whenever(module.name).thenReturn(moduleName)

        val event = Mockito.mock(Event::class.java)
        whenever(event.name).thenReturn(eventName)

        return GenericEvent.Instance(
            module = module,
            event = event,
            arguments = arguments
        )
    }

    private fun mockCall(moduleName: String, callName: String, arguments: Map<String, Any?> = emptyMap()): GenericCall.Instance {
        val module = Mockito.mock(Module::class.java)
        whenever(module.name).thenReturn(moduleName)

        val function = Mockito.mock(MetadataFunction::class.java)
        whenever(function.name).thenReturn(callName)

        return GenericCall.Instance(
            module = module,
            function = function,
            arguments = arguments
        )
    }
}
