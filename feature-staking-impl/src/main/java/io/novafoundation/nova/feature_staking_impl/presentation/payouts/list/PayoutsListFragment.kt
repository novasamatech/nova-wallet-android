package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list

import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentPayoutsListBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.model.PendingPayoutsStatisticsModel

class PayoutsListFragment : BaseFragment<PayoutsListViewModel, FragmentPayoutsListBinding>(), PayoutAdapter.ItemHandler {

    override fun createBinding() = FragmentPayoutsListBinding.inflate(layoutInflater)

    lateinit var adapter: PayoutAdapter

    override fun initViews() {
        binder.payoutsListContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        adapter = PayoutAdapter(this)
        binder.payoutsList.adapter = adapter

        binder.payoutsList.setHasFixedSize(true)

        binder.payoutsListToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.payoutsListAll.setOnClickListener {
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
                binder.payoutListPlaceholder.setVisible(placeholderVisible)
                binder.payoutsListProgress.makeGone()

                adapter.submitList(it.data.payouts)

                binder.payoutsListAll.text = it.data.payoutAllTitle
            }
        }

        observeRetries(viewModel)
    }

    override fun payoutClicked(index: Int) {
        viewModel.payoutClicked(index)
    }

    private fun setContentVisible(visible: Boolean) {
        binder.payoutsList.setVisible(visible)
        binder.payoutsListAll.setVisible(visible)
    }
}
