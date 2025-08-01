package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.data.network.runtime.binding.bindMultiAddress
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert
import org.mockito.Mockito
import java.math.BigInteger

fun createTestModuleWithCall(
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

fun createExtrinsic(
    signer: AccountId,
    call: GenericCall.Instance,
    events: List<GenericEvent.Instance>
) = ExtrinsicWithEvents(
    extrinsic = Extrinsic.Instance(
        type = Extrinsic.ExtrinsicType.Signed(
            accountIdentifier = bindMultiAddress(MultiAddress.Id(signer)),
            signature = null,
            signedExtras = emptyMap()
        ),
        call = call,
    ),
    extrinsicHash = "0x",
    events = events
)

fun extrinsicSuccess(): GenericEvent.Instance {
    return mockEvent("System", "ExtrinsicSuccess")
}

fun extrinsicFailed(): GenericEvent.Instance {
    return mockEvent("System", "ExtrinsicFailed")
}

fun mockEvent(moduleName: String, eventName: String, arguments: List<Any?> = emptyList()): GenericEvent.Instance {
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

fun mockCall(moduleName: String, callName: String, arguments: Map<String, Any?> = emptyMap()): GenericCall.Instance {
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

class TestModuleMocker {

    val testModule = createTestModuleWithCall(moduleName = "Test", callName = "test")

    val testInnerCall = GenericCall.Instance(
        module = testModule,
        function = testModule.call("test"),
        arguments = emptyMap()
    )

    val testEvent = mockEvent(testModule.name, "test")

    operator fun component1(): GenericCall.Instance {
        return testInnerCall
    }

    operator fun component2():GenericEvent.Instance {
        return testEvent
    }
}

suspend fun ExtrinsicWalk.walkSingleIgnoringBranches(extrinsicWithEvents: ExtrinsicWithEvents): ExtrinsicVisit {
    val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT).ignoreBranches()
    Assert.assertEquals(1, visits.size)

    return visits.single()
}

suspend fun ExtrinsicWalk.walkToList(extrinsicWithEvents: ExtrinsicWithEvents): List<ExtrinsicVisit> {
   return walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT)
}

suspend fun ExtrinsicWalk.walkEmpty(extrinsicWithEvents: ExtrinsicWithEvents) {
    val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT).ignoreBranches()
    Assert.assertTrue(visits.isEmpty())
}


suspend fun ExtrinsicWalk.walkMultipleIgnoringBranches(extrinsicWithEvents: ExtrinsicWithEvents, expectedSize: Int): List<ExtrinsicVisit> {
    val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT).ignoreBranches()
    Assert.assertEquals(expectedSize, visits.size)

    return visits
}

private fun List<ExtrinsicVisit>.ignoreBranches(): List<ExtrinsicVisit> {
    return filterNot { it.hasRegisteredNode }
}
