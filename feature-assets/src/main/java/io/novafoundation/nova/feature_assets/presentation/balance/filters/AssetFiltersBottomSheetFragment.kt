package io.novafoundation.nova.feature_assets.presentation.balance.filters

import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.databinding.FragmentAssetFiltersBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter

class AssetFiltersBottomSheetFragment : BaseBottomSheetFragment<AssetFiltersViewModel, FragmentAssetFiltersBinding>() {

    override val binder by viewBinding(FragmentAssetFiltersBinding::bind)

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
        binder.assetsFilterSwitchZeroBalances.bindFromMap(NonZeroBalanceFilter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)
    }
}
