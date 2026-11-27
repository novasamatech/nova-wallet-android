package io.novafoundation.nova.feature_swap_impl.data.exchanger

import io.novafoundation.nova.common.utils.integerDigits
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteArgs
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountPrecisionExceeded
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAPayLimits
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteResult
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val RESPONSE_AMOUNT_PATTERN = Regex("(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,36})?")

internal fun mapExchangerAQuoteRequest(args: ExchangerAQuoteArgs, constraints: ExchangerAAmountConstraints): ExchangerAQuoteRequest {
    val assetIn = args.assetIn
    val assetOut = args.assetOut
    require(assetIn.fullId != assetOut.fullId) { "Exchanger A requires different assets" }

    val specifiedAsset = when (args.swapDirection) {
        SwapDirection.SPECIFIED_IN -> assetIn
        SwapDirection.SPECIFIED_OUT -> assetOut
    }
    val amount = specifiedAsset.amountFromPlanks(args.amount).stripTrailingZeros()
    require(amount.isPositive) { "Exchanger A quote amount must be positive" }
    val limits = constraints.forAsset(specifiedAsset)
    if (amount.scale() > limits.maxFractionDigits || amount.integerDigits > limits.maxIntegerDigits) {
        throw ExchangerAAmountPrecisionExceeded(limits)
    }
    val amountString = amount.toPlainString()

    val from = ExchangerAAssetReference(assetIn.chainId, assetIn.id)
    val to = ExchangerAAssetReference(assetOut.chainId, assetOut.id)
    return when (args.swapDirection) {
        SwapDirection.SPECIFIED_IN -> ExchangerAQuoteRequest.pay(from, to, amountString)
        SwapDirection.SPECIFIED_OUT -> ExchangerAQuoteRequest.receive(from, to, amountString)
    }
}

internal fun mapExchangerAQuote(remote: ExchangerAQuoteRemote, args: ExchangerAQuoteArgs): ExchangerAQuoteResult {
    val limits = mapLimits(remote.limits, args.assetIn)
    return when (remote.kind) {
        "quote" -> mapQuote(remote, args, limits)
        "out_of_range" -> ExchangerAQuoteResult.OutOfRange(args, limits, mapLimitReason(remote.reason))
        else -> invalidQuote("kind")
    }
}

private fun mapQuote(
    remote: ExchangerAQuoteRemote,
    args: ExchangerAQuoteArgs,
    limits: ExchangerAPayLimits
): ExchangerAQuoteResult.Quote {
    val assetIn = args.assetIn
    val assetOut = args.assetOut
    val amountIn = mapAmount(remote.payAmount, assetIn, "payAmount", allowZero = false)
    val amountOut = mapAmount(remote.receiveAmount, assetOut, "receiveAmount", allowZero = false)
    if (args.swapDirection == SwapDirection.SPECIFIED_IN && amountIn != args.amount) invalidQuote("payAmount")
    if (amountIn < limits.min || limits.max?.let { amountIn > it } == true) invalidQuote("limits")
    val fees = remote.networkFees ?: invalidQuote("networkFees")

    return ExchangerAQuoteResult.Quote(
        args = args,
        limits = limits,
        amountIn = ChainAssetWithAmount(assetIn, amountIn),
        amountOut = ChainAssetWithAmount(assetOut, amountOut),
        depositFee = mapOptionalFee(fees.deposit, assetIn, "networkFees.deposit"),
        withdrawalFee = mapOptionalFee(fees.withdrawal, assetOut, "networkFees.withdrawal"),
        executionTime = remote.durationMinutes?.let(::mapExecutionTime),
        warning = remote.warning
    )
}

private fun mapLimits(remote: ExchangerALimitsRemote?, asset: Chain.Asset): ExchangerAPayLimits {
    if (remote == null) invalidQuote("limits")
    val min = mapAmount(remote.minPayAmount, asset, "limits.minPayAmount", allowZero = false)
    val max = remote.maxPayAmount?.let { mapAmount(it, asset, "limits.maxPayAmount", allowZero = false) }
    if (max != null && max < min) invalidQuote("limits")
    return ExchangerAPayLimits(min, max)
}

private fun mapLimitReason(reason: String?): ExchangerAQuoteResult.OutOfRange.Reason {
    return when (reason) {
        "below_minimum" -> ExchangerAQuoteResult.OutOfRange.Reason.BELOW_MINIMUM
        "above_maximum" -> ExchangerAQuoteResult.OutOfRange.Reason.ABOVE_MAXIMUM
        else -> invalidQuote("reason")
    }
}

private fun mapOptionalFee(raw: String?, asset: Chain.Asset, field: String): ChainAssetWithAmount? {
    return raw?.let { ChainAssetWithAmount(asset, mapAmount(it, asset, field, allowZero = true)) }
}

private fun mapAmount(raw: String?, asset: Chain.Asset, field: String, allowZero: Boolean): Balance {
    if (raw == null || !RESPONSE_AMOUNT_PATTERN.matches(raw)) invalidQuote(field)
    val amount = raw.toBigDecimal()
    val validSign = if (allowZero) amount.isNonNegative else amount.isPositive
    if (!validSign || amount.stripTrailingZeros().scale() > asset.precision.value) invalidQuote(field)
    return asset.planksFromAmount(amount)
}

private fun mapExecutionTime(remote: ExchangerADurationRemote): ClosedRange<Duration> {
    val min = remote.min ?: invalidQuote("durationMinutes.min")
    val max = remote.max ?: invalidQuote("durationMinutes.max")
    if (min < 0 || max < min) invalidQuote("durationMinutes")
    return min.minutes..max.minutes
}

private fun invalidQuote(field: String): Nothing {
    throw IOException("Invalid Exchanger A quote field: $field")
}
