package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings

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
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator_settings.selectCollatorSettingsApply
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator_settings.selectCollatorSettingsSort
import kotlinx.android.synthetic.main.fragment_parachain_staking_select_collator_settings.selectCollatorSettingsToolbar

private val SORT_MAPPING = mapOf(
    MythosCollatorSorting.REWARDS to R.id.selectCollatorSettingsSortRewards,
    MythosCollatorSorting.TOTAL_STAKE to R.id.selectCollatorSettingsSortTotalStake,
)

class SelectMythCollatorSettingsFragment : BaseFragment<SelectMythCollatorSettingsViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "SelectMythCollatorSettingsFragment.Payload"

        fun getBundle(payload: MythCollatorRecommendationConfigParcel) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mythos_staking_select_collator_settings, container, false)
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
            .selectMythosSettingsFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectMythCollatorSettingsViewModel) {
        selectCollatorSettingsSort.bindTo(viewModel.selectedSortingFlow, lifecycleScope, SORT_MAPPING)

        viewModel.isApplyButtonEnabled.observe {
            selectCollatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }
}
