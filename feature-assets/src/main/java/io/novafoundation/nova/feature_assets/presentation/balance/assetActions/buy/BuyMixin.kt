package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface BuyMixin {
    class IntegrationPayload(
            val provider: BuyTokenRegistry.Provider<*>,
            val chainAsset: Chain.Asset,
            val address: String,
    )

    class ProviderChooserPayload(
            val providers: List<BuyTokenRegistry.Provider<*>>,
            val chainAsset: Chain.Asset,
    )

    val showProviderChooserEvent: LiveData<Event<ProviderChooserPayload>>

    val integrateWithBuyProviderEvent: LiveData<Event<IntegrationPayload>>

    fun providerChosen(
            provider: BuyTokenRegistry.Provider<*>,
            chainAsset: Chain.Asset
    )

    interface Presentation : BuyMixin {

        override val showProviderChooserEvent: MutableLiveData<Event<ProviderChooserPayload>>

        override val integrateWithBuyProviderEvent: MutableLiveData<Event<IntegrationPayload>>

        fun buyClicked(chainId: ChainId, chainAssetId: Int)

        fun isBuyEnabled(chainId: ChainId, chainAssetId: Int): Boolean
    }
}

fun <V> BaseFragment<V>.setupBuyIntegration(viewModel: V) where V : BaseViewModel, V : BuyMixin {
    viewModel.integrateWithBuyProviderEvent.observeEvent {
        with(it) {
            when (provider) {
                is ExternalProvider -> provider.createIntegrator(it.chainAsset, address).integrate(requireContext())
            }
        }
    }

    viewModel.showProviderChooserEvent.observeEvent { payload ->
        BuyProviderChooserBottomSheet(
            requireContext(), payload.providers,
            onClick = {
                viewModel.providerChosen(it, payload.chainAsset)
            }
        ).show()
    }
}
