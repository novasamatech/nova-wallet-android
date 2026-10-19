package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BittensorSlippageSettings @Inject constructor(private val preferences: Preferences) {

    fun observe() = preferences.stringFlow(KEY)
        .map(::decode)
        .distinctUntilChanged()

    fun get(): BigDecimal = decode(preferences.getString(KEY))

    fun set(fraction: BigDecimal) {
        require(fraction in MINIMUM..MAXIMUM)
        preferences.putString(KEY, fraction.stripTrailingZeros().toPlainString())
    }

    fun payload() = SlippageSettingsPayload(
        selected = get().toDouble(),
        default = DEFAULT.toDouble(),
        tips = listOf(0.001, 0.005, 0.01, 0.03),
        minimum = MINIMUM.toDouble(),
        maximum = MAXIMUM.toDouble(),
        small = 0.001,
        big = 0.03
    )

    private fun decode(value: String?): BigDecimal = value?.toBigDecimalOrNull()
        ?.takeIf { it in MINIMUM..MAXIMUM }
        ?: DEFAULT

    companion object {

        val DEFAULT = 0.005.toBigDecimal()

        val MINIMUM = 0.0001.toBigDecimal()

        val MAXIMUM = 0.5.toBigDecimal()

        private const val KEY = "bittensor.slippage"
    }
}
