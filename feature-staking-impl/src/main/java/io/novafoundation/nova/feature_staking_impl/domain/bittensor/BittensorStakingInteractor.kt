package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSelection
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.addBittensorRootStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class BittensorStakingInteractor(
    private val repository: BittensorStakingRepository,
    private val extrinsicService: ExtrinsicService,
    private val flowState: BittensorStakingFlowState,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val submission: BittensorSubmissionExecutor,
    private val validators: BittensorSubnetValidatorRepository,
    private val selection: BittensorRootValidatorSelection
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> {
        return repository.observeRootStakingDetails().onEach(flowState::setDetails)
    }

    suspend fun selectRootValidator(address: String) {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        val validator = validators.rootValidators(details.chain).firstOrNull { it.address == address && it.selectable }
        requireNotNull(validator) { "Root validator is unavailable" }
        selection.select(address)
    }

    fun observeStakeDraft(): Flow<BittensorRootStakeDraft> = flowState.draft.filterNotNull()

    fun hasDraft(): Boolean = flowState.draft.value != null

    suspend fun estimateRootStakeFee(amountInPlanks: BigInteger): Fee {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()

        return extrinsicService.estimateFee(details.chain, TransactionOrigin.WalletWithId(details.metaId)) {
            addBittensorRootStake(details.validator.hotkey.value, amountInPlanks)
        }
    }

    suspend fun prepareRootStake(amountInPlanks: BigInteger): BittensorRootStakeDraft {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        requireSelectedWallet(details.metaId)
        require(selection.address.value == null || selection.address.value == details.chain.addressOf(details.validator.hotkey.value)) {
            "The selected validator is still loading. Review the stake again"
        }
        validators.validateRoot(details.chain, details.validator)
        val fee = estimateRootStakeFee(amountInPlanks)
        requireSelectedWallet(details.metaId)

        val feeDeduction = fee.maxAmountDeductionFor(details.asset.token.configuration)
        require(amountInPlanks + feeDeduction <= details.asset.transferableInPlanks) { "Insufficient transferable balance" }
        return BittensorRootStakeDraft(details.metaId, amountInPlanks, details.validator, fee).also(flowState::setDraft)
    }

    suspend fun executeRootStake(): BittensorOperationResult {
        val details = flowState.latestDetails ?: repository.observeRootStakingDetails().firstAndRemember()
        val draft = requireNotNull(flowState.draft.value) { "Bittensor stake draft is missing" }

        require(details.metaId == draft.metaId) { "The staking wallet has changed" }
        require(details.validator.hotkey == draft.validator.hotkey) { "The selected root validator changed. Review the stake again" }
        requireSelectedWallet(draft.metaId)
        require(selection.address.value == null || selection.address.value == details.chain.addressOf(draft.validator.hotkey.value)) {
            "The selected root validator changed. Review the stake again"
        }
        validators.validateRoot(details.chain, draft.validator)
        val asset = walletRepository.assetFlow(draft.metaId, details.chain.utilityAsset).first()
        val fee = extrinsicService.estimateFee(details.chain, TransactionOrigin.WalletWithId(draft.metaId)) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }
        require(draft.amountInPlanks + fee.maxAmountDeductionFor(asset.token.configuration) <= asset.transferableInPlanks) {
            "Insufficient transferable balance"
        }
        requireSelectedWallet(draft.metaId)
        val result = submission.execute(draft.metaId, details.chain) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }
        if (result is BittensorOperationResult.Success) {
            repository.refreshPosition()
            flowState.clear()
        }
        return result
    }

    fun clearDraft() {
        flowState.clear()
    }

    private suspend fun requireSelectedWallet(metaId: Long) {
        val selected = accountRepository.getSelectedMetaAccount()
        check(selected.id == metaId) { "The staking wallet has changed" }
        requireBittensorImmediateWallet(selected)
    }

    private suspend fun Flow<BittensorRootStakingDetails>.firstAndRemember(): BittensorRootStakingDetails {
        return first().also(flowState::setDetails)
    }
}

class BittensorStakingFlowState {

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
        latestDetails = null
    }
}
