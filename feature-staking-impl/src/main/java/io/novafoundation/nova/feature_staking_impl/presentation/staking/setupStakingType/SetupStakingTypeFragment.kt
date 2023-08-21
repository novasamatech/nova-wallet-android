package io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.adapter.SetupStakingTypeAdapter
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeList
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeToolbar

class SetupStakingTypeFragment : BaseFragment<SetupStakingTypeViewModel>(), SetupStakingTypeAdapter.ItemAssetHandler {

    private val adapter = SetupStakingTypeAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_staking_type, container, false)
    }

    override fun initViews() {
        setupStakingTypeToolbar.applyStatusBarInsets()
        setupStakingTypeList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setupStakingType()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SetupStakingTypeViewModel) {

        viewModel.availableToRewriteData.observe { setupStakingTypeToolbar.setRightActionEnabled(it) }

        viewModel.stakingTypeModels.observe {
            adapter.submitList(it)
        }
    }

    override fun stakingTypeClicked(item: EditableStakingTypeRVItem) {
        viewModel.selectStakingType(item)
    }
}
