package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
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
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val submissionHierarchy = SubmissionHierarchy(emptyList())

    private val successfulExecution = ExtrinsicExecutionResult("0x1234", "0xabcd", ExtrinsicDispatch.Ok(emptyList()), submissionHierarchy)

    private val unusedExtrinsicService = mock(ExtrinsicService::class.java)

    private val extrinsicService = RecordingExtrinsicService(unusedExtrinsicService, fee, Result.success(successfulExecution))

    private val flowState = BittensorStakingFlowState()

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private val amount = BigInteger.valueOf(1000)

    private val rootDetails = BittensorRootStakingDetails(1L, chain, asset, validator, null, BittensorDataStatus.Fresh)

    private val rootRepository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = flowOf(rootDetails)

        override fun observeRootStakingDetails(metaId: Long) = flowOf(rootDetails)
    }

    private val rootValidators = mock(BittensorSubnetValidatorRepository::class.java)

    private val rootInteractor = BittensorStakingInteractor(rootRepository, extrinsicService, flowState, wallets, rootValidators)

    private val subnetValidators = mock(BittensorSubnetValidatorRepository::class.java)

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val subnetStakingInteractor = BittensorSubnetStakingInteractor(
        chainRegistry,
        wallets,
        subnetValidators,
        subnetInteractor,
        extrinsicService,
        "5EAjYyJ8JaxyvHTSNuFp8fqjerTETjqhoM7MdZux2Ehzjxdq"
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
    fun prepare() = runBlocking {
        `when`(selectedAccount.id).thenReturn(1L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selectedAccount)
        `when`(chainRegistry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.valueOf(10000))
        `when`(wallets.assetFlow(1L, chainAsset)).thenReturn(flowOf(asset))
        `when`(fee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger.TEN)
        flowState.setDraft(BittensorRootStakeDraft(1L, amount, validator, fee))
    }

    @Test
    fun `validation uses the displayed fee without estimating again`() = runBlocking {
        val payload = when (stakingType) {
            StakingType.ROOT -> {
                val request = BittensorRootStakeRequest(1L, chain, validator, amount)
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
                val request = BittensorRootStakeRequest(1L, chain, validator, amount)
                rootInteractor.estimateRootStakeFee(request)
            }
            StakingType.SUBNET -> subnetStakingInteractor.estimateFee(subnetDraft)
        }

        assertSame(fee, estimatedFee)
        assertEquals(1, extrinsicService.feeEstimates)
        assertEquals(1L, (extrinsicService.feeOrigin as TransactionOrigin.WalletWithId).metaId)
        assertEquals(BatchMode.BATCH_ALL, extrinsicService.feeOptions?.batchMode)
        verifyNoInteractions(subnetInteractor, accounts)
    }

    @Test
    fun `successful execution uses standard submission and clears the root draft`() = runBlocking {
        assertSame(successfulExecution, execute().getOrThrow())
        assertEquals(1, extrinsicService.submissions)
        assertEquals(1L, (extrinsicService.submissionOrigin as TransactionOrigin.WalletWithId).metaId)
        assertEquals(BatchMode.BATCH_ALL, extrinsicService.submissionOptions?.batchMode)
        assertSame(chain, extrinsicService.submissionChain)
        if (stakingType == StakingType.ROOT) {
            verify(rootValidators).validateRoot(chain, validator)
            assertFalse(rootInteractor.hasDraft())
        } else {
            verify(subnetValidators).validate(chain, subnetId, validator)
            verifyNoInteractions(subnetInteractor)
        }
    }

    @Test
    fun `network failure is returned without an automatic resubmission or persistent lock`() = runBlocking {
        val failure = IOException("Connection lost")
        extrinsicService.result = Result.failure(failure)

        assertSame(failure, execute().exceptionOrNull())
        assertEquals(1, extrinsicService.submissions)
        assertDraftPreserved()

        extrinsicService.result = Result.success(successfulExecution)
        assertSame(successfulExecution, execute().getOrThrow())
        assertEquals(2, extrinsicService.submissions)
    }

    @Test
    fun `unknown execution outcome is not reported as success`() = runBlocking {
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Unknown))

        assertTrue(execute().isFailure)
        assertEquals(1, extrinsicService.submissions)
        assertDraftPreserved()
    }

    @Test
    fun `on chain failure is propagated without clearing the draft`() = runBlocking {
        extrinsicService.result = Result.success(successfulExecution.copy(outcome = ExtrinsicDispatch.Failed(DispatchError.Token)))

        assertSame(DispatchError.Token, execute().exceptionOrNull())
        assertEquals(1, extrinsicService.submissions)
        assertDraftPreserved()
    }

    @Test
    fun `wallet switch keeps the original submission wallet`() = runBlocking {
        `when`(selectedAccount.id).thenReturn(2L)

        assertTrue(execute().isSuccess)
        assertEquals(1, extrinsicService.submissions)
        assertEquals(1L, (extrinsicService.submissionOrigin as TransactionOrigin.WalletWithId).metaId)
        verifyNoInteractions(accounts)
    }

    private suspend fun execute(): Result<ExtrinsicExecutionResult> = when (stakingType) {
        StakingType.ROOT -> rootInteractor.executeRootStake(chain, BittensorRootStakeDraft(1L, amount, validator, fee))
        StakingType.SUBNET -> subnetStakingInteractor.execute(subnetDraft)
    }

    private fun assertDraftPreserved() {
        if (stakingType == StakingType.ROOT) assertTrue(rootInteractor.hasDraft())
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
