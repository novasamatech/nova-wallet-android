
package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.model.PendingPayoutsStatisticsModel

class PayoutsListFragment : BaseFragment<PayoutsListViewModel>(), PayoutAdapter.ItemHandler {

    lateinit var adapter: PayoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payouts_list, container, false)
    }

    override fun initViews() {
        payoutsListContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        adapter = PayoutAdapter(this)
        payoutsList.adapter = adapter

        payoutsList.setHasFixedSize(true)

        payoutsListToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        payoutsListAll.setOnClickListener {
            viewModel.payoutAllClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .payoutsListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PayoutsListViewModel) {
        viewModel.payoutsStatisticsState.observe {
            if (it is LoadingState.Loaded<PendingPayoutsStatisticsModel>) {
                val placeholderVisible = it.data.placeholderVisible

                setContentVisible(!placeholderVisible)
                payoutListPlaceholder.setVisible(placeholderVisible)
                payoutsListProgress.makeGone()

                adapter.submitList(it.data.payouts)

                payoutsListAll.text = it.data.payoutAllTitle
            }
        }

        observeRetries(viewModel)
    }

    override fun payoutClicked(index: Int) {
        viewModel.payoutClicked(index)
    }

    private fun setContentVisible(visible: Boolean) {
        payoutsList.setVisible(visible)
        payoutsListAll.setVisible(visible)
    }
}
