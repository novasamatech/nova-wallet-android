package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select

import android.os.Bundle
import android.widget.ImageView

import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSelectCustomValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload

class SelectCustomValidatorsFragment :
    BaseFragment<SelectCustomValidatorsViewModel, FragmentSelectCustomValidatorsBinding>(),
    StakeTargetAdapter.ItemHandler<Validator> {

    companion object {

        private const val KEY_PAYLOAD = "SelectCustomValidatorsFragment.Payload"

        fun getBundle(
            payload: CustomValidatorsPayload
        ) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun createBinding() = FragmentSelectCustomValidatorsBinding.inflate(layoutInflater)

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    var filterAction: ImageView? = null

    override fun initViews() {
        binder.selectCustomValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        binder.selectCustomValidatorsList.adapter = adapter
        binder.selectCustomValidatorsList.setHasFixedSize(true)

        binder.selectCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        filterAction = binder.selectCustomValidatorsToolbar.addCustomAction(R.drawable.ic_filter) {
            viewModel.settingsClicked()
        }

        binder.selectCustomValidatorsToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        binder.selectCustomValidatorsList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        binder.selectCustomValidatorsFillWithRecommended.setOnClickListener { viewModel.fillRestWithRecommended() }
        binder.selectCustomValidatorsClearFilters.setOnClickListener { viewModel.clearFilters() }
        binder.selectCustomValidatorsDeselectAll.setOnClickListener { viewModel.deselectAll() }

        binder.selectCustomValidatorsNext.setOnClickListener { viewModel.nextClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        filterAction = null
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCustomValidatorsComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCustomValidatorsViewModel) {
        viewModel.validatorModelsFlow.observe(adapter::submitList)

        viewModel.selectedTitle.observe(binder.selectCustomValidatorsCount::setText)

        viewModel.buttonState.observe {
            binder.selectCustomValidatorsNext.text = it.text

            val state = if (it.enabled) ButtonState.NORMAL else ButtonState.DISABLED

            binder.selectCustomValidatorsNext.setState(state)
        }

        viewModel.scoringHeader.observe(binder.selectCustomValidatorsSorting::setText)

        viewModel.fillWithRecommendedEnabled.observe(binder.selectCustomValidatorsFillWithRecommended::setEnabled)
        viewModel.clearFiltersEnabled.observe(binder.selectCustomValidatorsClearFilters::setEnabled)
        viewModel.deselectAllEnabled.observe(binder.selectCustomValidatorsDeselectAll::setEnabled)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: ValidatorStakeTargetModel) {
        viewModel.validatorInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: ValidatorStakeTargetModel) {
        viewModel.validatorClicked(stakeTargetModel)
    }
}
