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
import kotlinx.coroutines.flow.first

class BittensorStakingInteractor(
    private val repository: BittensorStakingRepository,
    private val extrinsicService: ExtrinsicService,
    private val walletRepository: WalletRepository,
    private val validatorRepository: BittensorSubnetValidatorRepository
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> = repository.observeRootStakingDetails()

    fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails> = repository.observeRootStakingDetails(metaId)

    suspend fun rootValidator(chain: Chain, address: String): BittensorValidator {
        val validators = validatorRepository.rootValidators(chain)
        val selectedValidator = validators
            .firstOrNull { it.address == address && it.selectable }
        return requireNotNull(selectedValidator) { "Validator is no longer eligible for root staking" }.validator
    }

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

    suspend fun executeRootStake(request: BittensorRootStakeRequest): Result<ExtrinsicExecutionResult> = runCatching {
        validatorRepository.validateRoot(request.chain, request.validator)
        extrinsicService.submitExtrinsicAndAwaitExecution(request.chain, TransactionOrigin.WalletWithId(request.metaId)) {
            addBittensorRootStake(request.validator.hotkey.value, request.amountInPlanks)
        }
            .requireOk()
            .getOrThrow()
    }
        .onFailure { if (it is CancellationException) throw it }

    fun validationPayload(request: BittensorRootStakeRequest, fee: Fee, checkRootHold: Boolean = false) = BittensorStakePreflightPayload(
        chain = request.chain,
        metaId = request.metaId,
        netuid = BittensorNetuid.Root,
        hotkey = request.validator.hotkey.value,
        amount = request.amountInPlanks,
        fee = fee,
        checkRootHold = checkRootHold
    )
}
