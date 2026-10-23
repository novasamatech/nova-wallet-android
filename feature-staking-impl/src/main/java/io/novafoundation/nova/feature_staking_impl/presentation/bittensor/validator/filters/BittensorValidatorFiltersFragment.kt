package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorValidatorFiltersBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting

class BittensorValidatorFiltersFragment : BaseFragment<BittensorValidatorFiltersViewModel, FragmentBittensorValidatorFiltersBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "payload"

        fun getBundle(request: BittensorValidatorFiltersCommunicator.Request) = Bundle().apply { putParcelable(PAYLOAD_KEY, request) }
    }

    override fun createBinding() = FragmentBittensorValidatorFiltersBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorValidatorFiltersComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        toolbar.setRightActionClickListener { viewModel.reset() }
        onBackPressed { viewModel.back() }
        apply.setOnClickListener { viewModel.apply() }
    }

    override fun subscribe(viewModel: BittensorValidatorFiltersViewModel) = with(binder) {
        totalStaked.text = viewModel.tokenSymbol?.let { getString(R.string.bittensor_validator_sort_total_staked, it) }
            ?: getString(R.string.staking_total_staked)
        viewModel.filters.observe { filters ->
            hideUnusual.field.setOnCheckedChangeListener(null)
            hideUnusual.field.isChecked = filters.hideUnusualApy
            hideUnusual.field.setOnCheckedChangeListener { _, checked -> viewModel.hideUnusualApyChanged(checked) }

            sorting.setOnCheckedChangeListener(null)
            sorting.check(filters.sorting.radioId())
            sorting.setOnCheckedChangeListener { _, checkedId -> sortingFor(checkedId)?.let(viewModel::sortingChanged) }
        }
        viewModel.isResetEnabled.observe(toolbar.rightActionText::setEnabled)
        viewModel.isApplyEnabled.observe { apply.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED) }
    }

    private fun BittensorValidatorSorting.radioId(): Int = when (this) {
        BittensorValidatorSorting.APY -> R.id.apy
        BittensorValidatorSorting.TOTAL_STAKE -> R.id.totalStaked
        BittensorValidatorSorting.NAME -> R.id.name
    }

    private fun sortingFor(radioId: Int): BittensorValidatorSorting? = when (radioId) {
        R.id.apy -> BittensorValidatorSorting.APY
        R.id.totalStaked -> BittensorValidatorSorting.TOTAL_STAKE
        R.id.name -> BittensorValidatorSorting.NAME
        else -> null
    }
}
