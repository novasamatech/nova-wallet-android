package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import java.math.BigDecimal
import java.math.BigInteger

data class BittensorStakeValidationPayload(
    val asset: Asset,
    val amount: BigDecimal,
    val fee: Fee,
    val preflight: BittensorStakePreflight,
    val quote: BittensorSubnetBuyQuote?,
    val netuid: BittensorNetuid,
    val priceImpactAccepted: Boolean,
    val checkedAtMillis: Long,
    val latestQuote: BittensorSubnetBuyQuote? = null,
    val inactiveValidator: Boolean = false,
    val inactiveValidatorAccepted: Boolean = false
)

sealed interface BittensorStakeValidationFailure {

    data object DELAYED_EXECUTION : BittensorStakeValidationFailure

    data object NON_POSITIVE_AMOUNT : BittensorStakeValidationFailure

    data object INSUFFICIENT_BALANCE : BittensorStakeValidationFailure

    data object FEE_RESERVE : BittensorStakeValidationFailure

    /** [requiredAmountInPlanks] is the smallest entered amount that passes, including any fees taken before staking. */
    data class BelowMinimumStake(val requiredAmountInPlanks: BigInteger) : BittensorStakeValidationFailure

    data object HOTKEY_NOT_REGISTERED : BittensorStakeValidationFailure

    data object SUBNET_STAKING_DISABLED : BittensorStakeValidationFailure

    data object COLDKEY_SWAP_PENDING : BittensorStakeValidationFailure

    data object SAFE_MODE_ACTIVE : BittensorStakeValidationFailure

    data object QUOTE_UNAVAILABLE : BittensorStakeValidationFailure

    data object QUOTE_EXPIRED : BittensorStakeValidationFailure

    data object SLIPPAGE_EXCEEDED : BittensorStakeValidationFailure

    data object HIGH_PRICE_IMPACT : BittensorStakeValidationFailure

    data object INACTIVE_VALIDATOR : BittensorStakeValidationFailure
}

typealias BittensorStakeValidationSystem = ValidationSystem<BittensorStakeValidationPayload, BittensorStakeValidationFailure>

typealias BittensorStakeValidation = Validation<BittensorStakeValidationPayload, BittensorStakeValidationFailure>

fun bittensorStakeValidationSystem(validations: List<BittensorStakeValidation>): BittensorStakeValidationSystem = ValidationSystem {
    positiveAmount(amount = { it.amount }, error = { BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT })
    sufficientBalance(
        fee = { it.fee },
        amount = { it.amount },
        available = { it.asset.transferable },
        error = { BittensorStakeValidationFailure.INSUFFICIENT_BALANCE }
    )
    validations.forEach(::validate)
}
