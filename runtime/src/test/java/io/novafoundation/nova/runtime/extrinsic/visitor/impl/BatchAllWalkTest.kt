package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.batch.BatchAllNode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
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
internal class BatchAllWalkTest {

    @Mock
    private lateinit var runtimeProvider: RuntimeProvider

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    @Mock
    private lateinit var runtime: RuntimeSnapshot

    @Mock
    private lateinit var metadata: RuntimeMetadata

    @Mock
    private lateinit var utilityModule: Module

    private lateinit var extrinsicWalk: ExtrinsicWalk

    private val itemCompletedType = Event("ItemCompleted", index = 0 to 0, documentation = emptyList(), arguments = emptyList())
    private val batchCompletedType = Event("BatchCompleted", index = 0 to 1, documentation = emptyList(), arguments = emptyList())
    private val itemFailedType = Event("ItemFailed", index = 0 to 2, documentation = emptyList(), arguments = emptyList())

    private val signer = byteArrayOf(0x00)

    private val testModuleMocker = TestModuleMocker()
    private val testEvent = testModuleMocker.testEvent
    private val testInnerCall = testModuleMocker.testInnerCall

    @Before
    fun setup() = runBlocking {
        whenever(utilityModule.events).thenReturn(
            mapOf(
                batchCompletedType.name to batchCompletedType,
                itemCompletedType.name to itemCompletedType,
                itemFailedType.name to itemFailedType
            )
        )
        whenever(metadata.modules).thenReturn(mapOf("Utility" to utilityModule))
        whenever(runtime.metadata).thenReturn(metadata)
        whenever(runtimeProvider.get()).thenReturn(runtime)
        whenever(chainRegistry.getRuntimeProvider(any())).thenReturn(runtimeProvider)

        extrinsicWalk = RealExtrinsicWalk(chainRegistry, knownNodes = listOf(BatchAllNode()))
    }

    @Test
    fun shouldVisitSucceededSingleBatchedCall() = runBlocking {
        val innerBatchEvents = listOf(testEvent)
        val events = innerBatchEvents + listOf(itemCompleted(), batchCompleted(), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            call = batchCall(testInnerCall),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(true, visit.success)
        assertArrayEquals(signer, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerBatchEvents, visit.events)
    }

    @Test
    fun shouldVisitFailedSingleBatchedCall() = runBlocking {
        val events = listOf(extrinsicFailed())

        val extrinsic = createExtrinsic(
            call = batchCall(testInnerCall),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(signer, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(true, visit.events.isEmpty())
    }

    @Test
    fun shouldVisitSucceededMultipleBatchedCalls() = runBlocking {
        val innerBatchEvents = listOf(testEvent)
        val events = buildList {
            addAll(innerBatchEvents)
            add(itemCompleted())

            addAll(innerBatchEvents)
            add(itemCompleted())

            add(batchCompleted())
            add(extrinsicSuccess())
        }

        val extrinsic = createExtrinsic(
            call = batchCall(testInnerCall, testInnerCall),
            events = events
        )

        val visits = extrinsicWalk.walkMultipleIgnoringBranches(extrinsic, expectedSize = 2)
        visits.forEach { visit ->
            assertEquals(true, visit.success)
            assertArrayEquals(signer, visit.origin)
            assertEquals(testInnerCall, visit.call)
            assertEquals(innerBatchEvents, visit.events)
        }
    }

    @Test
    fun shouldVisitFailedMultipleBatchedCalls() = runBlocking {
        val events = listOf(extrinsicFailed())

        val extrinsic = createExtrinsic(
            call = batchCall(testInnerCall, testInnerCall),
            events = events
        )

        val visits = extrinsicWalk.walkMultipleIgnoringBranches(extrinsic, expectedSize = 2)

        visits.forEach { visit ->
            assertEquals(false, visit.success)
            assertArrayEquals(signer, visit.origin)
            assertEquals(testInnerCall, visit.call)
            assertEquals(true, visit.events.isEmpty())
        }
    }

    @Test
    fun shouldVisitNestedBatches() = runBlocking {
        val innerBatchEvents = listOf(testEvent)
        val events = buildList {
            // first level batch starts
            addAll(innerBatchEvents)
            add(itemCompleted())

            run {
                addAll(innerBatchEvents)
                add(itemCompleted())

                run {
                    addAll(innerBatchEvents)
                    add(itemCompleted())

                    addAll(innerBatchEvents)
                    add(itemCompleted())

                    add(batchCompleted())
                }
                add(itemCompleted())

                add(batchCompleted())
            }
            add(itemCompleted())

            addAll(innerBatchEvents)
            add(itemCompleted())

            // first leve batch ends
            add(batchCompleted())
            add(extrinsicSuccess())
        }

        val extrinsic = createExtrinsic(
            call = batchCall(
                testInnerCall,
                batchCall(
                    testInnerCall,
                    batchCall(
                        testInnerCall,
                        testInnerCall
                    )
                ),
                testInnerCall
            ),
            events = events
        )

        val visits = extrinsicWalk.walkMultipleIgnoringBranches(extrinsic, expectedSize = 5)
        visits.forEach { visit ->
            assertEquals(true, visit.success)
            assertArrayEquals(signer, visit.origin)
            assertEquals(testInnerCall, visit.call)
            assertEquals(innerBatchEvents, visit.events)
        }
    }

    private fun createExtrinsic(
        call: GenericCall.Instance,
        events: List<GenericEvent.Instance>
    ) = createExtrinsic(signer, call, events)

    private fun itemCompleted(): GenericEvent.Instance {
        return GenericEvent.Instance(utilityModule, itemCompletedType, arguments = emptyList())
    }

    private fun batchCompleted(): GenericEvent.Instance {
        return GenericEvent.Instance(utilityModule, batchCompletedType, arguments = emptyList())
    }

    private fun batchCall(vararg innerCalls: GenericCall.Instance): GenericCall.Instance {
        return mockCall(
            moduleName = Modules.UTILITY,
            callName = "batch_all",
            arguments = mapOf(
                "calls" to innerCalls.toList()
            )
        )
    }
}
