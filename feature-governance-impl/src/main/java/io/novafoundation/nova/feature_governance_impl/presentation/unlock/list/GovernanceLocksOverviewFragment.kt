package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import kotlinx.android.synthetic.main.fragment_governance_locks_overview.governanceLockedTokens
import kotlinx.android.synthetic.main.fragment_governance_locks_overview.governanceLockedTokensToolbar

class GovernanceLocksOverviewFragment : BaseFragment<GovernanceLocksOverviewViewModel>() {

    private val headerAdapter = TotalGovernanceLocksHeaderAdapter()

    private val listAdapter = UnlockableTokensAdapter()

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, listAdapter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_governance_locks_overview, container, false)
    }

    override fun initViews() {
        governanceLockedTokensToolbar.setHomeButtonListener { viewModel.backClicked() }

        governanceLockedTokens.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .governanceLocksOverviewFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: GovernanceLocksOverviewViewModel) {
        viewModel.totalAmount.observe {
            headerAdapter.setAmount(it)
        }

        viewModel.lockModels.observe {
            listAdapter.submitList(it)
        }
    }
}
