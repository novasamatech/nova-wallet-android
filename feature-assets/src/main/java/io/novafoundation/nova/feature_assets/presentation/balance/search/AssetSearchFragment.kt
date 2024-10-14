package io.novafoundation.nova.feature_assets.presentation.balance.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentAssetSearchBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetGroupingDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

import javax.inject.Inject

class AssetSearchFragment :
    BaseBottomSheetFragment<AssetSearchViewModel, FragmentAssetSearchBinding>(),
    BalanceListAdapter.ItemAssetHandler {

    override val binder by viewBinding(FragmentAssetSearchBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.searchAssetSearch.applyStatusBarInsets()
        binder.searchAssetContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        binder.searchAssetList.setHasFixedSize(true)
        binder.searchAssetList.adapter = assetsAdapter

        AssetGroupingDecoration.applyDefaultTo(binder.searchAssetList, assetsAdapter)
        binder.searchAssetList.itemAnimator = null

        binder.searchAssetSearch.cancel.setOnClickListener {
            viewModel.cancelClicked()
        }
        onBackPressed { viewModel.cancelClicked() }

        binder.searchAssetSearch.searchInput.requestFocus()
        binder.searchAssetSearch.searchInput.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .assetSearchComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AssetSearchViewModel) {
        binder.searchAssetSearch.searchInput.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe { data ->
            binder.searchAssetsPlaceholder.setVisible(data.isEmpty())
            binder.searchAssetList.setVisible(data.isNotEmpty())

            assetsAdapter.submitListPreservingViewPoint(
                data = data,
                into = binder.searchAssetList,
                extraDiffCompletedCallback = { binder.searchAssetList.invalidateItemDecorations() }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binder.searchAssetSearch.searchInput.hideSoftKeyboard()
    }

    override fun assetClicked(asset: AssetModel) {
        viewModel.assetClicked(asset)
    }
}
