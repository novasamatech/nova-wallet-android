package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.multisig.MultisigNode
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.multisig.generateMultisigAddress
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
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
internal class MultisigWalkTest {

    @Mock
    private lateinit var runtimeProvider: RuntimeProvider

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    @Mock
    private lateinit var runtime: RuntimeSnapshot

    @Mock
    private lateinit var metadata: RuntimeMetadata

    @Mock
    private lateinit var multisigModule: Module

    private lateinit var extrinsicWalk: ExtrinsicWalk

    private val multisigExecutedEvent = Event("MultisigExecuted", index = 0 to 0, documentation = emptyList(), arguments = emptyList())
    private val multisigApprovalEvent = Event("MultisigApproval", index = 0 to 1, documentation = emptyList(), arguments = emptyList())
    private val multisigNewMultisigEvent = Event("NewMultisig", index = 0 to 1, documentation = emptyList(), arguments = emptyList())

    private val signatory = byteArrayOf(0x00)
    private val otherSignatories = listOf(byteArrayOf(0x01))
    private val threshold = 2
    private val multisig = generateMultisigAddress(
        signatory = signatory.intoKey(),
        otherSignatories = otherSignatories.map { it.intoKey() },
        threshold = threshold
    ).value

    private val testModuleMocker = TestModuleMocker()
    private val testEvent = testModuleMocker.testEvent
    private val testInnerCall = testModuleMocker.testInnerCall

    @Before
    fun setup() = runBlocking {
        // Explicitly using string literals instead of accessing name property as this would result in Unfinished stubbing exception
        whenever(multisigModule.events).thenReturn(
            mapOf(
                "MultisigExecuted" to multisigExecutedEvent,
                "MultisigApproval" to multisigApprovalEvent,
                "NewMultisig" to multisigNewMultisigEvent
            )
        )
        whenever(multisigModule.name).thenReturn("Multisig")
        whenever(metadata.modules).thenReturn(mapOf("Multisig" to multisigModule))
        whenever(runtime.metadata).thenReturn(metadata)
        whenever(runtimeProvider.get()).thenReturn(runtime)
        whenever(chainRegistry.getRuntimeProvider(any())).thenReturn(runtimeProvider)

        extrinsicWalk = RealExtrinsicWalk(chainRegistry, knownNodes = listOf(MultisigNode()))
    }

    @Test
    fun shouldVisitSucceededSingleMultisigCall() = runBlocking {
        val innerMultisigEvents = listOf(testEvent)
        val events = innerMultisigEvents + listOf(multisigExecuted(success = true), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = signatory,
            call = multisig_call(
                innerCall = testInnerCall,
                threshold = threshold,
                otherSignatories = otherSignatories
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(true, visit.success)
        assertArrayEquals(multisig, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerMultisigEvents, visit.events)
    }

    @Test
    fun shouldVisitFailedSingleMultisigCall() = runBlocking {
        val innerMultisigEvents = emptyList<GenericEvent.Instance>()
        val events = listOf(multisigExecuted(success = false), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = signatory,
            call = multisig_call(
                innerCall = testInnerCall,
                threshold = threshold,
                otherSignatories = otherSignatories
            ),
            events = events
        )

        val visit = extrinsicWalk.walkSingleIgnoringBranches(extrinsic)
        assertEquals(false, visit.success)
        assertArrayEquals(multisig, visit.origin)
        assertEquals(testInnerCall, visit.call)
        assertEquals(innerMultisigEvents, visit.events)
    }

    @Test
    fun shouldVisitNewMultisigCall() = runBlocking {
        val events = listOf(newMultisig(), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = signatory,
            call = multisig_call(
                innerCall = testInnerCall,
                threshold = threshold,
                otherSignatories = otherSignatories
            ),
            events = events
        )

        extrinsicWalk.walkEmpty(extrinsic)
    }

    @Test
    fun shouldVisitMultisigApprovalCall() = runBlocking {
        val events = listOf(multisigApproval(), extrinsicSuccess())

        val extrinsic = createExtrinsic(
            signer = signatory,
            call = multisig_call(
                innerCall = testInnerCall,
                threshold = threshold,
                otherSignatories = otherSignatories
            ),
            events = events
        )

        extrinsicWalk.walkEmpty(extrinsic)
    }

    @Test
    fun shouldVisitSucceededNestedMultisigCalls() = runBlocking {
        val events = listOf(
            newMultisig(),
            multisigExecuted(success = true),
            extrinsicSuccess()
        )

        val otherSignatories2 = otherSignatories
        val threshold2 = 1

        val extrinsic = createExtrinsic(
            signer = signatory,
            call = multisig_call(
                threshold = threshold,
                otherSignatories = otherSignatories,
                innerCall = multisig_call(
                    innerCall = testInnerCall,
                    threshold = threshold2,
                    otherSignatories = otherSignatories2
                ),
            ),
            events = events
        )

        val visit = extrinsicWalk.walkToList(extrinsic)
        assertEquals(2, visit.size)

        val visit1 = visit[0]
        assertEquals(true, visit1.success)
        assertArrayEquals(signatory, visit1.origin)

        val visit2 = visit[1]
        assertEquals(true, visit2.success)
        assertArrayEquals(multisig, visit2.origin)
    }

    private fun multisigExecuted(success: Boolean): GenericEvent.Instance {
        val outcomeVariant = if (success) "Ok" else "Err"
        val outcome = DictEnum.Entry(name = outcomeVariant, value = null)

        return GenericEvent.Instance(multisigModule, multisigExecutedEvent, arguments = listOf(null, null, null, null, outcome))
    }

    private fun newMultisig(): GenericEvent.Instance {
        return GenericEvent.Instance(multisigModule, multisigNewMultisigEvent, arguments = emptyList())
    }

    private fun multisigApproval(): GenericEvent.Instance {
        return GenericEvent.Instance(multisigModule, multisigApprovalEvent, arguments = emptyList())
    }

    private fun multisig_call(
        innerCall: GenericCall.Instance,
        threshold: Int,
        otherSignatories: List<ByteArray>
    ): GenericCall.Instance {
        return mockCall(
            moduleName = "Multisig",
            callName = "as_multi",
            arguments = mapOf(
                "threshold" to threshold.toBigInteger(),
                "other_signatories" to otherSignatories,
                "call" to innerCall,
                // other args are not relevant
            )
        )
    }
}
