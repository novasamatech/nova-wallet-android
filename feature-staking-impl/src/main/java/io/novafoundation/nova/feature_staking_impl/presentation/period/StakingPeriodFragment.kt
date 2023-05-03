package io.novafoundation.nova.feature_staking_impl.presentation.period

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_period_staking.customPeriodSettings
import kotlinx.android.synthetic.main.fragment_period_staking.customStackingPeriodAlwaysToday
import kotlinx.android.synthetic.main.fragment_period_staking.customStackingPeriodEnd
import kotlinx.android.synthetic.main.fragment_period_staking.customStackingPeriodStart
import kotlinx.android.synthetic.main.fragment_period_staking.stackingPeriodGroup
import kotlinx.android.synthetic.main.fragment_period_staking.stackingPeriodToolbar

class StakingPeriodFragment : BaseFragment<StakingPeriodViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_period_staking, container, false)
    }

    override fun initViews() {
        stackingPeriodToolbar.applyStatusBarInsets()
        stackingPeriodToolbar.setRightActionClickListener { viewModel.onSaveClick() }
        stackingPeriodToolbar.setHomeButtonListener { viewModel.backClicked() }

        stackingPeriodGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.onPeriodChanged(checkedId)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .stakingPeriodComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StakingPeriodViewModel) {
        viewModel.saveButtonEnabledState.observe {
            stackingPeriodToolbar.setRightActionEnabled(it)
        }
    }
}
