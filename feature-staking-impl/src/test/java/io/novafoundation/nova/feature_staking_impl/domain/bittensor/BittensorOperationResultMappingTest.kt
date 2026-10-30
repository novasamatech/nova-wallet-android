package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicWatchException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

class BittensorOperationResultMappingTest {

    private val account = mock(MetaAccount::class.java)

    private val immediateNode = SubmissionHierarchy.Node(account, CallExecutionType.IMMEDIATE)

    private val delayedNode = SubmissionHierarchy.Node(account, CallExecutionType.DELAYED)

    @Test
    fun `proxy completion requires exact wrapper count and every result ok`() {
        val proxyAccount = mock(MetaAccount::class.java)
        `when`(proxyAccount.type).thenReturn(LightMetaAccount.Type.PROXIED)
        val proxyNode = SubmissionHierarchy.Node(proxyAccount, CallExecutionType.IMMEDIATE)
        val path = listOf(immediateNode, proxyNode)
        val ok = event("Proxy", "ProxyExecuted", listOf(DictEnum.Entry("Ok", null)))
        val error = event("Proxy", "ProxyExecuted", listOf(DictEnum.Entry("Err", null)))
        val success = execution(ExtrinsicDispatch.Ok(listOf(ok)), path)
        assertEquals(BittensorOperationResult.Success(HASH), success.mapBittensorOperationResult())
        listOf(emptyList(), listOf(error), listOf(ok, ok)).forEach { events ->
            assertEquals(BittensorOperationResult.Unknown(HASH), execution(ExtrinsicDispatch.Ok(events), path).mapBittensorOperationResult())
        }
    }

    @Test
    fun `multisig requires matching account completion and rejects approvals`() {
        val chain = mock(Chain::class.java)
        val multisigAccount = mock(MetaAccount::class.java)
        val address = ByteArray(32) { 7 }
        `when`(multisigAccount.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(multisigAccount.accountIdIn(chain)).thenReturn(address)
        val node = SubmissionHierarchy.Node(multisigAccount, CallExecutionType.IMMEDIATE)
        val path = listOf(immediateNode, node)
        val ok = event("Multisig", "MultisigExecuted", listOf(null, null, address, null, DictEnum.Entry("Ok", null)))
        val wrong = event("Multisig", "MultisigExecuted", listOf(null, null, ByteArray(32), null, DictEnum.Entry("Ok", null)))
        val approval = event("Multisig", "MultisigApproval", emptyList())
        assertEquals(BittensorOperationResult.Success(HASH), execution(ExtrinsicDispatch.Ok(listOf(ok)), path).mapBittensorOperationResult(chain))
        listOf(wrong, approval).forEach { event ->
            assertEquals(BittensorOperationResult.Unknown(HASH), execution(ExtrinsicDispatch.Ok(listOf(event)), path).mapBittensorOperationResult(chain))
        }
    }

    @Test
    fun `batch interruption and malformed nested completion cannot become success`() {
        val interrupted = event("Utility", "BatchInterrupted", emptyList())
        val malformed = event("Proxy", "ProxyExecuted", emptyList())
        listOf(interrupted, malformed).forEach { event ->
            val result = execution(ExtrinsicDispatch.Ok(listOf(event)), listOf(immediateNode))
            assertEquals(BittensorOperationResult.Unknown(HASH), result.mapBittensorOperationResult())
        }
    }

    @Test
    fun `direct immediate dispatch success is executed`() {
        val result = execution(ExtrinsicDispatch.Ok(emptyList()), listOf(immediateNode))
        assertEquals(BittensorOperationResult.Success(HASH), result.mapBittensorOperationResult())
    }

    @Test
    fun `outer success does not prove nested immediate call execution`() {
        val result = execution(ExtrinsicDispatch.Ok(emptyList()), listOf(immediateNode, immediateNode))
        assertEquals(BittensorOperationResult.Unknown(HASH), result.mapBittensorOperationResult())
    }

    @Test
    fun `approval and empty hierarchy cannot become completed trade`() {
        listOf(listOf(delayedNode), emptyList()).forEach { path ->
            val result = execution(ExtrinsicDispatch.Ok(emptyList()), path)
            assertEquals(BittensorOperationResult.Unknown(HASH), result.mapBittensorOperationResult())
        }
    }

    @Test
    fun `confirmed outer failure preserves hash and cause`() {
        val dispatch = ExtrinsicDispatch.Failed(DispatchError.Unknown)
        val result = execution(dispatch, listOf(immediateNode))
        val evidence = result.mapBittensorOperationResult() as BittensorOperationResult.Failure
        assertEquals(HASH, evidence.transactionHash)
        assertEquals(BittensorFailureStage.DISPATCHED, evidence.stage)
        assertSame(dispatch.error, evidence.cause)
    }

    @Test
    fun `missing dispatch evidence is unconfirmed`() {
        val result = execution(ExtrinsicDispatch.Unknown, listOf(immediateNode))
        assertEquals(BittensorOperationResult.Unknown(HASH), result.mapBittensorOperationResult())
    }

    @Test
    fun `all ambiguous watch failures preserve known hash without offering failure retry`() {
        val statuses = listOf(
            ExtrinsicStatus.Dropped(HASH),
            ExtrinsicStatus.Invalid(HASH),
            ExtrinsicStatus.Usurped("replacement", HASH),
            ExtrinsicStatus.FinalityTimeout("block", HASH)
        )
        statuses.forEach { status ->
            val error = ExtrinsicWatchException(status)
            assertEquals(BittensorOperationResult.Unknown(HASH), error.mapBittensorSubmissionError())
        }
    }

    @Test
    fun `transport error without hash is not proof of non submission`() {
        val error = IOException("connection lost")
        assertEquals(BittensorOperationResult.Unknown(null), error.mapBittensorSubmissionError())
    }

    @Test
    fun `known preparation failure is not submitted`() {
        val error = IllegalArgumentException("invalid draft")
        val evidence = error.mapBittensorPreSubmissionError()
        assertEquals(BittensorFailureStage.NOT_SUBMITTED, evidence.stage)
        assertSame(error, evidence.cause)
    }

    @Test
    fun `cancellation is rethrown at both boundaries`() {
        val cancellation = CancellationException("cancelled")
        val classifiers = listOf<() -> Unit>(
            { cancellation.mapBittensorSubmissionError() },
            { cancellation.mapBittensorPreSubmissionError() }
        )
        classifiers.forEach { classify ->
            try {
                classify()
                fail("Expected cancellation")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }
    }

    private fun execution(dispatch: ExtrinsicDispatch, path: List<SubmissionHierarchy.Node>): ExtrinsicExecutionResult {
        val hierarchy = SubmissionHierarchy(path)
        return ExtrinsicExecutionResult(HASH, "block", dispatch, hierarchy)
    }

    private fun event(moduleName: String, name: String, args: List<Any?>): GenericEvent.Instance {
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn(moduleName)
        val type = Event(name, index = 0 to 0, documentation = emptyList(), arguments = emptyList())
        return GenericEvent.Instance(module, type, args)
    }

    companion object {
        private const val HASH = "transaction-hash"
    }
}
