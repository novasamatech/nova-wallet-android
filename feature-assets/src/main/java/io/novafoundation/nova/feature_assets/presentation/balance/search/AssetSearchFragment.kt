package io.novafoundation.nova.feature_assets.presentation.balance.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensItemAnimator
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.common.createForAssets
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.android.synthetic.main.fragment_asset_search.searchAssetContainer
import kotlinx.android.synthetic.main.fragment_asset_search.searchAssetList
import kotlinx.android.synthetic.main.fragment_asset_search.searchAssetSearch
import kotlinx.android.synthetic.main.fragment_asset_search.searchAssetsPlaceholder
import javax.inject.Inject

class AssetSearchFragment :
    BaseBottomSheetFragment<AssetSearchViewModel>(),
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
        return layoutInflater.inflate(R.layout.fragment_asset_search, container, false)
    }

    override fun initViews() {
        searchAssetSearch.applyStatusBarInsets()
        searchAssetContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        searchAssetList.setHasFixedSize(true)
        searchAssetList.adapter = assetsAdapter

        val animationSettings = ExpandableAnimationSettings.createForAssets()
        val animator = ExpandableAnimator(searchAssetList, animationSettings, assetsAdapter)

        searchAssetList.addItemDecoration(AssetTokensDecoration(requireContext(), assetsAdapter, animator))
        searchAssetList.itemAnimator = AssetTokensItemAnimator(animationSettings, animator)

        AssetBaseDecoration.applyDefaultTo(searchAssetList, assetsAdapter)

        searchAssetSearch.cancel.setOnClickListener {
            viewModel.cancelClicked()
        }
        onBackPressed { viewModel.cancelClicked() }

        searchAssetSearch.searchInput.requestFocus()
        searchAssetSearch.searchInput.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .assetSearchComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AssetSearchViewModel) {
        searchAssetSearch.searchInput.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.query.observe {
            searchAssetList.post {
                searchAssetList.layoutManager!!.scrollToPosition(0)
            }
        }

        viewModel.searchResults.observe { data ->
            searchAssetsPlaceholder.setVisible(data.isEmpty())
            searchAssetList.setVisible(data.isNotEmpty())

            assetsAdapter.submitList(data) { searchAssetList.invalidateItemDecorations() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        searchAssetSearch.searchInput.hideSoftKeyboard()
    }

    override fun assetClicked(asset: Chain.Asset) {
        viewModel.assetClicked(asset)
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        if (tokenGroup.groupType is TokenGroupUi.GroupType.SingleItem) {
            viewModel.assetClicked(tokenGroup.groupType.asset)
        } else {
            val itemAnimator = searchAssetList.itemAnimator as AssetTokensItemAnimator
            itemAnimator.prepareForAnimation()

            viewModel.assetListMixin.expandToken(tokenGroup)
        }
    }
}
