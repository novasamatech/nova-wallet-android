package jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl

import coil.ImageLoader
import coil.load
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.utils.setVisible
import jp.co.soramitsu.common.view.LabeledTextView
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin

fun <V> BaseFragment<V>.setupForcedChainUi(
    viewModel: V,
    ui: LabeledTextView,
    imageLoader: ImageLoader
) where V : BaseViewModel, V: WithForcedChainMixin {
    viewModel.forcedChainMixin.forcedChainLiveData.observe { chainUi ->
        ui.setVisible(chainUi != null)

        chainUi?.let {
            ui.setMessage(chainUi.name)
            ui.textIconView.load(chainUi.icon, imageLoader)
        }
    }
}
