package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import android.content.Context
import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.feature_buy_api.domain.BuyProvider
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.feature_buy_api.domain.providers.ExternalProvider
import kotlinx.coroutines.flow.Flow

class RealBuyMixinUi : BuyMixinUi {

    override fun setupBuyIntegration(fragment: BaseFragment<*>, mixin: BuyMixin) = with(fragment) {
        mixin.integrateWithBuyProviderEvent.observeEvent {
            with(it) {
                when (val integrator = integrator) {
                    is ExternalProvider.Integrator -> showBuyDisclaimer(requireContext(), buyProvider) {
                        integrator.openBuyFlow(requireContext())
                    }
                }
            }
        }

        mixin.awaitProviderChoosing.awaitableActionLiveData.observeEvent { action ->
            BuyProviderChooserBottomSheet(
                context = requireContext(),
                payload = action.payload,
                onSelect = { _, item -> action.onSuccess(item) },
                onCancel = action.onCancel
            ).show()
        }
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
