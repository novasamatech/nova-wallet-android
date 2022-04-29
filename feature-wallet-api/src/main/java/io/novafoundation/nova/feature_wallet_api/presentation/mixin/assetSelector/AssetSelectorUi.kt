package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.AssetSelectorBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.view.AssetSelectorView

interface WithAssetSelector {

    val assetSelectorMixin: AssetSelectorMixin
}

fun <V> BaseFragment<V>.setupAssetSelector(
    view: AssetSelectorView,
    selectorMixin: AssetSelectorMixin,
    imageLoader: ImageLoader
) where V : BaseViewModel {
    view.onClick {
        selectorMixin.assetSelectorClicked()
    }

    selectorMixin.selectedAssetModelFlow.observe {
        view.setState(imageLoader, it)
    }

    selectorMixin.showAssetChooser.observeEvent {
        AssetSelectorBottomSheet(
            imageLoader = imageLoader,
            context = requireContext(),
            payload = it,
            onClicked = selectorMixin::assetChosen
        ).show()
    }
}
