package io.novafoundation.nova.feature_assets.presentation.balance.search

import android.view.View
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.databinding.FragmentAssetSearchBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensItemAnimator
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.common.createForAssets
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

class AssetSearchFragment : BaseFragment<AssetSearchViewModel, FragmentAssetSearchBinding>(),
    BalanceListAdapter.ItemAssetHandler {

    override fun createBinding() = FragmentAssetSearchBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    override fun applyInsets(rootView: View) {
        binder.searchAssetSearch.applyStatusBarInsets()
        binder.searchAssetContainer.applyNavigationBarInsets(consume = false, imeInsets = true)
    }

    override fun initViews() {
        binder.searchAssetList.setHasFixedSize(true)
        binder.searchAssetList.adapter = assetsAdapter

        val animationSettings = ExpandableAnimationSettings.createForAssets()
        val animator = ExpandableAnimator(binder.searchAssetList, animationSettings, assetsAdapter)

        binder.searchAssetList.addItemDecoration(AssetTokensDecoration(requireContext(), assetsAdapter, animator))
        binder.searchAssetList.itemAnimator = AssetTokensItemAnimator(animationSettings, animator)

        AssetBaseDecoration.applyDefaultTo(binder.searchAssetList, assetsAdapter)

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

        viewModel.query.observe {
            binder.searchAssetList.post {
                binder.searchAssetList.layoutManager!!.scrollToPosition(0)
            }
        }

        viewModel.searchResults.observe { data ->
            binder.searchAssetsPlaceholder.setVisible(data.isEmpty())
            binder.searchAssetList.setVisible(data.isNotEmpty())

            assetsAdapter.submitList(data) { binder.searchAssetList.invalidateItemDecorations() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binder.searchAssetSearch.searchInput.hideSoftKeyboard()
    }

    override fun assetClicked(asset: Chain.Asset) {
        viewModel.assetClicked(asset)
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        if (tokenGroup.groupType is TokenGroupUi.GroupType.SingleItem) {
            viewModel.assetClicked(tokenGroup.groupType.asset)
        } else {
            val itemAnimator = binder.searchAssetList.itemAnimator as AssetTokensItemAnimator
            itemAnimator.prepareForAnimation()

            viewModel.assetListMixin.expandToken(tokenGroup)
        }
    }
}
