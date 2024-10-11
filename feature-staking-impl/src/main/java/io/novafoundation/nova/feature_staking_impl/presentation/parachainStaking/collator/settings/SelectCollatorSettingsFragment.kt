package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel

private val SORT_MAPPING = mapOf(
    CollatorSorting.REWARDS to R.id.selectCollatorSettingsSortRewards,
    CollatorSorting.MIN_STAKE to R.id.selectCollatorSettingsSortMinimumStake,
    CollatorSorting.TOTAL_STAKE to R.id.selectCollatorSettingsSortTotalStake,
    CollatorSorting.OWN_STAKE to R.id.selectCollatorSettingsSortOwnStake,
)

class SelectCollatorSettingsFragment : BaseFragment<SelectCollatorSettingsViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectCollatorSettingsFragment.Payload"

        fun getBundle(payload: CollatorRecommendationConfigParcelModel) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_select_collator_settings, container, false)
    }

    override fun initViews() {
        selectCollatorSettingsApply.setOnClickListener { viewModel.applyChanges() }

        selectCollatorSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        selectCollatorSettingsToolbar.setRightActionClickListener { viewModel.reset() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCollatorSettingsFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCollatorSettingsViewModel) {
        selectCollatorSettingsSort.bindTo(viewModel.selectedSortingFlow, lifecycleScope, SORT_MAPPING)

        viewModel.isApplyButtonEnabled.observe {
            selectCollatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }
}
