package io.novafoundation.nova.feature_assets.presentation.flow.asset

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_assets.databinding.FragmentAssetFlowSearchBinding
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.CompoundAssetDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.NetworkAssetDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.TokenAssetGroupDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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

    override fun applyInsets(rootView: View) {
        binder.assetFlowToolbar.applyStatusBarInsets()
        binder.assetFlowList.applyNavigationBarInsets(imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.assetFlowToolbar.setHomeButtonListener {
            hideKeyboard()
            viewModel.backClicked()
        }

        with(binder.assetFlowList) {
            setHasFixedSize(true)
            adapter = assetsAdapter

            AssetBaseDecoration.applyDefaultTo(
                this,
                assetsAdapter,
                CompoundAssetDecorationPreferences(
                    NetworkAssetDecorationPreferences(),
                    TokenAssetGroupDecorationPreferences()
                )
            )
            itemAnimator = null
        }

        binder.assetFlowToolbar.searchField.requestFocus()
        binder.assetFlowToolbar.searchField.content.showSoftKeyboard()
    }

    override fun subscribe(viewModel: T) {
        binder.assetFlowToolbar.searchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchHint.observe {
            binder.assetFlowToolbar.searchField.setHint(it)
        }

        viewModel.searchResults.observe { assets ->
            binder.assetFlowList.setVisible(assets.isNotEmpty())

            assetsAdapter.submitListPreservingViewPoint(
                data = assets,
                into = binder.assetFlowList,
                extraDiffCompletedCallback = { binder.assetFlowList.invalidateItemDecorations() }
            )
        }

        viewModel.placeholder.observe { placeholder ->
            binder.assetFlowPlaceholder.setModelOrHide(placeholder)
        }

        viewModel.acknowledgeLedgerWarning.awaitableActionLiveData.observeEvent {
            LedgerNotSupportedWarningBottomSheet(
                context = requireContext(),
                onSuccess = { it.onSuccess(Unit) },
                message = it.payload
            ).show()
        }
    }

    override fun assetClicked(asset: Chain.Asset) {
        viewModel.assetClicked(asset)

        binder.assetFlowToolbar.searchField.hideSoftKeyboard()
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        viewModel.tokenClicked(tokenGroup)

        binder.assetFlowToolbar.searchField.hideSoftKeyboard()
    }
}
