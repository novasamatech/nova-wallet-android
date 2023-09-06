package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsClearFilters
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsContainer
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsCount
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsDeselectAll
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsFillWithRecommended
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsList
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsNext
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsSorting
import kotlinx.android.synthetic.main.fragment_select_custom_validators.selectCustomValidatorsToolbar

class SelectCustomValidatorsFragment : BaseFragment<SelectCustomValidatorsViewModel>(), StakeTargetAdapter.ItemHandler<Validator> {

    companion object {

        private const val KEY_PAYLOAD = "SelectCustomValidatorsFragment.Payload"

        fun getBundle(
            payload: CustomValidatorsPayload
        ) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    var filterAction: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_custom_validators, container, false)
    }

    override fun initViews() {
        selectCustomValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        selectCustomValidatorsList.adapter = adapter
        selectCustomValidatorsList.setHasFixedSize(true)

        selectCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        filterAction = selectCustomValidatorsToolbar.addCustomAction(R.drawable.ic_filter) {
            viewModel.settingsClicked()
        }

        selectCustomValidatorsToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        selectCustomValidatorsList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        selectCustomValidatorsFillWithRecommended.setOnClickListener { viewModel.fillRestWithRecommended() }
        selectCustomValidatorsClearFilters.setOnClickListener { viewModel.clearFilters() }
        selectCustomValidatorsDeselectAll.setOnClickListener { viewModel.deselectAll() }

        selectCustomValidatorsNext.setOnClickListener { viewModel.nextClicked() }
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

        viewModel.selectedTitle.observe(selectCustomValidatorsCount::setText)

        viewModel.buttonState.observe {
            selectCustomValidatorsNext.text = it.text

            val state = if (it.enabled) ButtonState.NORMAL else ButtonState.DISABLED

            selectCustomValidatorsNext.setState(state)
        }

        viewModel.scoringHeader.observe(selectCustomValidatorsSorting::setText)

        viewModel.fillWithRecommendedEnabled.observe(selectCustomValidatorsFillWithRecommended::setEnabled)
        viewModel.clearFiltersEnabled.observe(selectCustomValidatorsClearFilters::setEnabled)
        viewModel.deselectAllEnabled.observe(selectCustomValidatorsDeselectAll::setEnabled)

        viewModel.recommendationSettingsIcon.observe { icon ->
            filterAction?.setImageResource(icon)
        }
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorModel) {
        viewModel.validatorClicked(validatorModel)
    }
}
