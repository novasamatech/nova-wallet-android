package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
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
    private val walletRepository: WalletRepository,
    private val validatorRepository: BittensorSubnetValidatorRepository
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> {
        return combine(repository.observeRootStakingDetails(), flowState.selectedValidator) { details, selectedValidator ->
            if (selectedValidator?.metaId == details.metaId) details.copy(validator = selectedValidator.validator) else details
        }
    }

    suspend fun selectValidator(details: BittensorRootStakingDetails, address: String) {
        val selectedValidator = validatorRepository.rootValidators(details.chain)
            .firstOrNull { it.address == address && it.selectable }
        requireNotNull(selectedValidator) { "Validator is no longer eligible for root staking" }
        flowState.selectedValidator.value = BittensorStakingFlowState.SelectedValidator(details.metaId, selectedValidator.validator)
    }

    fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails> = repository.observeRootStakingDetails(metaId)

    fun observeStakeDraft(): Flow<BittensorRootStakeDraft?> = flowState.draft

    fun hasDraft(): Boolean = flowState.draft.value != null

    suspend fun maximumStake(details: BittensorRootStakingDetails): BigInteger {
        val validator = requireNotNull(details.validator) { "Select a root validator" }
        val asset = walletRepository.assetFlow(details.metaId, details.chain.utilityAsset)
            .first()
        if (maximumBittensorStake(asset.transferableInPlanks, BigInteger.ZERO).isZero) return BigInteger.ZERO
        val request = BittensorRootStakeRequest(details.metaId, details.chain, validator, asset.transferableInPlanks)
        val networkFee = estimateRootStakeFee(request)
            .maxAmountDeductionFor(asset.token.configuration)
        val maximum = maximumBittensorStake(asset.transferableInPlanks, networkFee)
        return maximum
    }

    suspend fun estimateRootStakeFee(request: BittensorRootStakeRequest): Fee {
        return extrinsicService.estimateFee(request.chain, TransactionOrigin.WalletWithId(request.metaId)) {
            addBittensorRootStake(request.validator.hotkey.value, request.amountInPlanks)
        }
    }

    suspend fun prepareRootStake(request: BittensorRootStakeRequest): BittensorRootStakeDraft {
        validatorRepository.validateRoot(request.chain, request.validator)
        val fee = estimateRootStakeFee(request)
        val draft = BittensorRootStakeDraft(request.metaId, request.amountInPlanks, request.validator, fee)
        flowState.setDraft(draft)
        return draft
    }

    suspend fun executeRootStake(chain: Chain, draft: BittensorRootStakeDraft): Result<ExtrinsicExecutionResult> = runCatching {
        validatorRepository.validateRoot(chain, draft.validator)
        val execution = extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(draft.metaId)) {
            addBittensorRootStake(draft.validator.hotkey.value, draft.amountInPlanks)
        }.requireOk().getOrThrow()
        flowState.clear()
        execution
    }.onFailure { if (it is CancellationException) throw it }

    fun validationPayload(request: BittensorRootStakeRequest, fee: Fee, checkRootHold: Boolean = false) = BittensorStakePreflightPayload(
        chain = request.chain,
        metaId = request.metaId,
        netuid = BittensorNetuid.Root,
        hotkey = request.validator.hotkey.value,
        amount = request.amountInPlanks,
        fee = fee,
        checkRootHold = checkRootHold
    )

    fun clearDraft() = flowState.clear()
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
