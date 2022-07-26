package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import android.view.View
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfManyAwaitable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import kotlinx.coroutines.flow.Flow

interface BuyMixin {

    class IntegrationPayload(val integrator: BuyTokenRegistry.Integrator<*>)

    val awaitProviderChoosing: ChooseOneOfManyAwaitable<BuyProvider>

    val integrateWithBuyProviderEvent: LiveData<Event<IntegrationPayload>>

    val buyEnabled: Flow<Boolean>

    fun buyClicked()

    interface Presentation : BuyMixin
}

fun BaseFragment<*>.setupBuyIntegration(
    mixin: BuyMixin,
    buyButton: View,
    customBuyClick: (() -> Unit)? = null
) {
    mixin.integrateWithBuyProviderEvent.observeEvent {
        with(it) {
            when (integrator) {
                is ExternalProvider.Integrator -> integrator.openBuyFlow(requireContext())
            }
        }
    }

    mixin.awaitProviderChoosing.awaitableActionLiveData.observeEvent { action ->
        BuyProviderChooserBottomSheet(
            context = requireContext(),
            payload = action.payload,
            onSelect = action.onSuccess,
            onCancel = action.onCancel
        ).show()
    }

    buyButton.setOnClickListener { if (customBuyClick != null) customBuyClick() else mixin.buyClicked() }

    mixin.buyEnabled.observe(buyButton::setEnabled)
}
