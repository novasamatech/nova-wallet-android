package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.BalancesView

class AssetDetailBalancesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : BalancesView(context, attrs, defStyle) {

    val total = item(R.string.common_total)

    val transferable = item(R.string.wallet_balance_transferable)

    val locked = item(R.string.wallet_balance_locked).apply {
        setOwnDividerVisible(false)
        title.setDrawableEnd(R.drawable.ic_info, paddingInDp = 4)
    }
}
