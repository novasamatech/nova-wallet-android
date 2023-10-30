package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import android.content.Context
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.feature_assets.R

fun showBuyDisclaimer(
    context: Context,
    item: BuyProvider,
    positiveButton: () -> Unit
) {
    infoDialog(context) {
        setTitle(R.string.buy_provider_open_confirmation_title)
        setMessage(context.getString(R.string.buy_provider_open_confirmation_message, item.officialUrl))
        setPositiveButton(R.string.common_continue) { _, _ ->
            positiveButton()
        }
        setNegativeButton(R.string.common_cancel, null)
    }
}
