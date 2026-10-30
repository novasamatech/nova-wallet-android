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

data class BittensorWithdrawalNotice(val title: String, val message: String)

class BittensorWithdrawalFormatter @Inject constructor(
    private val resources: ResourceManager,
    private val tokens: TokenFormatter,
    @Named("BittensorWithdrawalDuration") private val durationFormatter: DurationFormatter
) {

    fun format(withdrawal: BittensorPositionWithdrawal, precision: Int, symbol: TokenSymbol): BittensorWithdrawalNotice {
        val locked = tokens.formatToken(BigDecimal(withdrawal.locked, precision), symbol)
        val messageRes = if (withdrawal.available.isPositive()) {
            R.string.bittensor_withdrawal_partial
        } else {
            R.string.bittensor_withdrawal_unavailable
        }
        val message = buildList {
            add(resources.getString(messageRes))
            if (withdrawal.holdDurationMillis.isPositive()) {
                val duration = durationFormatter.format(withdrawal.holdDurationMillis.toLong().milliseconds)
                add(resources.getString(R.string.bittensor_position_hold, duration))
            }
        }.joinToString("\n\n")
        return BittensorWithdrawalNotice(resources.getString(R.string.bittensor_withdrawal_locked_title, locked), message)
    }
}
