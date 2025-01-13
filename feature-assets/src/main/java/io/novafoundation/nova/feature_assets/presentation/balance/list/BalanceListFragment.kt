package io.novafoundation.nova.feature_assets.presentation.balance.list

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.feature_assets.databinding.FragmentBalanceListBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.BalanceBreakdownBottomSheet
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.AssetBaseDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensItemAnimator
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.common.createForAssets
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetsHeaderAdapter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

class BalanceListFragment :
    BaseFragment<BalanceListViewModel, FragmentBalanceListBinding>(),
    BalanceListAdapter.ItemAssetHandler,
    AssetsHeaderAdapter.Handler {

    override fun createBinding() = FragmentBalanceListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private var balanceBreakdownBottomSheet: BalanceBreakdownBottomSheet? = null

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AssetsHeaderAdapter(this)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, assetsAdapter)
    }

    override fun initViews() {
        binder.balanceListAssets.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        hideKeyboard()

        binder.balanceListAssets.setHasFixedSize(true)
        binder.balanceListAssets.adapter = adapter

        val animationSettings = ExpandableAnimationSettings.createForAssets()
        val animator = ExpandableAnimator(binder.balanceListAssets, animationSettings, assetsAdapter)

        binder.balanceListAssets.addItemDecoration(AssetTokensDecoration(requireContext(), assetsAdapter, animator))
        binder.balanceListAssets.itemAnimator = AssetTokensItemAnimator(animationSettings, animator)

        AssetBaseDecoration.applyDefaultTo(binder.balanceListAssets, assetsAdapter)

        binder.walletContainer.setOnRefreshListener {
            viewModel.fullSync()
        }
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
        viewModel.assetListMixin.assetModelsFlow.observe {
            assetsAdapter.submitList(it) {
                binder.balanceListAssets?.invalidateItemDecorations()
            }
        }

        viewModel.totalBalanceFlow.observe(headerAdapter::setTotalBalance)
        viewModel.selectedWalletModelFlow.observe(headerAdapter::setSelectedWallet)
        viewModel.shouldShowPlaceholderFlow.observe(headerAdapter::setPlaceholderVisibility)
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

        viewModel.walletConnectAccountSessionsUI.observe {
            headerAdapter.setWalletConnectModel(it)
        }

        viewModel.filtersIndicatorIcon.observe(headerAdapter::setFilterIconRes)

        viewModel.shouldShowCrowdloanBanner.observe(headerAdapter::setCrowdloanBannerVisible)

        viewModel.assetViewModeModelFlow.observe { headerAdapter.setAssetViewModeModel(it) }
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

    override fun sendClicked() {
        viewModel.sendClicked()
    }

    override fun receiveClicked() {
        viewModel.receiveClicked()
    }

    override fun buyClicked() {
        viewModel.buyClicked()
    }

    override fun crowdloanBannerClicked() {
        viewModel.crowdloanBannerClicked()
    }

    override fun crowdloanBannerCloseClicked() {
        viewModel.crowdloanBannerCloseClicked()
    }

    override fun assetViewModeClicked() {
        viewModel.switchViewMode()
    }

    override fun swapClicked() {
        viewModel.swapClicked()
    }
}
