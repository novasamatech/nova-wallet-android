package io.novafoundation.nova.feature_assets.presentation.flow

import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope

import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_assets.databinding.FragmentAssetFlowSearchBinding
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetGroupingDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import javax.inject.Inject

abstract class AssetFlowFragment<T : AssetFlowViewModel> :
    BaseFragment<T, FragmentAssetFlowSearchBinding>(),
    BalanceListAdapter.ItemAssetHandler {

    override fun createBinding() = FragmentAssetFlowSearchBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    fun setTitle(@StringRes titleRes: Int) {
        binder.assetFlowToolbar.setTitle(titleRes)
    }

    override fun initViews() {
        binder.assetFlowToolbar.applyStatusBarInsets()
        binder.assetFlowToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.assetFlowSearchContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        with(binder.assetFlowList) {
            setHasFixedSize(true)
            adapter = assetsAdapter

            AssetGroupingDecoration.applyDefaultTo(this, assetsAdapter)
            itemAnimator = null
        }

        binder.assetFlowToolbar.searchField.requestFocus()
        binder.assetFlowToolbar.searchField.content.showSoftKeyboard()
    }

    override fun subscribe(viewModel: T) {
        binder.assetFlowToolbar.searchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe { searchResult ->
            binder.assetFlowPlaceholder.setModelOrHide(searchResult.placeholder)
            binder.assetFlowList.setVisible(searchResult.assets.isNotEmpty())

            assetsAdapter.submitListPreservingViewPoint(
                data = searchResult.assets,
                into = binder.assetFlowList,
                extraDiffCompletedCallback = { binder.assetFlowList.invalidateItemDecorations() }
            )
        }

        viewModel.acknowledgeLedgerWarning.awaitableActionLiveData.observeEvent {
            LedgerNotSupportedWarningBottomSheet(
                context = requireContext(),
                onSuccess = { it.onSuccess(Unit) },
                message = it.payload
            ).show()
        }
    }

    override fun assetClicked(asset: AssetModel) {
        viewModel.assetClicked(asset)

        binder.assetFlowToolbar.searchField.hideSoftKeyboard()
    }
}
