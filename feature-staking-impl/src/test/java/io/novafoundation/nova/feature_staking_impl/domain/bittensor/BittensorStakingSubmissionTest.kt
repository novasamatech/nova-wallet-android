package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetStakeOperation
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicWatchException
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@RunWith(Parameterized::class)
class BittensorStakingSubmissionTest(private val stakingType: StakingType) {

    enum class StakingType { ROOT, SUBNET }

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val selectedAccount = mock(MetaAccount::class.java)

    private val fee = mock(Fee::class.java)

    private val submissionHierarchy = SubmissionHierarchy(selectedAccount, CallExecutionType.IMMEDIATE)

    private val successfulExecution = ExtrinsicExecutionResult(
        "0x1234",
        "0xabcd",
        ExtrinsicDispatch.Ok(emptyList()),
        submissionHierarchy
    )

    private val unusedExtrinsicService = mock(ExtrinsicService::class.java)

    private val extrinsicService = RecordingExtrinsicService(unusedExtrinsicService, fee, Result.success(successfulExecution))

    private val positionUpdates = BittensorPositionUpdates()

    private val submission = BittensorSubmissionExecutor(extrinsicService, positionUpdates)

    private val validator = BittensorValidator(
        AccountIdKey(ByteArray(32) { 1 }),
        "Validator"
    )

    private val amount = BigInteger.valueOf(1000)

    private val rootDetails = BittensorRootStakingDetails(
        1L,
        chain,
        asset,
        validator,
        null,
        BittensorDataStatus.Fresh
    )

    private val rootRepository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = flowOf(rootDetails)

        override fun observeRootStakingDetails(metaId: Long) = flowOf(rootDetails)

        override fun observeRootAccountDetails(metaId: Long) = flowOf(rootDetails)
    }

    private val rootValidators = mock(BittensorSubnetValidatorRepository::class.java)

    private val rootInteractor = BittensorStakingInteractor(
        rootRepository,
        extrinsicService,
        wallets,
        rootValidators,
        submission
    )

    private val subnetValidators = mock(BittensorSubnetValidatorRepository::class.java)

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val costBasis = mock(BittensorCostBasisInteractor::class.java)

    private val subnetStakingInteractor = BittensorSubnetStakingInteractor(
        chainRegistry,
        wallets,
        subnetValidators,
        subnetInteractor,
        extrinsicService,
        "5EAjYyJ8JaxyvHTSNuFp8fqjerTETjqhoM7MdZux2Ehzjxdq",
        submission,
        costBasis
    )

    private val subnetId = BittensorNetuid.Subnet(1)

    private val quote = BittensorSubnetBuyQuote(
        subnetId,
        amount,
        BigInteger.ZERO,
        BittensorSwapSimulation(
            subnetId,
            BittensorSwapDirection.TAO_TO_ALPHA,
            amount,
            BigInteger.TEN,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO
        ),
        BigInteger.valueOf(1005),
        BigDecimal.ZERO,
        BigInteger.valueOf(1000),
        System.currentTimeMillis()
    )

    private val beneficiary = AccountIdKey(ByteArray(32) { 2 })

    private val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, beneficiary)

    private val subnetDraft = BittensorSubnetStakeDraft(1L, validator, operation)

    @Before
    fun prepare(): Unit = runBlocking {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(selectedAccount.id).thenReturn(1L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selectedAccount)
        `when`(chainRegistry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.valueOf(10000))
        `when`(wallets.assetFlow(1L, chainAsset)).thenReturn(flowOf(asset))
        `when`(fee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger.TEN)
    }

    @Test
    fun `validation uses the displayed fee without estimating again`() = runBlocking {
        val payload = when (stakingType) {
            StakingType.ROOT -> {
                val request = BittensorRootStakeRequest(
                    1L,
                    chain,
                    validator,
                    amount
                )
                rootInteractor.validationPayload(request, fee)
            }
            StakingType.SUBNET -> subnetStakingInteractor.validationPayload(subnetDraft, fee)
        }

        assertSame(fee, payload.fee)
        assertEquals(0, extrinsicService.feeEstimates)
        if (stakingType == StakingType.SUBNET) assertSame(quote, payload.quote)
    }

    @Test
    fun `fee estimation uses the operation wallet and does not refresh the quote`() = runBlocking {
        val estimatedFee = when (stakingType) {
            StakingType.ROOT -> {
                val request = BittensorRootStakeRequest(
                    1L,
                    chain,
                    validator,
                    amount
                )
                rootInteractor.estimateRootStakeFee(request)
            }
            StakingType.SUBNET -> subnetStakingInteractor.estimateFee(subnetDraft)
        }

        assertSame(fee, estimatedFee)
        assertEquals(1, extrinsicService.feeEstimates)
        assertEquals(
            1L,
            (extrinsicService.feeOrigin as TransactionOrigin.WalletWithId).metaId
        )
        assertEquals(BatchMode.BATCH_ALL, extrinsicService.feeOptions?.batchMode)
        verifyNoInteractions(subnetInteractor, accounts)
    }

    @Test
    fun `successful execution uses standard submission with explicit operation parameters`() = runBlocking {
        assertEquals(BittensorOperationResult.Success(successfulExecution.extrinsicHash), execute())
        assertEquals(1, extrinsicService.submissions)
        assertEquals(
            1L,
            (extrinsicService.submissionOrigin as TransactionOrigin.WalletWithId).metaId
        )
        assertEquals(BatchMode.BATCH_ALL, extrinsicService.submissionOptions?.batchMode)
        assertSame(chain, extrinsicService.submissionChain)
        if (stakingType == StakingType.ROOT) {
            verify(rootValidators)
                .validateRoot(chain, validator)
        } else {
            verify(subnetValidators)
                .validate(chain, subnetId, validator)
            verifyNoInteractions(subnetInteractor)
        }
    }

    @Test
    fun `submission error is unknown without a persistent lock`() = runBlocking {
        val failure = IOException("Connection lost")
        extrinsicService.result = Result.failure(failure)

        assertEquals(BittensorOperationResult.Unknown(null), execute())
        assertEquals(1, extrinsicService.submissions)

        extrinsicService.result = Result.success(successfulExecution)
        assertEquals(BittensorOperationResult.Success(successfulExecution.extrinsicHash), execute())
        assertEquals(2, extrinsicService.submissions)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `only confirmed execution publishes a position and history refresh`() = runTest {
        val notifications = mutableListOf<BittensorPositionUpdates.Update>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            positionUpdates.updates.collect { notifications += it }
        }
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Unknown))
        execute()
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Failed(DispatchError.Token)))
        execute()
        val delayed = SubmissionHierarchy(selectedAccount, CallExecutionType.DELAYED)
        extrinsicService.result = Result.success(successfulExecution.copy(submissionHierarchy = delayed))
        execute()
        assertTrue(notifications.isEmpty())
        extrinsicService.result = Result.success(successfulExecution)
        execute()
        assertEquals(1, notifications.size)
        assertEquals(1L, notifications.single().metaId)
        assertEquals(ChainGeneses.BITTENSOR, notifications.single().chainId)
    }

    @Test
    fun `unknown execution outcome is not reported as success`() = runTest {
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Unknown))

        assertEquals(BittensorOperationResult.Unknown(successfulExecution.extrinsicHash), execute())
        assertEquals(1, extrinsicService.submissions)
    }

    @Test
    fun `watch failure preserves hash and is not retryable failure`() = runTest {
        val status = ExtrinsicStatus.Dropped(successfulExecution.extrinsicHash)
        val error = ExtrinsicWatchException(status)
        extrinsicService.result = Result.failure(error)

        assertEquals(BittensorOperationResult.Unknown(successfulExecution.extrinsicHash), execute())
    }

    @Test
    fun `nested immediate outer success is not completed trade`() = runTest {
        val node = SubmissionHierarchy.Node(selectedAccount, CallExecutionType.IMMEDIATE)
        val hierarchy = SubmissionHierarchy(listOf(node, node))
        extrinsicService.result = Result.success(successfulExecution.copy(submissionHierarchy = hierarchy))

        assertEquals(BittensorOperationResult.Unknown(successfulExecution.extrinsicHash), execute())
    }

    @Test
    fun `delayed approval outer success is not completed trade`() = runTest {
        val hierarchy = SubmissionHierarchy(selectedAccount, CallExecutionType.DELAYED)
        extrinsicService.result = Result.success(successfulExecution.copy(submissionHierarchy = hierarchy))

        assertEquals(BittensorOperationResult.Unknown(successfulExecution.extrinsicHash), execute())
    }

    @Test
    fun `on chain failure is propagated`() = runBlocking {
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Failed(DispatchError.Token)))

        assertSame(
            DispatchError.Token,
            (execute() as BittensorOperationResult.Failure).cause
        )
        assertEquals(1, extrinsicService.submissions)
    }

    @Test
    fun `executor attaches actual amounts for exact requested wallet context`() = runTest {
        val coldkey = ByteArray(32) { 9 }
        `when`(selectedAccount.id).thenReturn(1L)
        `when`(selectedAccount.accountIdIn(chain)).thenReturn(coldkey)
        val target = BittensorOutcomeTarget(BittensorOperationKind.STAKE, BittensorNetuid.Root, setOf(validator.hotkey))
        val added = outcomeEvent("SubtensorModule", "StakeAdded", listOf(coldkey, validator.hotkey.value, amount, amount, BigInteger.ZERO, BigInteger.ZERO))
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Ok(listOf(added))))

        val result = submission.execute(1L, chain, target) {} as BittensorOperationResult.Success

        assertEquals(amount, result.outcome?.tao)
        assertEquals(successfulExecution.blockHash, result.outcome?.blockHash)
        val wrongWallet = submission.execute(2L, chain, target) {} as BittensorOperationResult.Success
        org.junit.Assert.assertNull(wrongWallet.outcome)
    }

    @Test
    fun `failed dispatch keeps paid fee events but never executed amounts`() = runTest {
        val coldkey = ByteArray(32) { 9 }
        `when`(selectedAccount.id).thenReturn(1L)
        `when`(selectedAccount.accountIdIn(chain)).thenReturn(coldkey)
        val target = BittensorOutcomeTarget(BittensorOperationKind.STAKE, BittensorNetuid.Root, setOf(validator.hotkey))
        val feeEvent = outcomeEvent("TransactionPayment", "TransactionFeePaid", listOf(coldkey, BigInteger.TEN, BigInteger.ZERO))
        val added = outcomeEvent("SubtensorModule", "StakeAdded", listOf(coldkey, validator.hotkey.value, amount, amount, BigInteger.ZERO, BigInteger.ZERO))
        val dispatch = ExtrinsicDispatch.Failed(DispatchError.Token, listOf(feeEvent, added))
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = dispatch))

        val result = submission.execute(1L, chain, target) {} as BittensorOperationResult.Failure

        assertEquals(BigInteger.TEN, result.outcome?.networkFee)
        assertEquals(AccountIdKey(coldkey), result.outcome?.feePayer)
        org.junit.Assert.assertNull(result.outcome?.tao)
        org.junit.Assert.assertNull(result.outcome?.alpha)
    }

    @Test
    fun `root aggregate fallback requires confirmed execution and cannot override delayed execution`() = runTest {
        val coldkey = ByteArray(32) { 9 }
        `when`(selectedAccount.id).thenReturn(1L)
        `when`(selectedAccount.accountIdIn(chain)).thenReturn(coldkey)
        val target = BittensorOutcomeTarget(BittensorOperationKind.CLAIM, BittensorNetuid.Root, setOf(validator.hotkey))
        val claimed = outcomeEvent("SubtensorModule", "RootClaimed", listOf(coldkey, amount))
        val dispatch = ExtrinsicDispatch.Ok(listOf(claimed))
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = dispatch))
        val confirmed = submission.execute(1L, chain, target) {} as BittensorOperationResult.Success
        assertEquals(amount, confirmed.outcome?.tao)

        val delayed = SubmissionHierarchy(selectedAccount, CallExecutionType.DELAYED)
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = dispatch, submissionHierarchy = delayed))
        assertEquals(BittensorOperationResult.Unknown(successfulExecution.extrinsicHash), submission.execute(1L, chain, target) {})

        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Failed(DispatchError.Token, listOf(claimed))))
        val failed = submission.execute(1L, chain, target) {} as BittensorOperationResult.Failure
        org.junit.Assert.assertNull(failed.outcome?.tao)
    }

    private fun outcomeEvent(moduleName: String, name: String, arguments: List<Any?>): GenericEvent.Instance {
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn(moduleName)
        val event = Event(name, index = 0 to 0, arguments = emptyList(), documentation = emptyList())
        return GenericEvent.Instance(module, event, arguments)
    }

    @Test
    fun `wallet switch keeps the original submission wallet`() = runBlocking {
        `when`(selectedAccount.id).thenReturn(2L)

        assertTrue(execute() is BittensorOperationResult.Success)
        assertEquals(1, extrinsicService.submissions)
        assertEquals(
            1L,
            (extrinsicService.submissionOrigin as TransactionOrigin.WalletWithId).metaId
        )
        verifyNoInteractions(accounts)
    }

    @Test
    fun `cancellation is propagated without converting it to failure`() = runBlocking {
        val cancellation = kotlinx.coroutines.CancellationException("Cancelled")
        extrinsicService.result = Result.failure(cancellation)

        try {
            execute()
            org.junit.Assert.fail("Expected cancellation")
        } catch (error: kotlinx.coroutines.CancellationException) {
            assertSame(cancellation, error)
        }
    }

    @Test
    fun `additional subnet stake requires accepted inactivity warning before signing`() = runBlocking {
        org.junit.Assume.assumeTrue(stakingType == StakingType.SUBNET)
        val row = BittensorSubnetValidator(validator, "address", BigDecimal.ZERO, false, inactive = true, canAddToExistingPosition = true)
        `when`(subnetValidators.existingPositionValidator(chain, subnetId, validator)).thenReturn(row)
        val existing = subnetDraft.copy(existingPosition = true)

        val rejected = runCatching { subnetStakingInteractor.execute(existing) }
        assertTrue(rejected.exceptionOrNull() is IllegalArgumentException)
        verifyNoInteractions(costBasis)

        assertTrue(subnetStakingInteractor.execute(existing.copy(inactiveValidatorAccepted = true)) is BittensorOperationResult.Success)
        verify(subnetValidators, org.mockito.Mockito.times(2)).existingPositionValidator(chain, subnetId, validator)
        verify(subnetValidators, org.mockito.Mockito.never()).validate(chain, subnetId, validator)
    }

    private suspend fun execute(): BittensorOperationResult = when (stakingType) {
        StakingType.ROOT -> rootInteractor.executeRootStake(
            BittensorRootStakeRequest(
                1L,
                chain,
                validator,
                amount
            )
        )
        StakingType.SUBNET -> subnetStakingInteractor.execute(subnetDraft)
    }

    private class RecordingExtrinsicService(
        delegate: ExtrinsicService,
        private val fee: Fee,
        var result: Result<ExtrinsicExecutionResult>
    ) : ExtrinsicService by delegate {

        var submissions = 0

        var submissionOrigin: TransactionOrigin? = null

        var submissionChain: Chain? = null

        var submissionOptions: ExtrinsicService.SubmissionOptions? = null

        var feeOptions: ExtrinsicService.SubmissionOptions? = null

        var feeEstimates = 0

        var feeOrigin: TransactionOrigin? = null

        override suspend fun estimateFee(
            chain: Chain,
            origin: TransactionOrigin,
            submissionOptions: ExtrinsicService.SubmissionOptions,
            formExtrinsic: FormExtrinsicWithOrigin
        ): Fee {
            feeEstimates++
            feeOrigin = origin
            feeOptions = submissionOptions
            return fee
        }

        override suspend fun submitExtrinsicAndAwaitExecution(
            chain: Chain,
            origin: TransactionOrigin,
            submissionOptions: ExtrinsicService.SubmissionOptions,
            formExtrinsic: FormExtrinsicWithOrigin
        ): Result<ExtrinsicExecutionResult> {
            submissions++
            submissionOrigin = origin
            submissionChain = chain
            this.submissionOptions = submissionOptions
            return result
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun stakingTypes(): List<StakingType> = StakingType.entries
    }
}
