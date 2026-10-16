package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetStakeOperation
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.math.BigInteger

data class BittensorSubnetStakeDraft(
    val metaId: Long,
    val validator: BittensorValidator,
    val operation: BittensorSubnetStakeOperation,
    val fee: Fee
)

class BittensorSubnetStakingInteractor(
    private val chainRegistry: ChainRegistry,
    private val accounts: AccountRepository,
    private val wallets: WalletRepository,
    private val validators: BittensorSubnetValidatorRepository,
    private val subnets: BittensorSubnetInteractor,
    private val portfolio: BittensorPortfolioRepository,
    private val extrinsics: ExtrinsicService,
    private val beneficiaryAddress: String,
    private val submission: BittensorSubmissionExecutor
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun rootValidators(): List<BittensorSubnetValidator> = validators.rootValidators(chainRegistry.getChain(ChainGeneses.BITTENSOR))

    suspend fun maximumStake(metaId: Long, subnetId: BittensorSubnetId, validatorAddress: String): BigInteger = withTimeout(60_000L) {
        requireSelectedWallet(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val asset = wallets.assetFlow(metaId, chain.utilityAsset).first()
        val validator = validators(subnetId).firstOrNull { it.address == validatorAddress && it.selectable }
        requireNotNull(validator) { "Validator is no longer eligible in this subnet" }
        val beneficiary = AccountIdKey(chain.accountIdOf(beneficiaryAddress))
        maximumBittensorStake(asset.transferableInPlanks) { amount ->
            val quote = subnets.quote(subnetId, amount)
            val operation = BittensorSubnetStakeOperation(quote, validator.validator.hotkey, beneficiary)
            extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(metaId), options) { operation.build(this) }
                .maxAmountDeductionFor(asset.token.configuration)
        }.also { requireSelectedWallet(metaId) }
    }

    suspend fun prepare(
        metaId: Long,
        subnetId: BittensorSubnetId,
        amount: BigInteger,
        validator: BittensorValidator
    ): BittensorSubnetStakeDraft = withTimeout(60_000L) {
        requireSelectedWallet(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        validators.validate(chain, subnetId, validator)
        val quote = subnets.quote(subnetId, amount)
        val operation = BittensorSubnetStakeOperation(quote, validator.hotkey, AccountIdKey(chain.accountIdOf(beneficiaryAddress)))
        val fee = extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(metaId), options) { operation.build(this) }
        requireAffordable(chain, metaId, amount, fee)
        BittensorSubnetStakeDraft(metaId, validator, operation, fee)
    }

    suspend fun execute(draft: BittensorSubnetStakeDraft): BittensorOperationResult {
        requireSelectedWallet(draft.metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        validators.validate(chain, draft.operation.quote.subnetId, draft.validator)
        // Recheck affordability, but never replace the user's confirmed price limit or amounts.
        val fee = extrinsics.estimateFee(chain, TransactionOrigin.WalletWithId(draft.metaId), options) { draft.operation.build(this) }
        requireAffordable(chain, draft.metaId, draft.operation.quote.totalTaoInPlanks, fee)
        val result = submission.execute(draft.metaId, chain) {
            draft.operation.build(this)
        }
        if (result is BittensorOperationResult.Success) portfolio.refresh()
        return result
    }

    private suspend fun requireSelectedWallet(metaId: Long) {
        val selected = accounts.getSelectedMetaAccount()
        check(selected.id == metaId) { "The staking wallet has changed" }
        requireBittensorImmediateWallet(selected)
    }

    suspend fun validators(subnetId: BittensorSubnetId) = validators.validators(chainRegistry.getChain(ChainGeneses.BITTENSOR), subnetId)

    private suspend fun requireAffordable(chain: Chain, metaId: Long, amount: BigInteger, fee: Fee) {
        val asset = wallets.assetFlow(metaId, chain.utilityAsset).first()
        require(amount + fee.maxAmountDeductionFor(asset.token.configuration) <= asset.transferableInPlanks) {
            "Insufficient balance for stake and network fee"
        }
        requireSelectedWallet(metaId)
    }
}
