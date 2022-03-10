package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_assets.presentation.common.currencyItem
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

class LockedTokensBottomSheet(
    context: Context,
    private val payload: AssetModel
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = context.getString(R.string.wallet_balance_locked_template, payload.token.configuration.symbol)
        setTitle(title)

        currencyItem(R.string.wallet_balance_locked, payload.locked)
        currencyItem(R.string.wallet_balance_bonded, payload.bonded)
        currencyItem(R.string.wallet_balance_reserved, payload.reserved)
        currencyItem(R.string.wallet_balance_redeemable, payload.redeemable)
        currencyItem(R.string.wallet_balance_unbonding_v1_9_0, payload.unbonding)
    }
}
