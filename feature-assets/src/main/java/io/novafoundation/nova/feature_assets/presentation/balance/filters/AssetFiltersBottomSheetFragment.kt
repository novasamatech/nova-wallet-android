package io.novafoundation.nova.feature_assets.presentation.balance.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter

class AssetFiltersBottomSheetFragment : BaseBottomSheetFragment<AssetFiltersViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_asset_filters, container, false)

    override fun initViews() {}

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .assetFiltersComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AssetFiltersViewModel) {
        assetsFilterSwitchZeroBalances.bindFromMap(NonZeroBalanceFilter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)
    }
}
