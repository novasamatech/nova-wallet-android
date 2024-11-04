package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentGovernanceLocksOverviewBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent

class GovernanceLocksOverviewFragment : BaseFragment<GovernanceLocksOverviewViewModel, FragmentGovernanceLocksOverviewBinding>() {

    override val binder by viewBinding(FragmentGovernanceLocksOverviewBinding::bind)

    private val headerAdapter = TotalGovernanceLocksHeaderAdapter()

    private val listAdapter = UnlockableTokensAdapter()

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, listAdapter)
    }

    override fun initViews() {
        binder.governanceLockedTokensToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.governanceUnlockTokensButton.setOnClickListener { viewModel.unlockClicked() }

        binder.governanceLockedTokens.adapter = adapter
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
                binder.governanceLockedTokens.makeVisible()
                binder.governanceTokensProgress.makeGone()
            } else {
                binder.governanceLockedTokens.makeGone()
                binder.governanceTokensProgress.makeVisible()
            }
        }

        viewModel.isUnlockAvailable.observe {
            if (it is LoadingState.Loaded) {
                binder.governanceUnlockTokensButton.isEnabled = it.data
            } else {
                binder.governanceUnlockTokensButton.isEnabled = false
            }
        }
    }
}
