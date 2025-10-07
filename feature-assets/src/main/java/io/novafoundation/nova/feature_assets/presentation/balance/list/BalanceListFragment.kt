package io.novafoundation.nova.feature_assets.presentation.balance.list

import android.view.View
import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.EditablePlaceholderAdapter
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.space.SpaceBetween
import io.novafoundation.nova.common.utils.recyclerView.space.addSpaceItemDecoration
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentBalanceListBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.BalanceBreakdownBottomSheet
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensItemAnimator
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.setupBuySellSelectorMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.createForAssets
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetsHeaderAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetsHeaderHolder
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.ManageAssetsAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.ManageAssetsHolder
import io.novafoundation.nova.feature_banners_api.presentation.BannerHolder
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannerAdapter
import io.novafoundation.nova.feature_banners_api.presentation.bindWithAdapter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

class BalanceListFragment :
    BaseFragment<BalanceListViewModel, FragmentBalanceListBinding>(),
    BalanceListAdapter.ItemAssetHandler,
    AssetsHeaderAdapter.Handler,
    ManageAssetsAdapter.Handler {

    override fun createBinding() = FragmentBalanceListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private var balanceBreakdownBottomSheet: BalanceBreakdownBottomSheet? = null

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AssetsHeaderAdapter(this)
    }

    private val bannerAdapter: PromotionBannerAdapter by lazy(LazyThreadSafetyMode.NONE) {
        PromotionBannerAdapter(closable = true)
    }

    private val manageAssetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ManageAssetsAdapter(this)
    }

    private val emptyAssetsPlaceholder by lazy(LazyThreadSafetyMode.NONE) {
        EditablePlaceholderAdapter(
            model = getAssetsPlaceholderModel(),
            clickListener = { buySellClicked() }
        )
    }

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, bannerAdapter, manageAssetsAdapter, emptyAssetsPlaceholder, assetsAdapter)
    }

    override fun applyInsets(rootView: View) {
        binder.balanceListAssets.applyStatusBarInsets()
    }

    override fun initViews() {
        hideKeyboard()

        setupRecyclerView()

        binder.walletContainer.setOnRefreshListener {
            viewModel.fullSync()
        }
    }

    private fun setupRecyclerView() {
        binder.balanceListAssets.setHasFixedSize(true)
        binder.balanceListAssets.adapter = adapter

        setupAssetsDecorationForRecyclerView()
        setupRecyclerViewSpacing()
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .balanceListComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BalanceListViewModel) {
        setupBuySellSelectorMixin(viewModel.buySellSelectorMixin)

        viewModel.bannersMixin.bindWithAdapter(bannerAdapter) {
            binder.balanceListAssets.invalidateItemDecorations()
        }

        viewModel.assetListMixin.assetModelsFlow.observe {
            assetsAdapter.submitList(it) {
                binder.balanceListAssets.invalidateItemDecorations()
            }
        }

        viewModel.maskingModeEnableFlow.observe(headerAdapter::setMaskingEnabled)
        viewModel.totalBalanceFlow.observe(headerAdapter::setTotalBalance)
        viewModel.selectedWalletModelFlow.observe(headerAdapter::setSelectedWallet)
        viewModel.shouldShowPlaceholderFlow.observe(emptyAssetsPlaceholder::show)
        viewModel.nftCountFlow.observe(headerAdapter::setNftCountLabel)
        viewModel.nftPreviewsUi.observe(headerAdapter::setNftPreviews)

        viewModel.hideRefreshEvent.observeEvent {
            binder.walletContainer.isRefreshing = false
        }

        viewModel.balanceBreakdownFlow.observe {
            if (balanceBreakdownBottomSheet?.isShowing == true) {
                balanceBreakdownBottomSheet?.setBalanceBreakdown(it)
            }
        }

        viewModel.showBalanceBreakdownEvent.observeEvent { totalBalanceBreakdown ->
            if (balanceBreakdownBottomSheet == null) {
                balanceBreakdownBottomSheet = BalanceBreakdownBottomSheet(requireContext())

                balanceBreakdownBottomSheet?.setOnDismissListener {
                    balanceBreakdownBottomSheet = null
                }
            }
            balanceBreakdownBottomSheet?.setOnShowListener {
                balanceBreakdownBottomSheet?.setBalanceBreakdown(totalBalanceBreakdown)
            }
            balanceBreakdownBottomSheet?.show()
        }

        viewModel.walletConnectAccountSessionsUI.observe(headerAdapter::setWalletConnectModel)
        viewModel.pendingOperationsCountModel.observe(headerAdapter::setPendingOperationsCountModel)
        viewModel.filtersIndicatorIcon.observe(headerAdapter::setFilterIconRes)
        viewModel.assetViewModeModelFlow.observe { manageAssetsAdapter.setAssetViewModeModel(it) }
    }

    override fun assetClicked(asset: Chain.Asset) {
        viewModel.assetClicked(asset)
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        if (tokenGroup.groupType is TokenGroupUi.GroupType.SingleItem) {
            viewModel.assetClicked(tokenGroup.groupType.asset)
        } else {
            val itemAnimator = binder.balanceListAssets.itemAnimator as AssetTokensItemAnimator
            itemAnimator.prepareForAnimation()

            viewModel.assetListMixin.expandToken(tokenGroup)
        }
    }

    override fun totalBalanceClicked() {
        viewModel.balanceBreakdownClicked()
    }

    override fun manageClicked() {
        viewModel.manageClicked()
    }

    override fun searchClicked() {
        viewModel.searchClicked()
    }

    override fun avatarClicked() {
        viewModel.avatarClicked()
    }

    override fun goToNftsClicked() {
        viewModel.goToNftsClicked()
    }

    override fun walletConnectClicked() {
        viewModel.walletConnectClicked()
    }

    override fun maskClicked() {
        viewModel.toggleMasking()
    }

    override fun sendClicked() {
        viewModel.sendClicked()
    }

    override fun receiveClicked() {
        viewModel.receiveClicked()
    }

    override fun buySellClicked() {
        viewModel.buySellClicked()
    }

    override fun novaCardClick() {
        viewModel.novaCardClicked()
    }

    override fun pendingOperationsClicked() {
        viewModel.pendingOperationsClicked()
    }

    override fun assetViewModeClicked() {
        viewModel.switchViewMode()
    }

    override fun swapClicked() {
        viewModel.swapClicked()
    }

    private fun setupRecyclerViewSpacing() {
        binder.balanceListAssets.addSpaceItemDecoration {
            add(SpaceBetween(AssetsHeaderHolder, BannerHolder, spaceDp = 4))
            add(SpaceBetween(BannerHolder, ManageAssetsHolder, spaceDp = 4))
            add(SpaceBetween(AssetsHeaderHolder, ManageAssetsHolder, spaceDp = 24))
        }
    }

    private fun setupAssetsDecorationForRecyclerView() {
        val animationSettings = ExpandableAnimationSettings.createForAssets()
        val animator = ExpandableAnimator(binder.balanceListAssets, animationSettings, assetsAdapter)

        AssetBaseDecoration.applyDefaultTo(binder.balanceListAssets, assetsAdapter)

        binder.balanceListAssets.addItemDecoration(AssetTokensDecoration(requireContext(), assetsAdapter, animator))
        binder.balanceListAssets.itemAnimator = AssetTokensItemAnimator(animationSettings, animator)
    }

    private fun getAssetsPlaceholderModel() = PlaceholderModel(
        text = getString(R.string.wallet_assets_empty),
        imageRes = R.drawable.ic_planet_outline,
        buttonText = getString(R.string.assets_buy_tokens_placeholder_button)
    )
}
