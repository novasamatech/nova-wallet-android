package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.addBittensorRootStake
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class BittensorRootValidatorDetails(val validator: BittensorValidator, val annualPercentageYield: BigDecimal?)

class BittensorStakingInteractor(
    private val repository: BittensorStakingRepository,
    private val extrinsicService: ExtrinsicService,
    private val walletRepository: WalletRepository,
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val submissionExecutor: BittensorSubmissionExecutor
) {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> = repository.observeRootStakingDetails()

    fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails> = repository.observeRootStakingDetails(metaId)

    fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails> = repository.observeRootAccountDetails(metaId)

    suspend fun rootValidatorDetails(chain: Chain, address: String): BittensorRootValidatorDetails {
        val validator = rootValidator(chain, address)
        val annualPercentageYield = repository.annualPercentageYield(chain, validator)
        return BittensorRootValidatorDetails(validator, annualPercentageYield)
    }

    fun detailsForFixedValidator(
        details: BittensorRootStakingDetails,
        address: String,
        resolved: BittensorRootValidatorDetails?
    ): BittensorRootStakingDetails {
        val hotkey = AccountIdKey(details.chain.accountIdOf(address))
        return details.copy(
            validator = resolved?.validator,
            annualPercentageYield = resolved?.annualPercentageYield,
            position = details.positions.firstOrNull { it.validatorHotkey == hotkey }
        )
    }

    suspend fun detailsForValidator(details: BittensorRootStakingDetails, validator: BittensorValidator): BittensorRootStakingDetails {
        if (details.validator?.hotkey == validator.hotkey) return details.copy(validator = validator)
        return details.copy(
            validator = validator,
            annualPercentageYield = repository.annualPercentageYield(details.chain, validator),
            position = details.positions.firstOrNull { it.validatorHotkey == validator.hotkey }
        )
    }

    suspend fun rootValidator(chain: Chain, address: String): BittensorValidator {
        val validatorRepository = validatorRepository.rootValidators(chain)
        val selectedValidator = validatorRepository
            .firstOrNull { it.address == address && it.selectable }
        return requireNotNull(selectedValidator) { "Validator is no longer eligible for root staking" }
            .validator
    }

    suspend fun maximumStake(details: BittensorRootStakingDetails): BigInteger {
        val validator = requireNotNull(details.validator) { "Select a root validator" }
        val asset = walletRepository.assetFlow(details.metaId, details.chain.utilityAsset)
            .first()
        if (maximumBittensorStake(asset.transferableInPlanks, BigInteger.ZERO).isZero) {
            return BigInteger.ZERO
        }
        val request = BittensorRootStakeRequest(
            details.metaId,
            details.chain,
            validator,
            asset.transferableInPlanks
        )
        val networkFee = estimateRootStakeFee(request)
            .maxAmountDeductionFor(asset.token.configuration)
        val maximum = maximumBittensorStake(asset.transferableInPlanks, networkFee)
        return maximum
    }

    suspend fun estimateRootStakeFee(request: BittensorRootStakeRequest): Fee {
        return extrinsicService.estimateFee(
            request.chain,
            TransactionOrigin.WalletWithId(request.metaId)
        ) {
            addBittensorRootStake(request.validator.hotkey.value, request.amountInPlanks)
        }
    }

    suspend fun executeRootStake(request: BittensorRootStakeRequest): BittensorOperationResult {
        validatorRepository.validateRoot(request.chain, request.validator)
        return submissionExecutor.execute(
            request.metaId,
            request.chain,
            BittensorOutcomeTarget(BittensorOperationKind.STAKE, BittensorNetuid.Root, setOf(request.validator.hotkey))
        ) {
            addBittensorRootStake(request.validator.hotkey.value, request.amountInPlanks)
        }
    }

    fun validationPayload(request: BittensorRootStakeRequest, fee: Fee) = BittensorStakePreflightPayload(
        chain = request.chain,
        metaId = request.metaId,
        netuid = BittensorNetuid.Root,
        hotkey = request.validator.hotkey.value,
        amount = request.amountInPlanks,
        fee = fee
    )
}
