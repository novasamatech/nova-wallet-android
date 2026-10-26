package io.novafoundation.nova.feature_staking_impl.domain.bittensor

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    private val chain = mock(Chain::class.java).also { `when`(it.id).thenReturn("bittensor") }

    private val account = mock(MetaAccount::class.java)

    private val wallet = mock(MetaAccount::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val hierarchy = SubmissionHierarchy(account, CallExecutionType.IMMEDIATE)

    private val execution = ExtrinsicExecutionResult(HASH, "0xblock", ExtrinsicDispatch.Ok(emptyList()), hierarchy)

    private val extrinsics = FakeExtrinsicService()

    private val executor = BittensorSubmissionExecutor(extrinsics, accounts, BittensorPositionUpdates()) { _, inBlock ->
        assertEquals(HASH, inBlock.status.extrinsicHash)
        execution
    }

    @Test
    fun `build that never completes is a retryable not submitted failure and releases the lock`() = runTest {
        useWallet(LightMetaAccount.Type.SECRETS)
        extrinsics.build = { awaitCancellation() }

        val result = executor.execute(METAID, chain) {} as BittensorOperationResult.Failure

        assertEquals(BITTENSOR_BUILD_AND_SIGN_TIMEOUT.inWholeMilliseconds, testScheduler.currentTime)
        assertEquals(BittensorFailureStage.NOT_SUBMITTED, result.stage)
        assertTrue(result.cause is BittensorBuildTimeoutException)
        assertTrue(result.isRetryable)
        assertFalse(result.isSigningCancelled)
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

        val result = executor.execute(METAID, chain) {}

        assertEquals(BittensorOperationResult.Unknown(HASH), result)
        assertEquals(BITTENSOR_SEND_AND_INCLUSION_TIMEOUT.inWholeMilliseconds, testScheduler.currentTime)
        assertEquals(1, extrinsics.submissions)
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

        assertEquals(BittensorOperationResult.Success(HASH), executor.execute(METAID, chain) {})
        assertEquals(0L, testScheduler.currentTime)
    }

    private suspend fun useWallet(type: LightMetaAccount.Type) {
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
