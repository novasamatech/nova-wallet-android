package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorClaimClock
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPendingClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.io.IOException
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.RETURNS_DEFAULTS

class BittensorRootClaimExecutionTest {

    private val clock = BittensorClaimClock { 0L }

    private val pending = BittensorPendingClaims(clock)

    private val chains = mock(ChainRegistry::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val source = mock(BittensorRootClaimSource::class.java)

    private var dryRuns = 0

    private var dryRunError: Throwable? = null

    private val extrinsics = mock(ExtrinsicService::class.java) { invocation ->
        if (invocation.method.name == "dryRunExtrinsic") {
            dryRuns++
            dryRunError?.let { throw it }
            io.novafoundation.nova.runtime.extrinsic.ExtrinsicDryRunResult.Success
        } else {
            RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val constants = mock(WalletConstants::class.java)

    private val immediate = mock(BittensorImmediateExecution::class.java)

    private var outcome: BittensorOperationResult = BittensorOperationResult.Success("hash")

    private var executorError: Throwable? = null

    private var submissions = 0

    private val executor = mock(BittensorSubmissionExecutor::class.java) { invocation ->
        if (invocation.method.name == "execute") {
            submissions++
            executorError?.let { throw it }
            outcome
        } else {
            RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val interactor = BittensorRootClaimInteractor(chains, accounts, wallets, source, extrinsics, executor, constants, immediate, pending)

    private val wallet = mock(MetaAccount::class.java)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val fee = mock(Fee::class.java)

    private val coldkey = AccountIdKey(ByteArray(32) { 1 })

    private val hotkey = AccountIdKey(ByteArray(32) { 2 })

    private val preview = BittensorRootClaimPreview(hotkey, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO)

    private val claims = BittensorRootClaims(listOf(preview), BigInteger.ONE)

    private val state = BittensorRootClaimState(1, coldkey, chain, asset, claims, true)

    private val operation = BittensorRootClaimOperation(hotkey)

    private val draft = BittensorRootClaimDraft(state, preview, operation, fee)

    @Test
    fun `button override shows real preview without enabling dry run or claim eligibility`() {
        val belowMinimum = claims.copy(minimum = BigInteger.valueOf(100))
        val displayState = state.copy(claims = belowMinimum, allowBelowMinimumPreview = true)
        assertEquals(preview, displayState.selectedPreview)
        assertTrue(displayState.availableClaims.isEmpty())
        assertTrue(!displayState.previewOnly)
        assertNull(displayState.copy(pendingHotkeys = setOf(hotkey)).selectedPreview)
    }

    @Test
    fun `preview shows below minimum and zero rewards without changing live eligibility`() {
        val belowMinimum = claims.copy(minimum = BigInteger.valueOf(100))
        val liveState = state.copy(claims = belowMinimum)
        assertNull(liveState.selectedPreview)
        assertEquals(preview, liveState.copy(previewOnly = true).selectedPreview)
        assertTrue(liveState.copy(previewOnly = true).availableClaims.isEmpty())

        val zero = preview.copy(accrued = BigInteger.ZERO, redeemable = BigInteger.ZERO)
        val zeroState = liveState.copy(claims = belowMinimum.copy(previews = listOf(zero)), previewOnly = true)
        assertEquals(zero, zeroState.selectedPreview)
        assertNull(zeroState.copy(pendingHotkeys = setOf(hotkey)).selectedPreview)
        assertNull(zeroState.copy(claims = belowMinimum.copy(previews = emptyList())).selectedPreview)
    }

    @Test
    fun `preview cannot acquire or execute even with a valid lease`() = runBlocking {
        configure()
        val previewDraft = draft.copy(state = state.copy(previewOnly = true))
        assertNull(interactor.tryAcquire(previewDraft))
        val lease = requireNotNull(interactor.tryAcquire(draft))

        val result = interactor.execute(previewDraft, lease)

        assertTrue(result is BittensorOperationResult.Failure)
        assertEquals(0, submissions)
        assertTrue(pending.pendingTargets(chain.id, coldkey).isEmpty())
    }

    private suspend fun configure() {
        `when`(chain.id).thenReturn("chain")
        `when`(wallet.id).thenReturn(1L)
        `when`(wallet.accountIdIn(chain)).thenReturn(coldkey.value)
        `when`(accounts.getMetaAccount(1)).thenReturn(wallet)
    }

    @Test
    fun `debug dry run accepts below minimum without submission or pending claims`() = runBlocking {
        configure()
        `when`(wallet.type).thenReturn(io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS)
        val debugState = state.copy(previewOnly = true, claims = claims.copy(minimum = BigInteger.valueOf(100)))

        assertEquals(io.novafoundation.nova.runtime.extrinsic.ExtrinsicDryRunResult.Success, interactor.dryRun(debugState))
        assertEquals(1, dryRuns)
        assertEquals(0, submissions)
        assertTrue(pending.pendingTargets(chain.id, coldkey).isEmpty())
    }

    @Test
    fun `dry run errors and cancellation never fall back to submission`() = runBlocking {
        configure()
        `when`(wallet.type).thenReturn(io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS)
        val errors = listOf(IOException("node unavailable"), CancellationException("cancelled"))
        for (error in errors) {
            dryRunError = error
            val actual = runCatching { interactor.dryRun(state.copy(previewOnly = true)) }.exceptionOrNull()
            assertEquals(error, actual)
        }
        assertEquals(0, submissions)
        assertTrue(pending.pendingTargets(chain.id, coldkey).isEmpty())
    }

    @Test
    fun `dry run requires debug payload and a regular signing wallet`() = runBlocking {
        configure()
        assertTrue(runCatching { interactor.dryRun(state) }.isFailure)
        val types = io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.values()
            .filterNot { it == io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS }
        for (type in types) {
            `when`(wallet.type).thenReturn(type)
            assertTrue(runCatching { interactor.dryRun(state.copy(previewOnly = true)) }.isFailure)
        }
        assertEquals(0, dryRuns)
        assertEquals(0, submissions)
    }

    @Test
    fun `success and dispatched or not submitted failures release handoff guard`() = runBlocking {
        configure()
        val results = listOf(
            BittensorOperationResult.Success("hash"),
            BittensorOperationResult.Failure(IOException(), stage = BittensorFailureStage.DISPATCHED),
            BittensorOperationResult.Failure(IOException(), stage = BittensorFailureStage.NOT_SUBMITTED)
        )
        for (result in results) {
            outcome = result
            val lease = requireNotNull(interactor.tryAcquire(draft))
            assertEquals(result, interactor.execute(draft, lease))
            assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
        }
        assertEquals(3, submissions)
    }

    @Test
    fun `proven signing cancellation releases lease for another review attempt`() = runBlocking {
        configure()
        val cancellation = io.novafoundation.nova.common.base.errors.SigningCancelledException()
        val cancellingExtrinsics = object : ExtrinsicService by extrinsics {
            override suspend fun submitExtrinsicAndAwaitExecution(
                chain: Chain,
                origin: TransactionOrigin,
                submissionOptions: ExtrinsicService.SubmissionOptions,
                formExtrinsic: FormExtrinsicWithOrigin
            ): Result<ExtrinsicExecutionResult> {
                return Result.failure(cancellation)
            }
        }
        val positionUpdates = BittensorPositionUpdates()
        val realExecutor = BittensorSubmissionExecutor(cancellingExtrinsics, positionUpdates)
        val cancellingInteractor = BittensorRootClaimInteractor(
            chains, accounts, wallets, source, cancellingExtrinsics, realExecutor, constants, immediate, pending
        )
        val lease = requireNotNull(cancellingInteractor.tryAcquire(draft))

        val result = cancellingInteractor.execute(draft, lease) as BittensorOperationResult.Failure

        assertTrue(result.isSigningCancelled)
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
        assertNotNull(cancellingInteractor.tryAcquire(draft))
    }

    @Test
    fun `unknown outcome prevents a second screen from acquiring same target`() = runBlocking {
        configure()
        outcome = BittensorOperationResult.Unknown(null)
        val lease = requireNotNull(interactor.tryAcquire(draft))
        assertNull(interactor.tryAcquire(draft))
        interactor.execute(draft, lease)
        assertNull(interactor.tryAcquire(draft))
        assertEquals(1, submissions)
    }

    @Test
    fun `pre submission wallet failure clears guard without calling executor`() = runBlocking {
        configure()
        `when`(wallet.accountIdIn(chain)).thenReturn(ByteArray(32) { 9 })
        val lease = requireNotNull(interactor.tryAcquire(draft))
        val result = interactor.execute(draft, lease) as BittensorOperationResult.Failure
        assertEquals(BittensorFailureStage.NOT_SUBMITTED, result.stage)
        assertNotNull(interactor.tryAcquire(draft))
        assertEquals(0, submissions)
    }

    @Test
    fun `executor exception remains unknown and retains target`() = runBlocking {
        configure()
        executorError = IOException("transport")
        val lease = requireNotNull(interactor.tryAcquire(draft))
        assertTrue(interactor.execute(draft, lease) is BittensorOperationResult.Unknown)
        assertNull(interactor.tryAcquire(draft))
    }

    @Test
    fun `cancellation after executor handoff retains target`() = runBlocking {
        configure()
        executorError = CancellationException("uncertain")
        val lease = requireNotNull(interactor.tryAcquire(draft))
        assertTrue(runCatching { interactor.execute(draft, lease) }.exceptionOrNull() is CancellationException)
        assertNull(interactor.tryAcquire(draft))
    }

    @Test
    fun `cancellation before executor handoff releases target`() = runBlocking {
        configure()
        `when`(accounts.getMetaAccount(1)).thenThrow(CancellationException("before handoff"))
        val lease = requireNotNull(interactor.tryAcquire(draft))
        assertTrue(runCatching { interactor.execute(draft, lease) }.exceptionOrNull() is CancellationException)
        assertNotNull(interactor.tryAcquire(draft))
        assertEquals(0, submissions)
    }

    @Test
    fun `pending targets do not replace rewards or hide another validator`() {
        val other = preview.copy(hotkey = AccountIdKey(ByteArray(32) { 3 }))
        val loaded = state.copy(claims = claims.copy(previews = listOf(preview, other)), pendingHotkeys = setOf(hotkey))
        assertEquals(other, loaded.selectedPreview)
        assertEquals(BigInteger.valueOf(20), loaded.claims.totalRedeemable)
        assertNull(loaded.copy(selectedHotkey = hotkey).selectedPreview)
    }
}
