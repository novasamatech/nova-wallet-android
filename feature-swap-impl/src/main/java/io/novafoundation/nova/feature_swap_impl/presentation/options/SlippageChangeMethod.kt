package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/** Formats the editable percentage using the same precision as the input field. */
internal fun slippageInputText(slippage: Fraction, maxFractionDigits: Int): String = BigDecimal.valueOf(slippage.inPercents)
    .setScale(maxFractionDigits, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

/** Back to the default is a reset, a listed tip is a preset, anything else is custom. */
internal fun slippageChangeMethod(initial: Fraction, selected: Fraction, config: SlippageConfig): SlippageSettingsCommunicator.Method = when {
    selected.sameAs(config.defaultSlippage) && !initial.sameAs(config.defaultSlippage) -> SlippageSettingsCommunicator.Method.RESET
    config.slippageTips.any(selected::sameAs) -> SlippageSettingsCommunicator.Method.PRESET
    else -> SlippageSettingsCommunicator.Method.CUSTOM
}

// Typed percents and configured fractions take different floating-point paths, e.g. 0.1 / 100 != 0.001.
internal fun Fraction.sameAs(other: Fraction): Boolean = abs(inFraction - other.inFraction) < FRACTION_EPSILON

private const val FRACTION_EPSILON = 1e-12
