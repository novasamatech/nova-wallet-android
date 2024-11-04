package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings

import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentCustomValidatorsSettingsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationPostProcessor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.HasIdentityFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotOverSubscribedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotSlashedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.postprocessors.RemoveClusteringPostprocessor

class CustomValidatorsSettingsFragment : BaseFragment<CustomValidatorsSettingsViewModel, FragmentCustomValidatorsSettingsBinding>() {

    override val binder by viewBinding(FragmentCustomValidatorsSettingsBinding::bind)

    override fun initViews() {
        binder.customValidatorSettingsApply.setOnClickListener { viewModel.applyChanges() }

        binder.customValidatorSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.customValidatorSettingsToolbar.setRightActionClickListener { viewModel.reset() }
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
        bindFilters(viewModel)
        bindPostprocessors(viewModel)

        binder.customValidatorSettingsSort.bindTo(viewModel.selectedSortingIdFlow, lifecycleScope)

        viewModel.isResetButtonEnabled.observe(binder.customValidatorSettingsToolbar.rightActionText::setEnabled)
        viewModel.isApplyButtonEnabled.observe {
            binder.customValidatorSettingsApply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }

        viewModel.tokenNameFlow.observe {
            binder.customValidatorSettingsSortTotalStake.text = getString(R.string.staking_validator_total_stake_token, it)
            binder.customValidatorSettingsSortOwnStake.text = getString(R.string.staking_filter_title_own_stake_token, it)
        }
    }

    private fun bindFilters(viewModel: CustomValidatorsSettingsViewModel) {
        val filterToView = listOf(
            HasIdentityFilter::class.java to binder.customValidatorSettingsFilterIdentity,
            NotSlashedFilter::class.java to binder.customValidatorSettingsFilterSlashes,
            NotOverSubscribedFilter::class.java to binder.customValidatorSettingsFilterOverSubscribed,
        )

        filterToView.onEach { (filterClass, view) -> view.field.bindFilter(filterClass) }

        viewModel.allAvailableFilters.observe { availableFilters ->
            filterToView.onEach { (filterClass, view) ->
                view.setVisible(filterClass in availableFilters)
            }
        }
    }

    private fun bindPostprocessors(viewModel: CustomValidatorsSettingsViewModel) {
        val postProcessorToView = listOf(
            RemoveClusteringPostprocessor::class.java to binder.customValidatorSettingsFilterClustering
        )

        postProcessorToView.onEach { (postProcessorClass, view) -> view.field.bindPostProcessor(postProcessorClass) }

        viewModel.availablePostProcessors.observe { availablePostProcessors ->
            postProcessorToView.onEach { (postProcessorClass, view) ->
                view.setVisible(postProcessorClass in availablePostProcessors)
            }
        }
    }

    private fun CompoundButton.bindPostProcessor(postProcessorClass: Class<out RecommendationPostProcessor>) {
        bindFromMap(postProcessorClass, viewModel.postProcessorsEnabledMap, lifecycleScope)
    }

    private fun CompoundButton.bindFilter(filterClass: Class<out RecommendationFilter>) {
        bindFromMap(filterClass, viewModel.filtersEnabledMap, lifecycleScope)
    }
}
