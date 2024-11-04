package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingSelectCollatorSettingsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel

private val SORT_MAPPING = mapOf(
    CollatorSorting.REWARDS to R.id.selectCollatorSettingsSortRewards,
    CollatorSorting.MIN_STAKE to R.id.selectCollatorSettingsSortMinimumStake,
    CollatorSorting.TOTAL_STAKE to R.id.selectCollatorSettingsSortTotalStake,
    CollatorSorting.OWN_STAKE to R.id.selectCollatorSettingsSortOwnStake,
)

class SelectCollatorSettingsFragment : BaseFragment<SelectCollatorSettingsViewModel, FragmentParachainStakingSelectCollatorSettingsBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectCollatorSettingsFragment.Payload"

        fun getBundle(payload: CollatorRecommendationConfigParcelModel) = bundleOf(PAYLOAD_KEY to payload)
    }

    override val binder by viewBinding(FragmentParachainStakingSelectCollatorSettingsBinding::bind)

    override fun initViews() {
        binder.selectCollatorSettingsApply.setOnClickListener { viewModel.applyChanges() }

        binder.selectCollatorSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.selectCollatorSettingsToolbar.setRightActionClickListener { viewModel.reset() }
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
        binder.selectCollatorSettingsSort.bindTo(viewModel.selectedSortingFlow, lifecycleScope, SORT_MAPPING)

        viewModel.isApplyButtonEnabled.observe {
            binder.selectCollatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }
}
