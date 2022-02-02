package io.novafoundation.nova.feature_wallet_impl.presentation.balance.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import io.novafoundation.nova.feature_wallet_impl.domain.assets.filters.NonZeroBalanceFilter
import kotlinx.android.synthetic.main.fragment_asset_filters.assetsFilterApply
import kotlinx.android.synthetic.main.fragment_asset_filters.assetsFilterSwitchZeroBalances
import kotlinx.android.synthetic.main.fragment_asset_filters.assetsFilterToolbar
import javax.inject.Inject

class AssetFiltersFragment : BaseFragment<AssetFiltersViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_asset_filters, container, false)

    override fun initViews() {
        assetsFilterToolbar.applyStatusBarInsets()

        assetsFilterToolbar.setHomeButtonListener { viewModel.backClicked() }

        assetsFilterApply.setOnClickListener { viewModel.applyClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
        )
            .assetFiltersComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AssetFiltersViewModel) {
        assetsFilterSwitchZeroBalances.bindFromMap(NonZeroBalanceFilter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)
    }
}
