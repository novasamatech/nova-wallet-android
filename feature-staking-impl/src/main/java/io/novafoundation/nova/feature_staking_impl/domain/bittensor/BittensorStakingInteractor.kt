package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
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
    private val submission: BittensorSubmissionExecutor
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> {
        return repository.observeRootStakingDetails().onEach(flowState::setDetails)
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
        requireSelectedWallet(draft.metaId)
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
        check(accountRepository.getSelectedMetaAccount().id == metaId) { "The staking wallet has changed" }
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
