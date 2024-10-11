package io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

class ReferendaFiltersFragment : BaseFragment<ReferendaFiltersViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_referenda_filters, container, false)

    override fun initViews() {
        referendaFiltersToolbar.applyStatusBarInsets()
        referendaFiltersToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }
        referendaFiltersToolbar.setRightActionClickListener { referendaFilterAll.isChecked = true }

        referendaFiltersApplyButton.setOnClickListener {
            viewModel.onApplyFilters()
        }

        referendaFiltersTypeGroup.check(viewModel.getReferendumTypeSelectedOption())
        referendaFiltersTypeGroup.setOnCheckedChangeListener { _, checkedId ->
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
            referendaFiltersApplyButton.isEnabled = it
        }
    }
}
