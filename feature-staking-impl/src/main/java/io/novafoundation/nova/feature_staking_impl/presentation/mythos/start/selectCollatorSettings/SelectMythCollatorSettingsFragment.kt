package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMythosStakingSelectCollatorSettingsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel

private val SORT_MAPPING = mapOf(
    MythosCollatorSorting.REWARDS to R.id.selectCollatorSettingsSortRewards,
    MythosCollatorSorting.TOTAL_STAKE to R.id.selectCollatorSettingsSortTotalStake,
)

class SelectMythCollatorSettingsFragment : BaseFragment<SelectMythCollatorSettingsViewModel, FragmentMythosStakingSelectCollatorSettingsBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectMythCollatorSettingsFragment.Payload"

        fun getBundle(payload: MythCollatorRecommendationConfigParcel) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun createBinding() = FragmentMythosStakingSelectCollatorSettingsBinding.inflate(layoutInflater)

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
            .selectMythosSettingsFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectMythCollatorSettingsViewModel) {
        binder.selectCollatorSettingsSort.bindTo(viewModel.selectedSortingFlow, lifecycleScope, SORT_MAPPING)

        viewModel.isApplyButtonEnabled.observe {
            binder.selectCollatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }
}
