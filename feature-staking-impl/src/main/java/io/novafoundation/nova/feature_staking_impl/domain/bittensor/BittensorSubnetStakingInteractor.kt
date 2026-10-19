package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

data class BittensorSubnetStakeDraft(
    val metaId: Long,
    val validator: BittensorValidator,
    val operation: BittensorSubnetStakeOperation
)

data class BittensorPreparedSubnetStake(val subnet: BittensorSubnet, val draft: BittensorSubnetStakeDraft)

class BittensorSubnetStakingInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val extrinsicService: ExtrinsicService,
    private val beneficiaryAddress: String,
    private val submissionExecutor: BittensorSubmissionExecutor,
    private val costBasisInteractor: BittensorCostBasisInteractor
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun validatorCount(subnetId: BittensorNetuid.Subnet): Int {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        return validatorRepository.validatorCount(chain, subnetId)
    }

    fun observeWallet(metaId: Long): Flow<BittensorWalletContext> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        walletRepository.assetFlow(metaId, chain.utilityAsset).map { BittensorWalletContext(metaId, chain, it) }
    }

    suspend fun maximumStake(
        metaId: Long,
        subnetId: BittensorNetuid.Subnet,
        validatorAddress: String,
        slippage: BittensorSlippage = bittensorSubnetInteractor.slippage
    ): BigInteger = withTimeout(60_000L) {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val asset = walletRepository.assetFlow(metaId, chain.utilityAsset)
            .first()
        if (maximumBittensorStake(asset.transferableInPlanks, BigInteger.ZERO).isZero) {
            return@withTimeout BigInteger.ZERO
        }
        val validator = requireEligibleValidator(chain, subnetId, validatorAddress)
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        val quote = bittensorSubnetInteractor.quote(subnetId, asset.transferableInPlanks, slippage)
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
        validatorAddress: String,
        slippage: BittensorSlippage = bittensorSubnetInteractor.slippage
    ): BittensorPreparedSubnetStake = withTimeout(60_000L) {
        val subnet = bittensorSubnetInteractor.subnet(subnetId)
        val quote = bittensorSubnetInteractor.quote(subnetId, amount, slippage)
        val draft = prepareDraft(metaId, quote, validatorAddress)
        BittensorPreparedSubnetStake(subnet, draft)
    }

    suspend fun prepareDraft(
        metaId: Long,
        quote: BittensorSubnetBuyQuote,
        validatorAddress: String
    ): BittensorSubnetStakeDraft {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val validator = requireEligibleValidator(chain, quote.subnetId, validatorAddress)
        return previewDraft(metaId, quote, validator)
    }

    /** A display/fee draft from an already checked preset; review and submission revalidate eligibility. */
    suspend fun previewDraft(metaId: Long, quote: BittensorSubnetBuyQuote, validator: BittensorValidator): BittensorSubnetStakeDraft {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, beneficiary)
        return BittensorSubnetStakeDraft(metaId, validator, operation)
    }

    suspend fun execute(draft: BittensorSubnetStakeDraft): BittensorOperationResult {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        validatorRepository.validate(chain, draft.operation.quote.subnetId, draft.validator)
        val historyRequest = costBasisInteractor.ownTradeRequest(draft.metaId, chain)
        val result = submissionExecutor.execute(
            draft.metaId,
            chain,
            BittensorOutcomeTarget(
                BittensorOperationKind.STAKE,
                draft.operation.quote.subnetId,
                setOf(draft.operation.hotkey),
                draft.operation.beneficiary
            )
        ) { draft.operation.build(this) }
        costBasisInteractor.onTradeResult(historyRequest, result)
        return result
    }

    suspend fun validationPayload(draft: BittensorSubnetStakeDraft, fee: Fee): BittensorStakePreflightPayload {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val quote = draft.operation.quote
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

    suspend fun estimateFee(draft: BittensorSubnetStakeDraft): Fee {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        return estimateFee(chain, draft.metaId, draft.operation)
    }

    private suspend fun estimateFee(chain: Chain, metaId: Long, operation: BittensorSubnetStakeOperation): Fee {
        return extrinsicService.estimateFee(
            chain,
            TransactionOrigin.WalletWithId(metaId),
            options
        ) {
            operation.build(this)
        }
    }

    private suspend fun requireEligibleValidator(chain: Chain, subnetId: BittensorNetuid.Subnet, address: String): BittensorValidator {
        val candidates = validatorRepository.validators(chain, subnetId)
        val eligibleValidator = candidates.firstOrNull { it.address == address && it.selectable }
        return requireNotNull(eligibleValidator) { "Validator is no longer eligible in this subnet" }
            .validator
    }

    suspend fun validators(subnetId: BittensorNetuid.Subnet): List<BittensorSubnetValidator> {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        return validatorRepository.validators(chain, subnetId)
    }
}
