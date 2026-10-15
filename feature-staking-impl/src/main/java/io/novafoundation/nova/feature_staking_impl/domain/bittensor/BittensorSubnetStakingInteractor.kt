package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
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
import kotlinx.coroutines.flow.first
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
    private val accounts: AccountRepository,
    private val wallets: WalletRepository,
    private val validators: BittensorSubnetValidatorRepository,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val extrinsics: ExtrinsicService,
    private val beneficiaryAddress: String
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun maximumStake(metaId: Long, subnetId: BittensorNetuid.Subnet, validatorAddress: String): BigInteger = withTimeout(60_000L) {
        requireSelectedWallet(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val asset = wallets.assetFlow(metaId, chain.utilityAsset).first()
        val validator = requireEligibleValidator(chain, subnetId, validatorAddress)
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        val maximumStake = maximumBittensorStake(asset.transferableInPlanks) { amount ->
            val quote = bittensorSubnetInteractor.quote(subnetId, amount)
            val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, beneficiary)
            extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(metaId), options) { operation.build(this) }
                .maxAmountDeductionFor(asset.token.configuration)
        }
        requireSelectedWallet(metaId)
        maximumStake
    }

    suspend fun prepare(
        metaId: Long,
        subnetId: BittensorNetuid.Subnet,
        amount: BigInteger,
        validatorAddress: String
    ): BittensorPreparedSubnetStake = withTimeout(60_000L) {
        requireSelectedWallet(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val subnet = bittensorSubnetInteractor.subnet(subnetId)
        val validator = requireEligibleValidator(chain, subnetId, validatorAddress)
        val quote = bittensorSubnetInteractor.quote(subnetId, amount)
        val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, AccountIdKey(chain.accountIdOf(beneficiaryAddress)))
        val fee = extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(metaId), options) { operation.build(this) }
        requireAffordable(chain, metaId, amount, fee)
        BittensorPreparedSubnetStake(subnet, BittensorSubnetStakeDraft(metaId, validator, operation, fee))
    }

    suspend fun execute(draft: BittensorSubnetStakeDraft): Result<ExtrinsicExecutionResult> = runCatching {
        requireSelectedWallet(draft.metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        validators.validate(chain, draft.operation.quote.subnetId, draft.validator)
        // Recheck affordability, but never replace the user's confirmed price limit or amounts.
        val fee = extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(draft.metaId), options) { draft.operation.build(this) }
        requireAffordable(chain, draft.metaId, draft.operation.quote.totalTaoInPlanks, fee)
        extrinsics.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(draft.metaId), options) {
            draft.operation.build(this)
        }.requireOk().getOrThrow()
    }.onFailure { if (it is CancellationException) throw it }

    private suspend fun requireEligibleValidator(chain: Chain, subnetId: BittensorNetuid.Subnet, address: String): BittensorValidator {
        val eligibleValidator = validators.validators(chain, subnetId).firstOrNull { it.address == address && it.selectable }
        return requireNotNull(eligibleValidator) { "Validator is no longer eligible in this subnet" }.validator
    }

    private suspend fun requireSelectedWallet(metaId: Long) {
        check(accounts.getSelectedMetaAccount().id == metaId) { "The staking wallet has changed" }
    }

    suspend fun validators(subnetId: BittensorNetuid.Subnet) = validators.validators(chainRegistry.getChain(ChainGeneses.BITTENSOR), subnetId)

    private suspend fun requireAffordable(chain: Chain, metaId: Long, amount: BigInteger, fee: Fee) {
        val asset = wallets.assetFlow(metaId, chain.utilityAsset).first()
        require(amount + fee.maxAmountDeductionFor(asset.token.configuration) <= asset.transferableInPlanks) {
            "Insufficient balance for stake and network fee"
        }
        requireSelectedWallet(metaId)
    }
}
