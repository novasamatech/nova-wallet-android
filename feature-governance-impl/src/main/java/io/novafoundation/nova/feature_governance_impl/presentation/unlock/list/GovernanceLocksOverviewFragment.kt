package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

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
        governanceUnlockTokensButton.setOnClickListener { viewModel.unlockClicked() }

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
            if (it is LoadingState.Loaded) {
                listAdapter.submitList(it.data)
                governanceLockedTokens.makeVisible()
                governanceTokensProgress.makeGone()
            } else {
                governanceLockedTokens.makeGone()
                governanceTokensProgress.makeVisible()
            }
        }

        viewModel.isUnlockAvailable.observe {
            if (it is LoadingState.Loaded) {
                governanceUnlockTokensButton.isEnabled = it.data
            } else {
                governanceUnlockTokensButton.isEnabled = false
            }
        }
    }
}
