package io.novafoundation.nova.feature_account_impl.data.extrinsic

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicBuildingContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicDryRunResult
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.eq
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class RealExtrinsicServiceTest {

    private val rpcCalls = mock(RpcCalls::class.java)

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val accountRepository = mock(AccountRepository::class.java)

    private val builderFactory = mock(ExtrinsicBuilderFactory::class.java)

    private val signerProvider = mock(SignerProvider::class.java)

    private val splitter = mock(ExtrinsicSplitter::class.java)

    private val feeRegistry = mock(FeePaymentProviderRegistry::class.java)

    private val eventsRepository = mock(EventsRepository::class.java)

    private val signingContextFactory = mock(SigningContext.Factory::class.java)

    private val signingContext = mock(SigningContext::class.java)

    private val feeProvider = mock(FeePaymentProvider::class.java)

    private val feePayment = mock(FeePayment::class.java)

    private val chain = mock(Chain::class.java)

    private val wallet = mock(MetaAccount::class.java)

    private val signer = mock(NovaSigner::class.java)

    private val builder = mock(ExtrinsicBuilder::class.java)

    private val extrinsic = mock(SendableExtrinsic::class.java)

    private val confirmedCall = mock(GenericCall.Instance::class.java)

    private val runtimeProvider = mock(RuntimeProvider::class.java)

    private val runtime = mock(RuntimeSnapshot::class.java)

    private val hierarchy = SubmissionHierarchy(wallet, CallExecutionType.IMMEDIATE)

    private val options = ExtrinsicService.SubmissionOptions()

    private val builderOptions = ExtrinsicBuilderFactory.Options(options.batchMode)

    private val origin = TransactionOrigin.WalletWithId(42L)

    private val accountId: AccountId = byteArrayOf(1, 2, 3)

    private val signingAccountId: AccountId = byteArrayOf(4, 5, 6)

    private val inBlock = ExtrinsicStatus.InBlock(blockHash = "block", extrinsicHash = "hash")

    private val formExtrinsic: FormExtrinsicWithOrigin = { call(confirmedCall) }

    private val service = RealExtrinsicService(
        rpcCalls = rpcCalls,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        extrinsicBuilderFactory = builderFactory,
        signerProvider = signerProvider,
        extrinsicSplitter = splitter,
        feePaymentProviderRegistry = feeRegistry,
        eventsRepository = eventsRepository,
        signingContextFactory = signingContextFactory,
        coroutineScope = null
    )

    @Before
    fun setUp() {
        runBlocking {
            `when`(chain.id).thenReturn("chain")
            `when`(accountRepository.getMetaAccount(42L)).thenReturn(wallet)
            `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
            `when`(signer.metaAccount).thenReturn(wallet)
            `when`(wallet.accountIdIn(chain)).thenReturn(accountId)
            `when`(signer.submissionSignerAccountId(chain)).thenReturn(signingAccountId)
            `when`(signer.getSigningHierarchy()).thenReturn(hierarchy)
            `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE)
            `when`(
                builderFactory.create(
                    eq<Chain?>(chain) ?: chain,
                    any<ExtrinsicBuilderFactory.Options?>() ?: builderOptions
                )
            ).thenReturn(builder)
            `when`(builder.buildExtrinsic()).thenReturn(extrinsic)
            `when`(feeRegistry.providerFor("chain")).thenReturn(feeProvider)
            `when`(feeProvider.feePaymentFor(options.feePaymentCurrency, coroutineScope = null)).thenReturn(feePayment)
            `when`(signingContextFactory.default(chain)).thenReturn(signingContext)
            `when`(chainRegistry.getRuntimeProvider("chain")).thenReturn(runtimeProvider)
            `when`(runtimeProvider.get()).thenReturn(runtime)
        }
    }

    @Test
    fun `submission keeps the confirmed call and explicit signing wallet`() = runBlocking<Unit> {
        `when`(rpcCalls.submitExtrinsic("chain", extrinsic)).thenReturn("hash")
        var buildingContext: ExtrinsicBuildingContext? = null
        val confirmedBuilder: FormExtrinsicWithOrigin = { context ->
            buildingContext = context
            call(confirmedCall)
        }

        val result = service.submitExtrinsic(chain, origin, options, confirmedBuilder).getOrThrow()

        assertSame(signer, buildingContext?.signer)
        assertSame(chain, buildingContext?.chain)
        assertSame(accountId, result.submissionOrigin.executingAccount)
        assertSame(signingAccountId, result.submissionOrigin.signingAccount)
        assertSame(hierarchy, result.submissionHierarchy)
        assertEquals("hash", result.hash)
        assertEquals(CallExecutionType.IMMEDIATE, result.callExecutionType)
        verify(builder).call(confirmedCall)
        verify(feePayment).modifyExtrinsic(builder)
        with(builder) { verify(signer).setSignerDataForSubmission(signingContext) }
        verify(accountRepository, never()).getSelectedMetaAccount()
        verify(signerProvider).rootSignerFor(wallet)
    }

    @Test
    fun `preparation cancellation propagates for both submission entry points`() = runBlocking<Unit> {
        val cancellation = CancellationException("Preparation cancelled")
        `when`(accountRepository.getMetaAccount(42L)).thenThrow(cancellation)

        assertCancellation(cancellation) { service.submitExtrinsic(chain, origin, options, formExtrinsic) }
        assertCancellation(cancellation) { service.submitAndWatchExtrinsic(chain, origin, options, formExtrinsic) }
        verifyNoInteractions(rpcCalls)
    }

    @Test
    fun `submission RPC cancellation is not returned as a recoverable error`() = runBlocking<Unit> {
        val cancellation = CancellationException("Submission cancelled")
        `when`(rpcCalls.submitExtrinsic("chain", extrinsic)).thenThrow(cancellation)

        assertCancellation(cancellation) { service.submitExtrinsic(chain, origin, options, formExtrinsic) }
    }

    @Test
    fun `signing cancellation propagates without sending or building a signed extrinsic`() = runBlocking<Unit> {
        val cancellation = CancellationException("Signing cancelled")
        with(builder) {
            `when`(signer.setSignerDataForSubmission(signingContext)).thenThrow(cancellation)
        }

        assertCancellation(cancellation) { service.submitExtrinsic(chain, origin, options, formExtrinsic) }
        assertCancellation(cancellation) { service.submitAndWatchExtrinsic(chain, origin, options, formExtrinsic) }

        verify(builder, never()).buildExtrinsic()
        verifyNoInteractions(rpcCalls)
    }

    @Test
    fun `ordinary preparation failure retains its cause and never sends`() = runBlocking<Unit> {
        val failure = IllegalStateException("Cannot build")
        `when`(accountRepository.getMetaAccount(42L)).thenThrow(failure)

        assertSame(failure, service.submitExtrinsic(chain, origin, options, formExtrinsic).exceptionOrNull())
        assertSame(failure, service.submitAndWatchExtrinsic(chain, origin, options, formExtrinsic).exceptionOrNull())
        verifyNoInteractions(rpcCalls)
    }

    @Test
    fun `unsupported dry run never builds signs or submits`() = runBlocking<Unit> {
        val failure = UnsupportedOperationException("Dry run unavailable")
        `when`(rpcCalls.requireDryRunSupported("chain")).thenThrow(failure)

        try {
            service.dryRunExtrinsic(chain, origin, options, formExtrinsic)
            fail("Unsupported dry run must fail")
        } catch (error: UnsupportedOperationException) {
            assertSame(failure, error)
        }
        verifyNoInteractions(accountRepository, signerProvider, builderFactory)
        verify(rpcCalls, never()).submitExtrinsic(any(), any())
        verify(rpcCalls, never()).submitAndWatchExtrinsic(any(), any())
    }

    @Test
    fun `supported dry run uses the confirmed call without submitting`() = runBlocking<Unit> {
        `when`(rpcCalls.dryRunExtrinsic("chain", extrinsic)).thenReturn(ExtrinsicDryRunResult.Success)

        assertSame(ExtrinsicDryRunResult.Success, service.dryRunExtrinsic(chain, origin, options, formExtrinsic))

        verify(rpcCalls).requireDryRunSupported("chain")
        verify(builder).call(confirmedCall)
        with(builder) { verify(signer).setSignerDataForSubmission(signingContext) }
        verify(accountRepository, never()).getSelectedMetaAccount()
        verify(rpcCalls, never()).submitExtrinsic(any(), any())
        verify(rpcCalls, never()).submitAndWatchExtrinsic(any(), any())
    }

    @Test
    fun `event lookup cancellation propagates after inclusion`() = runBlocking<Unit> {
        givenInclusion()
        val cancellation = CancellationException("Event lookup cancelled")
        `when`(eventsRepository.getExtrinsicWithEvents("chain", "hash", "block")).thenThrow(cancellation)

        assertCancellation(cancellation) {
            service.submitExtrinsicAndAwaitExecution(chain, origin, options, formExtrinsic)
        }
    }

    @Test
    fun `event lookup failure or missing evidence remains unknown`() = runBlocking<Unit> {
        givenInclusion()
        val failure = IllegalStateException("Events unavailable")
        `when`(eventsRepository.getExtrinsicWithEvents("chain", "hash", "block"))
            .thenThrow(failure)
            .thenReturn(null)

        mockStatic(Log::class.java).use {
            repeat(2) {
                val result = service.submitExtrinsicAndAwaitExecution(chain, origin, options, formExtrinsic).getOrThrow()

                assertSame(ExtrinsicDispatch.Unknown, result.outcome)
                assertSame(hierarchy, result.submissionHierarchy)
                assertEquals("hash", result.extrinsicHash)
                assertEquals("block", result.blockHash)
                assertFalse(Result.success(result).requireOk().isSuccess)
            }
        }
    }

    @Test
    fun `dispatch outcomes retain events on both success and failure`() = runBlocking<Unit> {
        givenInclusion()
        val successEvent = event("ExtrinsicSuccess")
        val dispatchError = DictEnum.Entry("Token", value = null)
        val failureEvent = event("ExtrinsicFailed", listOf(dispatchError))
        val includedExtrinsic = mock(Extrinsic.Instance::class.java)
        val successful = ExtrinsicWithEvents(includedExtrinsic, "hash", listOf(successEvent))
        val failed = ExtrinsicWithEvents(includedExtrinsic, "hash", listOf(failureEvent))
        `when`(eventsRepository.getExtrinsicWithEvents("chain", "hash", "block"))
            .thenReturn(successful)
            .thenReturn(failed)

        val success = service.submitExtrinsicAndAwaitExecution(chain, origin, options, formExtrinsic).getOrThrow()
        val failure = service.submitExtrinsicAndAwaitExecution(chain, origin, options, formExtrinsic).getOrThrow()

        assertSame(successful.events, (success.outcome as ExtrinsicDispatch.Ok).emittedEvents)
        assertSame(failed.events, (failure.outcome as ExtrinsicDispatch.Failed).emittedEvents)
        assertSame(DispatchError.Token, (failure.outcome as ExtrinsicDispatch.Failed).error)
        assertTrue(Result.success(success).requireOk().isSuccess)
        assertSame(DispatchError.Token, Result.success(failure).requireOk().exceptionOrNull())
    }

    private fun givenInclusion() {
        `when`(rpcCalls.submitAndWatchExtrinsic("chain", extrinsic)).thenReturn(flowOf(inBlock))
    }

    private fun event(name: String, arguments: List<Any?> = emptyList()): GenericEvent.Instance {
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn("System")
        val event = mock(Event::class.java)
        `when`(event.name).thenReturn(name)
        return GenericEvent.Instance(module, event, arguments)
    }

    private suspend fun assertCancellation(expected: CancellationException, action: suspend () -> Any?) {
        try {
            action()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
    }
}
