package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import java.math.BigDecimal

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

enum class BittensorStakeValidationFailure {
    DELAYED_EXECUTION,
    NON_POSITIVE_AMOUNT,
    INSUFFICIENT_BALANCE,
    FEE_RESERVE,
    BELOW_MINIMUM_STAKE,
    HOTKEY_NOT_REGISTERED,
    SUBNET_STAKING_DISABLED,
    COLDKEY_SWAP_PENDING,
    SAFE_MODE_ACTIVE,
    QUOTE_UNAVAILABLE,
    QUOTE_EXPIRED,
    SLIPPAGE_EXCEEDED,
    HIGH_PRICE_IMPACT,
    INACTIVE_VALIDATOR
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
