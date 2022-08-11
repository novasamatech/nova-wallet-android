package io.novafoundation.nova.feature_assets.presentation.model

import android.content.Context
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.util.*

class BalanceLocksModel(
    val locks: List<Lock>
) {

    class Lock(
        val id: String,
        val amount: AmountModel
    ) {

        fun formattedId(context: Context): String {
            return when (id) {
                "staking" -> context.getString(R.string.wallet_balance_locks_staking)
                "democrac" -> context.getString(R.string.wallet_balance_locks_democrac)
                "vesting" -> context.getString(R.string.wallet_balance_locks_vesting)
                "phrelect" -> context.getString(R.string.wallet_balance_locks_phrelect)
                else -> id.capitalize(Locale.getDefault())
            }
        }
    }
}
