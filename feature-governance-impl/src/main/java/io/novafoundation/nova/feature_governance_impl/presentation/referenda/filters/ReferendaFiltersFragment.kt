package io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendaFiltersBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

class ReferendaFiltersFragment : BaseFragment<ReferendaFiltersViewModel, FragmentReferendaFiltersBinding>() {

    override val binder by viewBinding(FragmentReferendaFiltersBinding::bind)

    override fun initViews() {
        binder.referendaFiltersToolbar.applyStatusBarInsets()
        binder.referendaFiltersToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }
        binder.referendaFiltersToolbar.setRightActionClickListener { binder.referendaFilterAll.isChecked = true }

        binder.referendaFiltersApplyButton.setOnClickListener {
            viewModel.onApplyFilters()
        }

        binder.referendaFiltersTypeGroup.check(viewModel.getReferendumTypeSelectedOption())
        binder.referendaFiltersTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.onFilterTypeChanged(checkedId)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendaFiltersFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendaFiltersViewModel) {
        viewModel.isApplyButtonAvailableFlow.observe {
            binder.referendaFiltersApplyButton.isEnabled = it
        }
    }
}
