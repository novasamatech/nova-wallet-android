package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.common

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay
import java.math.BigInteger

/**
 * Builders for the Nova Wallet service-fee disclosure row shown next to the
 * network-fee row on the four Subtensor stake/unstake screens. Mirrors the
 * shipped iOS feature. The fee value is a pure display artifact — the actual
 * fee leg lives in the extrinsic builder (Phase A). These helpers only produce
 * a [FeeStatus] for an existing `FeeView`/`setFeeStatus` binding.
 *
 * Isolated here (own file, own package object) so none of the existing
 * network-fee rows or VMs are touched.
 */
object SubtensorNovaFee {

    /**
     * Whether the disclosure (fee row + caption) should be shown at all.
     *
     * True only when the fee actually applies: a non-root subnet AND a
     * configured recipient. Today [SubtensorStakingConstants.NOVA_FEE_RECIPIENT]
     * is `null`, so this is always false and the UI stays hidden — matching the
     * inert Phase-A mechanism. Setting a recipient flips both on.
     */
    fun feeApplies(netuid: Int): Boolean {
        return netuid != SubtensorStakingConstants.ROOT_NETUID &&
            SubtensorStakingConstants.NOVA_FEE_RECIPIENT != null
    }

    /**
     * [FeeStatus] for the Nova fee rendered in the native asset (TAO) — used by
     * the stake screens and the unstake-confirm screen. [grossPlanks] is the
     * amount the 0.3% is charged on; the fee itself is
     * [SubtensorStakingConstants.novaFeeAmount].
     *
     * Returns [FeeStatus.NoFee] when the fee does not apply, which hides the row
     * via the standard `FeeView` behavior.
     */
    fun nativeFeeStatus(
        netuid: Int,
        grossPlanks: BigInteger,
        token: TokenBase,
        amountFormatter: AmountFormatter,
    ): FeeStatus<Unit, FeeDisplay> {
        if (!feeApplies(netuid)) return FeeStatus.NoFee

        // No amount entered yet → keep the row present but show the standard
        // loading spinner (matches iOS) instead of a literal "0". Once a positive
        // amount exists, dust that floors to 0 still renders as "0".
        if (grossPlanks <= BigInteger.ZERO) return FeeStatus.Loading(visibleDuringProgress = true)

        val feePlanks = SubtensorStakingConstants.novaFeeAmount(grossPlanks)
        val display = amountFormatter.formatAmountToAmountModel(feePlanks, token).toFeeDisplay()

        return FeeStatus.Loaded(FeeModel(fee = Unit, display = display))
    }

    /**
     * [FeeStatus] for the Nova fee rendered in the screen's (alpha) asset units —
     * used by the unstake-setup screen, where iOS shows the fee in the subnet
     * token. The fee is 0.3% of the entered alpha [grossPlanks]; we format the
     * raw value with [precision] and tag it with [ticker] (e.g. "SN56") since
     * subnet alpha carries no fiat price (matching the amount header).
     */
    fun alphaFeeStatus(
        netuid: Int,
        grossPlanks: BigInteger,
        precision: Int,
        ticker: String,
    ): FeeStatus<Unit, FeeDisplay> {
        if (!feeApplies(netuid)) return FeeStatus.NoFee

        // No amount entered yet → loading spinner (matches iOS), not "0 <ticker>".
        if (grossPlanks <= BigInteger.ZERO) return FeeStatus.Loading(visibleDuringProgress = true)

        val feePlanks = SubtensorStakingConstants.novaFeeAmount(grossPlanks)
        val feeAlpha = feePlanks.toBigDecimal().movePointLeft(precision)
        val display = FeeDisplay(
            title = "%.5f %s".format(feeAlpha, ticker),
            subtitle = null,
        )

        return FeeStatus.Loaded(FeeModel(fee = Unit, display = display))
    }
}
