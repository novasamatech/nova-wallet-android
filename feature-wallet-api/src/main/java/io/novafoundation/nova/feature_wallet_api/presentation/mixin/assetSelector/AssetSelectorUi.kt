package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.AssetSelectorBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.view.AssetSelectorView

interface WithAssetSelector {

    val assetSelectorMixin: AssetSelectorMixin
}

fun <V> BaseFragment<V, *>.setupAssetSelector(
    view: AssetSelectorView,
    selectorMixin: AssetSelectorMixin,
    imageLoader: ImageLoader
) where V : BaseViewModel {
    view.onClick {
        selectorMixin.assetSelectorClicked()
    }

    subscribeOnAssetChange(selectorMixin) {
        view.setState(imageLoader, it)
    }
    subscribeOnAssetClick(selectorMixin, imageLoader)
}

fun <V> BaseFragment<V, *>.subscribeOnAssetChange(
    selectorMixin: AssetSelectorMixin,
    onAssetChanged: (AssetSelectorModel) -> Unit
) where V : BaseViewModel {
    selectorMixin.selectedAssetModelFlow.observe {
        onAssetChanged(it)
    }
}

fun <V> BaseFragment<V, *>.subscribeOnAssetClick(
    selectorMixin: AssetSelectorMixin,
    imageLoader: ImageLoader
) where V : BaseViewModel {
    selectorMixin.showAssetChooser.observeEvent {
        AssetSelectorBottomSheet(
            imageLoader = imageLoader,
            context = requireContext(),
            payload = it,
            onClicked = { _, item -> selectorMixin.assetChosen(item) }
        ).show()
    }
}
