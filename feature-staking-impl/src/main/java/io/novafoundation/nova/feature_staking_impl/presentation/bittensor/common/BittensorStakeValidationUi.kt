package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationKind
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorRetainedForFees
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.postTradePrice
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import java.math.BigInteger
import java.math.RoundingMode

private const val PRICE_IMPACT_SCALE = 6

/** Formats the values the iOS stake alerts name; [requiredAmount] rounds up like iOS `formatRequiredAmount`. */
class BittensorStakeAlertFormat(
    val amount: (BigInteger) -> CharSequence,
    val requiredAmount: (BigInteger) -> CharSequence,
    val rate: (BittensorSubnetBuyQuote) -> CharSequence = { "" }
)

fun TokenFormatter.bittensorStakeAlertFormat(token: Token, rate: (BittensorSubnetBuyQuote) -> CharSequence = { "" }) = BittensorStakeAlertFormat(
    amount = { formatToken(token.amountFromPlanks(it), token.configuration.symbol) },
    requiredAmount = { formatBittensorRequiredAmount(it, token) },
    rate = rate
)

fun BittensorStakePreflightFailure.titleAndMessage(resources: ResourceManager, format: BittensorStakeAlertFormat): TitleAndMessage {
    fun error(message: Int) = resources.getString(R.string.common_error_general_title) to resources.getString(message)
    fun alert(title: Int, message: Int) = resources.getString(title) to resources.getString(message)
    return when (failure) {
        // As on iOS: the same wording for root and subnets, naming the amount the user has to enter.
        is BittensorStakeValidationFailure.BelowMinimumStake -> resources.getString(R.string.common_amount_low) to resources.getString(
            R.string.bittensor_validation_minimum_stake,
            format.requiredAmount(failure.requiredAmountInPlanks)
        )
        // As on iOS: Ledger gets the signer-not-supported copy, proxied and delayed multisig wallets the unsupported-wallet copy.
        BittensorStakeValidationFailure.LEDGER_NOT_SUPPORTED ->
            resources.getString(R.string.account_parity_signer_not_supported_title) to resources.getString(R.string.ledger_sign_raw_not_supported)
        BittensorStakeValidationFailure.PROXIED_NOT_SUPPORTED -> resources.getString(
            R.string.bittensor_staking_wallet_unsupported_title,
            resources.getString(R.string.account_proxied)
        ) to resources.getString(R.string.bittensor_staking_wallet_unsupported_message)
        BittensorStakeValidationFailure.DELAYED_EXECUTION -> resources.getString(
            R.string.bittensor_staking_wallet_unsupported_title,
            resources.getString(R.string.account_multisig)
        ) to resources.getString(R.string.bittensor_staking_wallet_unsupported_message)
        BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT -> error(R.string.common_enter_amount)
        BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> error(R.string.common_error_not_enough_tokens)
        BittensorStakeValidationFailure.FEE_RESERVE -> {
            val reserve = format.amount(bittensorRetainedForFees(BigInteger.ZERO))
            resources.getString(R.string.bittensor_fee_reserve_title, reserve) to
                resources.getString(R.string.bittensor_fee_reserve_message, format.amount(maximumStake.orZero()), reserve)
        }
        BittensorStakeValidationFailure.HOTKEY_NOT_REGISTERED -> alert(R.string.bittensor_validation_hotkey_title, R.string.bittensor_validation_hotkey)
        BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED ->
            alert(R.string.bittensor_validation_staking_disabled_title, R.string.bittensor_validation_staking_disabled)
        BittensorStakeValidationFailure.COLDKEY_SWAP_PENDING ->
            alert(R.string.bittensor_validation_coldkey_swap_title, R.string.bittensor_validation_coldkey_swap)
        BittensorStakeValidationFailure.SAFE_MODE_ACTIVE -> alert(R.string.bittensor_validation_safe_mode_title, R.string.bittensor_validation_safe_mode)
        BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> resources.getString(R.string.swap_not_enought_liquidity_failure) to null
        BittensorStakeValidationFailure.QUOTE_EXPIRED -> error(R.string.bittensor_validation_quote_expired)
        BittensorStakeValidationFailure.SELF_IMPACT -> alert(R.string.bittensor_self_impact_title, R.string.bittensor_self_impact_message)
        BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED -> resources.getString(R.string.swap_rate_was_updated_failure_title) to resources.getString(
            R.string.bittensor_rate_updated_message,
            quote.formatRate(format),
            latestQuote.formatRate(format)
        )
        // As on iOS, the impact of the quote the warning was raised for.
        BittensorStakeValidationFailure.HIGH_PRICE_IMPACT -> resources.getString(R.string.bittensor_price_impact_title) to resources.getString(
            R.string.bittensor_price_impact_message,
            (latestQuote ?: quote).formatPriceImpact()
        )
        BittensorStakeValidationFailure.INACTIVE_VALIDATOR -> alert(R.string.bittensor_inactive_validator_title, R.string.bittensor_inactive_validator_message)
    }
}

/** iOS SubtensorValidationReporter kinds: a missing or unreadable quote reports liquidity, a self-impacting order its own kind. */
fun BittensorStakePreflightFailure.analyticsKind(): StakingValidationKind? = when (failure) {
    BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> StakingValidationKind.LIQUIDITY
    BittensorStakeValidationFailure.SELF_IMPACT -> StakingValidationKind.ORDER_TOO_LARGE
    else -> failure.analyticsKind()
}

private fun BittensorSubnetBuyQuote?.formatRate(format: BittensorStakeAlertFormat): CharSequence = this?.let(format.rate) ?: "—"

// iOS NumberFormatter.percentSingle: the pool price move against spot, at most two fraction digits rounded down.
private fun BittensorSubnetBuyQuote?.formatPriceImpact(): String {
    val impact = this?.let {
        (it.postTradePrice() - it.spotPrice).abs().toBigDecimal().divide(it.spotPrice.toBigDecimal(), PRICE_IMPACT_SCALE, RoundingMode.DOWN)
    }
    return impact.orZero().movePointRight(2).formatAsPercentage()
}

fun bittensorStakeValidationFailureUi(
    failure: ValidationStatus.NotValid<BittensorStakePreflightFailure>,
    actions: ValidationFlowActions<BittensorStakePreflightPayload>,
    resources: ResourceManager,
    format: BittensorStakeAlertFormat,
    onOutcome: (StakingValidationOutcome) -> Unit = {},
    acceptRate: () -> Unit,
    refreshQuote: () -> Unit
): TransformedFailure {
    val (title, message) = failure.reason.titleAndMessage(resources, format)
    fun proceed(label: Int, action: () -> Unit) = CustomDialogDisplayer.Payload.DialogAction(resources.getString(label)) {
        onOutcome(StakingValidationOutcome.PROCEEDED)
        action()
    }
    val cancel = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
        onOutcome(StakingValidationOutcome.CANCELLED)
    }
    val dialog = when (failure.reason.failure) {
        BittensorStakeValidationFailure.INACTIVE_VALIDATOR -> CustomDialogDisplayer.Payload(
            title,
            message,
            okAction = proceed(R.string.common_continue) { actions.revalidate { it.copy(inactiveValidatorAccepted = true) } },
            cancelAction = cancel
        )
        BittensorStakeValidationFailure.HIGH_PRICE_IMPACT -> CustomDialogDisplayer.Payload(
            title,
            message,
            okAction = proceed(R.string.common_proceed) { actions.revalidate { it.copy(priceImpactAccepted = true) } },
            cancelAction = cancel
        )
        // iOS presentRateUpdated: Confirm takes the new rate.
        BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED ->
            CustomDialogDisplayer.Payload(title, message, okAction = proceed(R.string.common_confirm, acceptRate), cancelAction = cancel)
        BittensorStakeValidationFailure.QUOTE_EXPIRED ->
            CustomDialogDisplayer.Payload(title, message, okAction = proceed(R.string.bittensor_review_updated_quote, refreshQuote), cancelAction = cancel)
        else -> {
            onOutcome(StakingValidationOutcome.BLOCKED)
            return TransformedFailure.Default(title to message)
        }
    }
    return TransformedFailure.Custom(dialog)
}
