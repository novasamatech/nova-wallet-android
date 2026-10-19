package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionWithdrawal
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.milliseconds

class BittensorWithdrawalFormatter @Inject constructor(
    private val resources: ResourceManager,
    private val tokens: TokenFormatter,
    @Named("BittensorWithdrawalDuration") private val durationFormatter: DurationFormatter
) {

    fun format(withdrawal: BittensorPositionWithdrawal, precision: Int, symbol: TokenSymbol): String = buildList {
        val available = tokens.formatToken(BigDecimal(withdrawal.available, precision), symbol)
        add(resources.getString(R.string.bittensor_position_withdrawable, available))
        if (withdrawal.locked.isPositive()) {
            val locked = tokens.formatToken(BigDecimal(withdrawal.locked, precision), symbol)
            add(resources.getString(R.string.bittensor_position_locked, locked))
        }
        if (withdrawal.holdDurationMillis.isPositive()) {
            val duration = durationFormatter.format(withdrawal.holdDurationMillis.toLong().milliseconds)
            add(resources.getString(R.string.bittensor_position_hold, duration))
        }
    }.joinToString("\n\n")
}
