package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationPostProcessor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.HasIdentityFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotOverSubscribedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotSlashedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.postprocessors.RemoveClusteringPostprocessor
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsApply
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsFilterClustering
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsFilterIdentity
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsFilterOverSubscribed
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsFilterSlashes
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsSort
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsSortOwnStake
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsSortTotalStake
import kotlinx.android.synthetic.main.fragment_custom_validators_settings.customValidatorSettingsToolbar

class CustomValidatorsSettingsFragment : BaseFragment<CustomValidatorsSettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_validators_settings, container, false)
    }

    override fun initViews() {
        customValidatorSettingsApply.setOnClickListener { viewModel.applyChanges() }

        customValidatorSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        customValidatorSettingsToolbar.setRightActionClickListener { viewModel.reset() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .customValidatorsSettingsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CustomValidatorsSettingsViewModel) {
        customValidatorSettingsSort.bindTo(viewModel.selectedSortingIdFlow, lifecycleScope)

        customValidatorSettingsFilterIdentity.bindFilter(HasIdentityFilter::class.java)
        customValidatorSettingsFilterSlashes.bindFilter(NotSlashedFilter::class.java)
        customValidatorSettingsFilterOverSubscribed.bindFilter(NotOverSubscribedFilter::class.java)
        customValidatorSettingsFilterClustering.bindPostProcessor(RemoveClusteringPostprocessor::class.java)

        viewModel.isResetButtonEnabled.observe(customValidatorSettingsToolbar.rightActionText::setEnabled)
        viewModel.isApplyButtonEnabled.observe {
            customValidatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }

        viewModel.tokenNameFlow.observe {
            customValidatorSettingsSortTotalStake.text = getString(R.string.staking_validator_total_stake_token, it)
            customValidatorSettingsSortOwnStake.text = getString(R.string.staking_filter_title_own_stake_token, it)
        }
    }

    private fun CompoundButton.bindPostProcessor(postProcessorClass: Class<out RecommendationPostProcessor>) {
        bindFromMap(postProcessorClass, viewModel.postProcessorsEnabledMap, lifecycleScope)
    }

    private fun CompoundButton.bindFilter(filterClass: Class<out RecommendationFilter>) {
        bindFromMap(filterClass, viewModel.filtersEnabledMap, lifecycleScope)
    }
}
