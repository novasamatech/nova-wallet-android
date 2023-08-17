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
import io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem.RedeemViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingType.StakingTypeModel
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeDirectStaking
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypePoolStaking
import kotlinx.android.synthetic.main.fragment_setup_staking_type.setupStakingTypeToolbar

class SetupStakingTypeFragment : BaseFragment<SetupStakingTypeViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_staking_type, container, false)
    }

    override fun initViews() {
        setupStakingTypeToolbar.applyStatusBarInsets()
        setupStakingTypePoolStaking.setBackgroundRes(R.drawable.ic_pool_staking_banner_picture)
        setupStakingTypeDirectStaking.setBackgroundRes(R.drawable.ic_direct_staking_banner_picture)
        setupStakingTypePoolStaking.setModel(
            StakingTypeModel(
                "Pool Staking",
                false,
                listOf("Single condition"),
                null
            )
        )
        setupStakingTypeDirectStaking.setModel(
            StakingTypeModel(
                "Direct Staking",
                true,
                listOf("First condition", "Second Condition", "Third condition"),
                StakingTypeModel.StakingTarget.Model(
                    StakingTargetModel("Validator", "Hehe", R.color.text_tertiary, StakingTargetModel.Icon.Quantity("19"))
                )
            )
        )
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

    }
}
