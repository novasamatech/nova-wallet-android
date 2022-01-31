package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view.AssetGroupingDecoration
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view.AssetsHeaderAdapter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view.BalanceListAdapter
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import kotlinx.android.synthetic.main.fragment_balance_list.balanceListAssets
import kotlinx.android.synthetic.main.fragment_balance_list.walletContainer
import javax.inject.Inject

class BalanceListFragment : BaseFragment<BalanceListViewModel>(), BalanceListAdapter.ItemAssetHandler, AssetsHeaderAdapter.Handler {

    @Inject protected lateinit var imageLoader: ImageLoader

    private val assetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceListAdapter(imageLoader, this)
    }

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AssetsHeaderAdapter(this)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, assetsAdapter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_balance_list, container, false)
    }

    override fun initViews() {
        balanceListAssets.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        hideKeyboard()

        balanceListAssets.setHasFixedSize(true)
        balanceListAssets.adapter = adapter

        val groupBackground = with(requireContext()) {
            addRipple(getRoundedCornerDrawable(R.color.blurColor))
        }
        val decoration = AssetGroupingDecoration(
            background = groupBackground,
            assetsAdapter = assetsAdapter,
            context = requireContext(),
            isApplicable = { globalPosition -> globalPosition > headerAdapter.itemCount - 1 }
        )
        balanceListAssets.addItemDecoration(decoration)

        walletContainer.setOnRefreshListener {
            viewModel.sync()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
        )
            .balanceListComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BalanceListViewModel) {
        viewModel.sync()

        viewModel.assetsFlow.observe {
            assetsAdapter.submitListPreservingViewPoint(it, balanceListAssets)
        }

        viewModel.totalBalanceFlow.observe(headerAdapter::setTotalBalance)

        viewModel.currentAddressModelFlow.observe(headerAdapter::setAddress)

        viewModel.hideRefreshEvent.observeEvent {
            walletContainer.isRefreshing = false
        }
    }

    override fun assetClicked(asset: AssetModel) {
        viewModel.assetClicked(asset)
    }

    override fun avatarClicked() {
        viewModel.avatarClicked()
    }
}
