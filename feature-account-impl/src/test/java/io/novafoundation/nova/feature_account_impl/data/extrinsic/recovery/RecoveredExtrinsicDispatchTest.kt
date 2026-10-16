package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class RecoveredExtrinsicDispatchTest {
    private val stake = call("SubtensorModule", "add_stake")

    @Test fun `failed system dispatch is failure even without wrapper events`() {
        assertEquals(RecoveredExtrinsicOutcome.FAILURE, recoveredExtrinsicDispatch(call("Proxy", "proxy", stake), ExtrinsicStatus.FAILURE, emptyList()))
    }

    @Test fun `successful envelope with rejected proxy call is not staking success`() {
        val event = event("Proxy", "ProxyExecuted", listOf(DictEnum.Entry("Err", null)))
        assertEquals(RecoveredExtrinsicOutcome.FAILURE, recoveredExtrinsicDispatch(call("Proxy", "proxy", stake), ExtrinsicStatus.SUCCESS, listOf(event)))
    }

    @Test fun `proxy success requires positive inner dispatch evidence`() {
        val event = event("Proxy", "ProxyExecuted", listOf(DictEnum.Entry("Ok", null)))
        assertEquals(RecoveredExtrinsicOutcome.SUCCESS, recoveredExtrinsicDispatch(call("Proxy", "proxy", stake), ExtrinsicStatus.SUCCESS, listOf(event)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing proxy event remains unverifiable instead of successful`() {
        recoveredExtrinsicDispatch(call("Proxy", "proxy", stake), ExtrinsicStatus.SUCCESS, emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nested proxies require evidence for both levels`() {
        val event = event("Proxy", "ProxyExecuted", listOf(DictEnum.Entry("Ok", null)))
        recoveredExtrinsicDispatch(call("Proxy", "proxy", call("Proxy", "proxy", stake)), ExtrinsicStatus.SUCCESS, listOf(event))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `multisig approval is not execution`() {
        val event = event("Multisig", "NewMultisig", emptyList())
        recoveredExtrinsicDispatch(call("Multisig", "as_multi", stake), ExtrinsicStatus.SUCCESS, listOf(event))
    }

    @Test fun `a rejected final multisig dispatch is failure`() {
        val event = event("Multisig", "MultisigExecuted", listOf(null, null, null, null, DictEnum.Entry("Err", null)))
        assertEquals(RecoveredExtrinsicOutcome.FAILURE, recoveredExtrinsicDispatch(call("Multisig", "as_multi", stake), ExtrinsicStatus.SUCCESS, listOf(event)))
    }

    @Test fun `a fully executed legacy multisig can be recovered`() {
        val event = event("Multisig", "MultisigExecuted", listOf(null, null, null, null, DictEnum.Entry("Ok", null)))
        assertEquals(RecoveredExtrinsicOutcome.SUCCESS, recoveredExtrinsicDispatch(call("Multisig", "as_multi", stake), ExtrinsicStatus.SUCCESS, listOf(event)))
    }

    @Test fun `interrupted utility batch is not complete success`() {
        val event = event("Utility", "BatchInterrupted", emptyList())
        assertEquals(RecoveredExtrinsicOutcome.FAILURE, recoveredExtrinsicDispatch(call("Utility", "batch", stake), ExtrinsicStatus.SUCCESS, listOf(event)))
    }

    @Test fun `signed call tree identifies a nested multisig before broadcasting`() {
        assertTrue(containsMultisigCall(call("Proxy", "proxy", call("Multisig", "as_multi", stake))))
        assertFalse(containsMultisigCall(call("Proxy", "proxy", stake)))
    }

    private fun call(moduleName: String, name: String, child: GenericCall.Instance? = null): GenericCall.Instance {
        val module = Module(moduleName, null, emptyMap(), null, emptyMap(), emptyMap(), BigInteger.ZERO)
        val function = MetadataFunction(name, emptyList(), emptyList(), 0 to 0)
        return GenericCall.Instance(module, function, if (child == null) emptyMap() else mapOf("call" to child))
    }

    private fun event(moduleName: String, name: String, arguments: List<Any?>): GenericEvent.Instance {
        val module = Module(moduleName, null, emptyMap(), null, emptyMap(), emptyMap(), BigInteger.ZERO)
        val event = Event(name, index = 0 to 0, documentation = emptyList(), arguments = emptyList())
        return GenericEvent.Instance(module, event, arguments)
    }
}
