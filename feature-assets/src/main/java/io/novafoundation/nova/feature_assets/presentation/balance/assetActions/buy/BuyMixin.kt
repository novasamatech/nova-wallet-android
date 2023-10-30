package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import android.view.View
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfManyAwaitable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface BuyMixin {

    class IntegrationPayload(val buyProvider: BuyProvider, val integrator: BuyTokenRegistry.Integrator<*>)

    val awaitProviderChoosing: ChooseOneOfManyAwaitable<BuyProvider>

    val integrateWithBuyProviderEvent: LiveData<Event<IntegrationPayload>>
    fun buyEnabledFlow(chainAsset: Chain.Asset): Flow<Boolean>

    fun buyClicked(chainAsset: Chain.Asset)

    interface Presentation : BuyMixin
}

fun BaseFragment<*>.setupBuyIntegration(
    mixin: BuyMixin
) {
    mixin.integrateWithBuyProviderEvent.observeEvent {
        with(it) {
            when (integrator) {
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

fun BaseFragment<*>.setupBuyButton(
    buyButton: View,
    buyEnabledFlow: Flow<Boolean>,
    customBuyClick: (() -> Unit)
) {
    buyButton.setOnClickListener { customBuyClick() }

    buyEnabledFlow.observe(buyButton::setEnabled)
}
