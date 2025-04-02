package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import android.content.Context
import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.feature_buy_api.domain.TradeProvider
import io.novafoundation.nova.feature_buy_api.domain.providers.InternalProvider
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_impl.R
import kotlinx.coroutines.flow.Flow

class RealBuyMixinUi : BuyMixinUi {

    override fun setupBuyIntegration(fragment: BaseFragment<*>, mixin: TradeMixin) = with(fragment) {

    }

    override fun setupBuyButton(
        fragment: BaseFragment<*>,
        buyButton: View,
        buyEnabledFlow: Flow<Boolean>,
        customBuyClick: () -> Unit
    ) = with(fragment) {
        buyButton.setOnClickListener { customBuyClick() }

        buyEnabledFlow.observe(buyButton::setEnabled)
    }
}

private fun showBuyDisclaimer(
    context: Context,
    item: TradeProvider,
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
