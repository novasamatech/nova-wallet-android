package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevExecution
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevCarrier
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotReady
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.startCoroutine
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevService
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorHistoryRequest
import io.novafoundation.nova.runtime.ext.addressOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.doThrow
import kotlinx.coroutines.cancelAndJoin
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistorySource
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorSubmissionExecutorTimeoutTest {

    private val history = mock(BittensorOperationHistorySource::class.java)

    private val logger = mock(Logger::class.java)

    private val positions = mock(BittensorPositionRepository::class.java)

    private val chain = mock(Chain::class.java).also { `when`(it.id).thenReturn("bittensor") }

    private val coldKey = ByteArray(32)

    private val historyRequest = BittensorHistoryRequest(chain.addressOf(coldKey), "https://infra.test/".toHttpUrl(), 0L, METAID)

    private val targetSubnet = BittensorNetuid.Subnet(7)

    private val subnetTarget = BittensorOutcomeTarget(BittensorOperationKind.STAKE, targetSubnet, emptySet())

    private val subnetScope = BittensorSettlementScope.Network(targetSubnet)

    private val account = mock(MetaAccount::class.java)

    private val wallet = mock(MetaAccount::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val hierarchy = SubmissionHierarchy(account, CallExecutionType.IMMEDIATE)

    private val execution = ExtrinsicExecutionResult(HASH, "0xblock", ExtrinsicDispatch.Ok(emptyList()), hierarchy)

    private val extrinsics = FakeExtrinsicService()

    private val positionUpdates = BittensorPositionUpdates()

    private var mevSubmission: suspend (suspend (String) -> Unit, () -> Unit) -> BittensorMevExecution = { _, _ -> error("Not configured") }

    @Suppress("UNCHECKED_CAST")
    private val mev = mock(BittensorMevService::class.java) { invocation ->
        if (invocation.method.name == "submit") {
            val onUnconfirmed = invocation.getArgument<suspend (String) -> Unit>(3)
            val onHandoff = invocation.getArgument<() -> Unit>(4)
            val continuation = invocation.rawArguments.last() as Continuation<BittensorMevExecution>
            val submit: suspend () -> BittensorMevExecution = { mevSubmission(onUnconfirmed, onHandoff) }
            submit.startCoroutine(continuation)
            COROUTINE_SUSPENDED
        } else {
            org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val resolver = BittensorExtrinsicOutcomeResolver { _, inBlock ->
        assertEquals(HASH, inBlock.status.extrinsicHash)
        execution
    }

    private val executor = BittensorSubmissionExecutor(extrinsics, accounts, positionUpdates, positions, history, logger, resolver, mev)

    @Test
    fun `build that never completes is a retryable not submitted failure and releases the lock`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { awaitCancellation() }

        val result = executor.execute(METAID, chain, target = subnetTarget) {} as BittensorOperationResult.Failure

        assertEquals(BITTENSOR_BUILD_AND_SIGN_TIMEOUT.inWholeMilliseconds, testScheduler.currentTime)
        assertEquals(BittensorFailureStage.NOT_SUBMITTED, result.stage)
        assertTrue(result.cause is BittensorBuildTimeoutException)
        assertTrue(result.isRetryable)
        assertFalse(result.isSigningCancelled)
        verifyNoInteractions(positions)
        verify(history, org.mockito.Mockito.never()).ownTradeSubmitted(historyRequest, subnetScope)
        assertLockReleased()
    }

    @Test
    fun `proxied wallet with a key held in the app is bounded by the build timeout`() = runTest {
        val proxyKey = mock(MetaAccount::class.java)
        `when`(proxyKey.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(accounts.getMetaAccount(PROXY_METAID)).thenReturn(proxyKey)
        val proxy = mock(ProxyAccount::class.java)
        `when`(proxy.proxyMetaId).thenReturn(PROXY_METAID)
        val proxied = mock(ProxiedMetaAccount::class.java)
        `when`(proxied.type).thenReturn(LightMetaAccount.Type.PROXIED)
        `when`(proxied.proxy).thenReturn(proxy)
        `when`(proxied.accountIdIn(chain)).thenReturn(coldKey)
        `when`(accounts.getMetaAccount(METAID)).thenReturn(proxied)
        extrinsics.build = { awaitCancellation() }

        val result = executor.execute(METAID, chain) {} as BittensorOperationResult.Failure

        assertEquals(BITTENSOR_BUILD_AND_SIGN_TIMEOUT.inWholeMilliseconds, testScheduler.currentTime)
        assertTrue(result.cause is BittensorBuildTimeoutException)
    }

    @Test
    fun `signing outside the app is not bounded by the build timeout`() = runTest {
        useWallet(LightMetaAccount.Type.POLKADOT_VAULT)
        val signingTime = BITTENSOR_BUILD_AND_SIGN_TIMEOUT * 2
        extrinsics.build = {
            delay(signingTime)
            inBlockWatch()
        }

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain) {})
        assertTrue(testScheduler.currentTime >= signingTime.inWholeMilliseconds)
    }

    @Test
    fun `status that never arrives after send is unknown and releases the lock`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = {
            flow {
                emit(watchResult(ExtrinsicStatus.Ready(HASH)))
                awaitCancellation()
            }
        }

        val result = executor.execute(METAID, chain, target = subnetTarget) {}

        assertEquals(BittensorOperationResult.Unknown(HASH), result)
        assertEquals(BITTENSOR_SEND_AND_INCLUSION_TIMEOUT.inWholeMilliseconds, testScheduler.currentTime)
        assertEquals(1, extrinsics.submissions)
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
        assertLockReleased()
        assertEquals(2, extrinsics.submissions)
    }

    @Test
    fun `send without any status is unknown without a hash`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { flow { awaitCancellation() } }

        assertEquals(BittensorOperationResult.Unknown(null), executor.execute(METAID, chain) {})
    }

    @Test
    fun `cancelling a hanging execution releases the lock`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { awaitCancellation() }
        val hanging = launch { executor.execute(METAID, chain) {} }
        runCurrent()
        hanging.cancel()

        assertLockReleased()
    }

    @Test
    fun `happy path reports the included outcome without waiting`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { inBlockWatch() }

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain, target = subnetTarget) {})
        assertEquals(0L, testScheduler.currentTime)
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `missing backend origin does not skip local position invalidation after success`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        `when`(history.request(historyRequest.address, METAID)).thenReturn(null)
        extrinsics.build = { inBlockWatch() }

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain) {})
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history, org.mockito.Mockito.never()).ownTradeSubmitted(historyRequest, BittensorSettlementScope.Account)
    }

    @Test
    fun `failed backend context does not skip local invalidation after unknown execution`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        `when`(history.request(historyRequest.address, METAID)).thenThrow(IllegalStateException("backend unavailable"))
        extrinsics.build = { flow { awaitCancellation() } }

        assertEquals(BittensorOperationResult.Unknown(null), executor.execute(METAID, chain) {})
        verify(positions).invalidate(chain.id, coldKey, METAID)
    }

    @Test
    fun `history cache failure does not prevent position invalidation or change success`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { inBlockWatch() }
        doThrow(IllegalStateException("History cache unavailable")).`when`(history).ownTradeSubmitted(historyRequest, BittensorSettlementScope.Account)

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain) {})
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, BittensorSettlementScope.Account)
    }

    @Test
    fun `position cache failure does not prevent history invalidation or change success`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { inBlockWatch() }
        val chainId = chain.id
        val failure = IllegalStateException("Position cache unavailable")
        `when`(positions.invalidate(chainId, coldKey, METAID)).thenReturn(Result.failure(failure))

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain) {})
        verify(history).ownTradeSubmitted(historyRequest, BittensorSettlementScope.Account)
        verify(logger).log(failure)
    }

    @Test
    fun `cancellation after handoff invalidates affected data before leaving`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { flow { awaitCancellation() } }
        val submission = launch { executor.execute(METAID, chain, target = subnetTarget) {} }
        runCurrent()
        submission.cancelAndJoin()

        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `watch failure after handoff marks only the target network as settling`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { flow { throw IllegalStateException("Disconnected after submission") } }

        val result = executor.execute(METAID, chain, target = subnetTarget) {}

        assertEquals(BittensorOperationResult.Unknown(null), result)
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `signing outside the app is reported from the build until the signature is in`() = runTest {
        useWallet(LightMetaAccount.Type.LEDGER)
        val signed = CompletableDeferred<Unit>()
        extrinsics.build = {
            signed.await()
            inBlockWatch()
        }
        val observed = mutableListOf<Boolean>()
        val observing = launch { executor.signingOutsideApp.toList(observed) }

        val execution = async { executor.execute(METAID, chain) {} }
        runCurrent()
        assertTrue(executor.signingOutsideApp.value)

        signed.complete(Unit)
        assertEquals(BittensorOperationResult.Success(HASH), execution.await())
        assertFalse(executor.signingOutsideApp.value)
        observing.cancel()
        assertEquals(listOf(false, true, false), observed)
    }

    @Test
    fun `a key held in the app never reports signing outside the app`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        val signed = CompletableDeferred<Unit>()
        extrinsics.build = {
            signed.await()
            inBlockWatch()
        }

        val execution = async { executor.execute(METAID, chain) {} }
        runCurrent()
        assertFalse(executor.signingOutsideApp.value)

        signed.complete(Unit)
        assertEquals(BittensorOperationResult.Success(HASH), execution.await())
        assertFalse(executor.signingOutsideApp.value)
    }

    @Test
    fun `signing outside the app is cleared when the signing screen is cancelled`() = runTest {
        useWallet(LightMetaAccount.Type.LEDGER)
        extrinsics.build = { throw SigningCancelledException() }

        val result = executor.execute(METAID, chain) {} as BittensorOperationResult.Failure

        assertTrue(result.isSigningCancelled)
        assertFalse(executor.signingOutsideApp.value)
    }

    @Test
    fun `MEV success invalidates the operation account and subnet`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, onHandoff ->
            onHandoff()
            BittensorMevExecution.Executed(execution, null, BittensorMevCarrier.ABSENT)
        }

        assertEquals(BittensorOperationResult.Success(HASH), executeProtected())
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
        assertEquals(0, extrinsics.submissions)
    }

    @Test
    fun `MEV unknown result invalidates affected data without reporting a failure`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, onHandoff ->
            onHandoff()
            BittensorMevExecution.Unconfirmed(HASH)
        }

        assertEquals(BittensorOperationResult.Unknown(HASH), executeProtected())
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `MEV uncertainty invalidates before notifying the screen while finality is still pending`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        val notified = CompletableDeferred<Unit>()
        mevSubmission = { onUnconfirmed, onHandoff ->
            onHandoff()
            onUnconfirmed(HASH)
            awaitCancellation()
        }
        val pending = launch { executeProtected { hash ->
            assertEquals(HASH, hash)
            notified.complete(Unit)
        } }
        notified.await()

        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
        assertFalse(pending.isCompleted)
        pending.cancelAndJoin()
    }

    @Test
    fun `cancelling MEV after handoff invalidates affected data`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        val handedOff = CompletableDeferred<Unit>()
        mevSubmission = { _, onHandoff ->
            onHandoff()
            handedOff.complete(Unit)
            awaitCancellation()
        }
        val pending = launch { executeProtected() }
        handedOff.await()
        pending.cancelAndJoin()

        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `cancelling MEV readiness does not invalidate unchanged data`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, _ -> awaitCancellation() }
        val pending = launch { executeProtected() }
        runCurrent()
        pending.cancelAndJoin()

        verifyNoInteractions(positions)
        verify(history, org.mockito.Mockito.never()).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `MEV readiness failure remains not submitted without invalidation`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, _ -> throw BittensorMevNotReady() }

        val result = executeProtected() as BittensorOperationResult.Failure

        assertEquals(BittensorFailureStage.NOT_SUBMITTED, result.stage)
        verifyNoInteractions(positions)
        verify(history, org.mockito.Mockito.never()).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `MEV exception after handoff cannot become a not submitted failure`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, onHandoff ->
            onHandoff()
            throw SigningCancelledException()
        }

        assertEquals(BittensorOperationResult.Unknown(null), executeProtected())
        verify(positions).invalidate(chain.id, coldKey, METAID)
        verify(history).ownTradeSubmitted(historyRequest, subnetScope)
    }

    @Test
    fun `proven MEV non execution does not mark trade history as settling`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        mevSubmission = { _, onHandoff ->
            onHandoff()
            BittensorMevExecution.NotExecuted(BittensorMevCarrier.INCLUDED, HASH)
        }

        val result = executeProtected() as BittensorOperationResult.Failure

        assertEquals(BittensorFailureStage.NOT_EXECUTED, result.stage)
        verifyNoInteractions(positions)
        verify(history, org.mockito.Mockito.never()).ownTradeSubmitted(historyRequest, subnetScope)
    }

    private suspend fun executeProtected(onUnconfirmed: (String) -> Unit = {}): BittensorOperationResult = executor.execute(
        metaId = METAID,
        chain = chain,
        target = subnetTarget,
        mevProtection = true,
        onUnconfirmed = onUnconfirmed
    ) {}

    private suspend fun useWallet(type: LightMetaAccount.Type) {
        `when`(wallet.accountIdIn(chain)).thenReturn(coldKey)
        `when`(history.request(historyRequest.address, METAID)).thenReturn(historyRequest)
        `when`(wallet.type).thenReturn(type)
        `when`(accounts.getMetaAccount(METAID)).thenReturn(wallet)
    }

    // A leaked lock makes the follow-up execution wait forever; fail fast instead of hanging the test.
    private suspend fun assertLockReleased() {
        extrinsics.build = { inBlockWatch() }
        val result = withTimeout(LOCK_RELEASE_TIMEOUT_MS) { executor.execute(METAID, chain) {} }
        assertEquals(BittensorOperationResult.Success(HASH), result)
    }

    private fun inBlockWatch(): Flow<ExtrinsicWatchResult<ExtrinsicStatus>> =
        flowOf(watchResult(ExtrinsicStatus.Ready(HASH)), watchResult(ExtrinsicStatus.InBlock("0xblock", HASH)))

    private fun watchResult(status: ExtrinsicStatus) = ExtrinsicWatchResult(status, hierarchy)

    private class FakeExtrinsicService : ExtrinsicService by mock(ExtrinsicService::class.java) {

        var build: suspend () -> Flow<ExtrinsicWatchResult<ExtrinsicStatus>> = { error("Not configured") }

        var submissions = 0

        // Like the real service: everything until the returned flow is collected is building and signing.
        override suspend fun submitAndWatchExtrinsic(
            chain: Chain,
            origin: TransactionOrigin,
            submissionOptions: ExtrinsicService.SubmissionOptions,
            formExtrinsic: FormExtrinsicWithOrigin
        ): Result<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>> = runCatching {
            submissions++
            build()
        }
    }

    private companion object {
        const val HASH = "0x1234"
        const val METAID = 1L
        const val PROXY_METAID = 2L
        const val LOCK_RELEASE_TIMEOUT_MS = 1_000L
    }
}
