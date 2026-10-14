package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.addBittensorRootStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.combine
import java.math.BigInteger

class BittensorStakingInteractor(
    private val repository: BittensorStakingRepository,
    private val extrinsicService: ExtrinsicService,
    private val flowState: BittensorStakingFlowState,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val validators: BittensorSubnetValidatorRepository
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> {
        return combine(repository.observeRootStakingDetails(), flowState.selectedValidator) { details, selected ->
            if (selected?.metaId == details.metaId) details.copy(validator = selected.validator) else details
        }.onEach(flowState::setDetails)
    }

    suspend fun selectValidator(address: String) {
        val details = requireNotNull(flowState.latestDetails) { "Root staking details are not loaded" }
        requireSelectedWallet(details.metaId)
        val selected = validators.rootValidators(details.chain).firstOrNull { it.address == address && it.selectable }
        requireNotNull(selected) { "Validator is no longer eligible for root staking" }
        requireSelectedWallet(details.metaId)
        flowState.selectedValidator.value = BittensorStakingFlowState.SelectedValidator(details.metaId, selected.validator)
    }

    fun observeStakeDraft(): Flow<BittensorRootStakeDraft> = flowState.draft.filterNotNull()

    fun hasDraft(): Boolean = flowState.draft.value != null

    suspend fun estimateRootStakeFee(amountInPlanks: BigInteger): Fee {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        val validator = requireNotNull(details.validator) { "Select a root validator" }

        return estimateRootStakeFee(details, validator, amountInPlanks)
    }

    private suspend fun estimateRootStakeFee(details: BittensorRootStakingDetails, validator: BittensorValidator, amountInPlanks: BigInteger): Fee {
        return extrinsicService.estimateFee(details.chain, TransactionOrigin.WalletWithId(details.metaId)) {
            addBittensorRootStake(validator.hotkey.value, amountInPlanks)
        }
    }

    suspend fun prepareRootStake(amountInPlanks: BigInteger): BittensorRootStakeDraft {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        requireSelectedWallet(details.metaId)
        val validator = requireNotNull(details.validator) { "Select a root validator" }
        validators.validateRoot(details.chain, validator)
        val fee = estimateRootStakeFee(details, validator, amountInPlanks)
        requireSelectedWallet(details.metaId)

        val feeDeduction = fee.maxAmountDeductionFor(details.asset.token.configuration)
        require(amountInPlanks + feeDeduction <= details.asset.transferableInPlanks) { "Insufficient transferable balance" }
        return BittensorRootStakeDraft(details.metaId, amountInPlanks, validator, fee).also(flowState::setDraft)
    }

    suspend fun executeRootStake(): Result<ExtrinsicExecutionResult> {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        val draft = requireNotNull(flowState.draft.value) { "Bittensor stake draft is missing" }

        require(details.metaId == draft.metaId) { "The staking wallet has changed" }
        requireSelectedWallet(draft.metaId)
        validators.validateRoot(details.chain, draft.validator)
        val asset = walletRepository.assetFlow(draft.metaId, details.chain.utilityAsset).first()
        val fee = extrinsicService.estimateFee(details.chain, TransactionOrigin.WalletWithId(draft.metaId)) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }
        require(draft.amountInPlanks + fee.maxAmountDeductionFor(asset.token.configuration) <= asset.transferableInPlanks) {
            "Insufficient transferable balance"
        }
        requireSelectedWallet(draft.metaId)
        return extrinsicService.submitExtrinsicAndAwaitExecution(details.chain, TransactionOrigin.WalletWithId(draft.metaId)) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }.requireOk()
            .onSuccess {
                repository.refreshPosition()
                flowState.clear()
            }
    }

    fun clearDraft() {
        flowState.clear()
    }

    private suspend fun requireSelectedWallet(metaId: Long) {
        check(accountRepository.getSelectedMetaAccount().id == metaId) { "The staking wallet has changed" }
    }

    private suspend fun Flow<BittensorRootStakingDetails>.firstAndRemember(): BittensorRootStakingDetails {
        return first().also(flowState::setDetails)
    }
}

class BittensorStakingFlowState {

    internal data class SelectedValidator(val metaId: Long, val validator: BittensorValidator)

    internal val selectedValidator = MutableStateFlow<SelectedValidator?>(null)

    internal val draft = MutableStateFlow<BittensorRootStakeDraft?>(null)

    @Volatile
    internal var latestDetails: BittensorRootStakingDetails? = null
        private set

    fun setDetails(details: BittensorRootStakingDetails) {
        latestDetails = details
    }

    fun setDraft(draft: BittensorRootStakeDraft) {
        this.draft.value = draft
    }

    fun clear() {
        draft.value = null
        selectedValidator.value = null
        latestDetails = null
    }
}
