package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.data.network.runtime.binding.bindMultiAddress
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.test_shared.whenever
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.MetadataFunction
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
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

suspend fun ExtrinsicWalk.walkSingle(extrinsicWithEvents: ExtrinsicWithEvents): ExtrinsicVisit {
    val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT)
    Assert.assertEquals(1, visits.size)

    return visits.single()
}

suspend fun ExtrinsicWalk.walkMultiple(extrinsicWithEvents: ExtrinsicWithEvents, expectedSize: Int): List<ExtrinsicVisit> {
    val visits = walkToList(extrinsicWithEvents, Chain.Geneses.POLKADOT)
    Assert.assertEquals(expectedSize, visits.size)

    return visits
}
