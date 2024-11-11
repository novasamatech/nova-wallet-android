package io.novafoundation.nova.feature_assets.presentation.flow.asset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.CompoundAssetDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.NetworkAssetDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.TokenAssetGroupDecorationPreferences
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowList
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowPlaceholder
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowSearchContainer
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowToolbar

abstract class AssetFlowFragment<T : AssetFlowViewModel> :
    BaseFragment<T>(),
    BalanceListAdapter.ItemAssetHandler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_asset_flow_search, container, false)
    }

    fun setTitle(@StringRes titleRes: Int) {
        assetFlowToolbar.setTitle(titleRes)
    }

    override fun initViews() {
        assetFlowToolbar.applyStatusBarInsets()
        assetFlowToolbar.setHomeButtonListener { viewModel.backClicked() }
        assetFlowSearchContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        with(assetFlowList) {
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

        assetFlowToolbar.searchField.requestFocus()
        assetFlowToolbar.searchField.content.showSoftKeyboard()
    }

    override fun subscribe(viewModel: T) {
        assetFlowToolbar.searchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchHint.observe {
            assetFlowToolbar.searchField.setHint(it)
        }

        viewModel.searchResults.observe { assets ->
            assetFlowList.setVisible(assets.isNotEmpty())

            assetsAdapter.submitListPreservingViewPoint(
                data = assets,
                into = assetFlowList,
                extraDiffCompletedCallback = { assetFlowList.invalidateItemDecorations() }
            )
        }

        viewModel.placeholder.observe { placeholder ->
            assetFlowPlaceholder.setModelOrHide(placeholder)
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

        assetFlowToolbar.searchField.hideSoftKeyboard()
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        viewModel.tokenClicked(tokenGroup)

        assetFlowToolbar.searchField.hideSoftKeyboard()
    }
}
