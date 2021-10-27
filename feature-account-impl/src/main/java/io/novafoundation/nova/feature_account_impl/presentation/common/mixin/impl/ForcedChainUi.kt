package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.LabeledTextView
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin

fun <V> BaseFragment<V>.setupForcedChainUi(
    viewModel: V,
    ui: LabeledTextView,
    imageLoader: ImageLoader
) where V : BaseViewModel, V : WithForcedChainMixin {
    viewModel.forcedChainMixin.forcedChainLiveData.observe { chainUi ->
        ui.setVisible(chainUi != null)

        chainUi?.let {
            ui.setMessage(chainUi.name)
            ui.textIconView.load(chainUi.icon, imageLoader)
        }
    }
}
