package io.novafoundation.nova.feature_assets.presentation.balance.list

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.feature_assets.databinding.FragmentBalanceListBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.BalanceBreakdownBottomSheet
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetGroupingDecoration
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.common.applyDefaultTo
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetsHeaderAdapter
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

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

        AssetGroupingDecoration.applyDefaultTo(binder.balanceListAssets, assetsAdapter)

        // modification animations only harm here
        binder.balanceListAssets.itemAnimator = null

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
        viewModel.assetModelsFlow.observe {
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
    }

    override fun assetClicked(asset: AssetModel) {
        viewModel.assetClicked(asset)
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

    override fun filtersClicked() {
        viewModel.filtersClicked()
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

    override fun swapClicked() {
        viewModel.swapClicked()
    }
}
