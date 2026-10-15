package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.addBittensorRootStake
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class BittensorStakingInteractor(
    private val repository: BittensorStakingRepository,
    private val extrinsicService: ExtrinsicService,
    private val flowState: BittensorStakingFlowState,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val validatorRepository: BittensorSubnetValidatorRepository
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> {
        return combine(repository.observeRootStakingDetails(), flowState.selectedValidator) { details, selectedValidator ->
            if (selectedValidator?.metaId == details.metaId) details.copy(validator = selectedValidator.validator) else details
        }
    }

    suspend fun selectValidator(details: BittensorRootStakingDetails, address: String) {
        requireSelectedWallet(details.metaId)
        val selectedValidator = validatorRepository.rootValidators(details.chain)
            .firstOrNull { it.address == address && it.selectable }
        requireNotNull(selectedValidator) { "Validator is no longer eligible for root staking" }
        requireSelectedWallet(details.metaId)
        flowState.selectedValidator.value = BittensorStakingFlowState.SelectedValidator(details.metaId, selectedValidator.validator)
    }

    fun observeStakeDraft(): Flow<BittensorRootStakeDraft?> = flowState.draft

    fun hasDraft(): Boolean = flowState.draft.value != null

    suspend fun maximumStake(details: BittensorRootStakingDetails): BigInteger {
        requireSelectedWallet(details.metaId)
        val validator = requireNotNull(details.validator) { "Select a root validator" }
        val asset = walletRepository.assetFlow(details.metaId, details.chain.utilityAsset).first()
        val maximum = maximumBittensorStake(asset.transferableInPlanks) { amount ->
            val request = BittensorRootStakeRequest(details.metaId, details.chain, validator, amount)
            estimateRootStakeFee(request).maxAmountDeductionFor(asset.token.configuration)
        }
        requireSelectedWallet(details.metaId)
        return maximum
    }

    suspend fun estimateRootStakeFee(request: BittensorRootStakeRequest): Fee {
        requireSelectedWallet(request.metaId)
        return extrinsicService.estimateFee(request.chain, TransactionOrigin.WalletWithId(request.metaId)) {
            addBittensorRootStake(request.validator.hotkey.value, request.amountInPlanks)
        }
    }

    suspend fun prepareRootStake(request: BittensorRootStakeRequest): BittensorRootStakeDraft {
        requireSelectedWallet(request.metaId)
        validatorRepository.validateRoot(request.chain, request.validator)
        val fee = estimateRootStakeFee(request)
        requireAffordable(request.chain, request.metaId, request.amountInPlanks, fee)
        val draft = BittensorRootStakeDraft(request.metaId, request.amountInPlanks, request.validator, fee)
        flowState.setDraft(draft)
        return draft
    }

    suspend fun executeRootStake(chain: Chain, draft: BittensorRootStakeDraft): Result<ExtrinsicExecutionResult> = runCatching {
        requireSelectedWallet(draft.metaId)
        validatorRepository.validateRoot(chain, draft.validator)
        val request = BittensorRootStakeRequest(draft.metaId, chain, draft.validator, draft.amountInPlanks)
        val fee = estimateRootStakeFee(request)
        requireAffordable(chain, draft.metaId, draft.amountInPlanks, fee)
        val execution = extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(draft.metaId)) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }.requireOk().getOrThrow()
        flowState.clear()
        execution
    }.onFailure { if (it is CancellationException) throw it }

    fun clearDraft() = flowState.clear()

    private suspend fun requireSelectedWallet(metaId: Long) {
        check(accountRepository.getSelectedMetaAccount().id == metaId) { "The staking wallet has changed" }
    }

    private suspend fun requireAffordable(chain: Chain, metaId: Long, amount: BigInteger, fee: Fee) {
        val asset = walletRepository.assetFlow(metaId, chain.utilityAsset).first()
        require(amount + fee.maxAmountDeductionFor(asset.token.configuration) <= asset.transferableInPlanks) {
            "Insufficient transferable balance"
        }
        requireSelectedWallet(metaId)
    }
}

class BittensorStakingFlowState {

    internal data class SelectedValidator(val metaId: Long, val validator: BittensorValidator)

    internal val selectedValidator = MutableStateFlow<SelectedValidator?>(null)

    internal val draft = MutableStateFlow<BittensorRootStakeDraft?>(null)

    fun setDraft(draft: BittensorRootStakeDraft) {
        this.draft.value = draft
    }

    fun clear() {
        draft.value = null
        selectedValidator.value = null
    }
}
