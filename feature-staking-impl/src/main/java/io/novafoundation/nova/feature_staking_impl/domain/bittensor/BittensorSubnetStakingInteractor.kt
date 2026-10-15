package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetStakeOperation
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

data class BittensorSubnetStakeDraft(
    val metaId: Long,
    val validator: BittensorValidator,
    val operation: BittensorSubnetStakeOperation,
    val fee: Fee
)

data class BittensorPreparedSubnetStake(val subnet: BittensorSubnet, val draft: BittensorSubnetStakeDraft)

class BittensorSubnetStakingInteractor(
    private val chainRegistry: ChainRegistry,
    private val wallets: WalletRepository,
    private val validators: BittensorSubnetValidatorRepository,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val extrinsics: ExtrinsicService,
    private val beneficiaryAddress: String
) {

    fun observeWallet(metaId: Long): Flow<BittensorWalletContext> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        wallets.assetFlow(metaId, chain.utilityAsset)
            .map { BittensorWalletContext(metaId, chain, it) }
    }

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun maximumStake(metaId: Long, subnetId: BittensorNetuid.Subnet, validatorAddress: String): BigInteger = withTimeout(60_000L) {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val asset = wallets.assetFlow(metaId, chain.utilityAsset)
            .first()
        if (maximumBittensorStake(asset.transferableInPlanks, BigInteger.ZERO).isZero) return@withTimeout BigInteger.ZERO
        val validator = requireEligibleValidator(chain, subnetId, validatorAddress)
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        val quote = bittensorSubnetInteractor.quote(subnetId, asset.transferableInPlanks)
        val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, beneficiary)
        val networkFee = estimateFee(chain, metaId, operation)
            .maxAmountDeductionFor(asset.token.configuration)
        val maximumStake = maximumBittensorStake(asset.transferableInPlanks, networkFee)
        maximumStake
    }

    suspend fun prepare(
        metaId: Long,
        subnetId: BittensorNetuid.Subnet,
        amount: BigInteger,
        validatorAddress: String
    ): BittensorPreparedSubnetStake = withTimeout(60_000L) {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val subnet = bittensorSubnetInteractor.subnet(subnetId)
        val validator = requireEligibleValidator(chain, subnetId, validatorAddress)
        val quote = bittensorSubnetInteractor.quote(subnetId, amount)
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, beneficiary)
        val fee = estimateFee(chain, metaId, operation)
        val draft = BittensorSubnetStakeDraft(metaId, validator, operation, fee)
        BittensorPreparedSubnetStake(subnet, draft)
    }

    suspend fun execute(draft: BittensorSubnetStakeDraft): Result<ExtrinsicExecutionResult> = runCatching {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        validators.validate(chain, draft.operation.quote.subnetId, draft.validator)
        extrinsics.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(draft.metaId), options) {
            draft.operation.build(this)
        }
            .requireOk()
            .getOrThrow()
    }
        .onFailure { if (it is CancellationException) throw it }

    suspend fun validationPayload(draft: BittensorSubnetStakeDraft): BittensorStakePreflightPayload {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val quote = draft.operation.quote
        val fee = estimateFee(chain, draft.metaId, draft.operation)
        return BittensorStakePreflightPayload(
            chain = chain,
            metaId = draft.metaId,
            netuid = quote.subnetId,
            hotkey = draft.validator.hotkey.value,
            amount = quote.totalTaoInPlanks,
            fee = fee,
            quote = quote
        )
    }

    private suspend fun estimateFee(chain: Chain, metaId: Long, operation: BittensorSubnetStakeOperation): Fee {
        return extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(metaId), options) {
            operation.build(this)
        }
    }

    private suspend fun requireEligibleValidator(chain: Chain, subnetId: BittensorNetuid.Subnet, address: String): BittensorValidator {
        val candidates = validators.validators(chain, subnetId)
        val eligibleValidator = candidates.firstOrNull { it.address == address && it.selectable }
        return requireNotNull(eligibleValidator) { "Validator is no longer eligible in this subnet" }.validator
    }

    suspend fun validators(subnetId: BittensorNetuid.Subnet): List<BittensorSubnetValidator> {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        return validators.validators(chain, subnetId)
    }
}
