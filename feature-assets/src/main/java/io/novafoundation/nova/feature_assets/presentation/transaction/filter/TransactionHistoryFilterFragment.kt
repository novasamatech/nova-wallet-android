package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import kotlinx.android.synthetic.main.fragment_transactions_filter.transactionFilterApplyBtn
import kotlinx.android.synthetic.main.fragment_transactions_filter.transactionsFilterOtherTransactions
import kotlinx.android.synthetic.main.fragment_transactions_filter.transactionsFilterRewards
import kotlinx.android.synthetic.main.fragment_transactions_filter.transactionsFilterSwitchTransfers
import kotlinx.android.synthetic.main.fragment_transactions_filter.transactionsFilterToolbar

class TransactionHistoryFilterFragment : BaseFragment<TransactionHistoryFilterViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_transactions_filter, container, false)

    override fun initViews() {
        transactionsFilterToolbar.setHomeButtonListener { viewModel.backClicked() }

        transactionsFilterToolbar.setRightActionClickListener {
            viewModel.resetFilter()
        }

        transactionsFilterRewards.bindFilter(TransactionFilter.REWARD)
        transactionsFilterSwitchTransfers.bindFilter(TransactionFilter.TRANSFER)
        transactionsFilterOtherTransactions.bindFilter(TransactionFilter.EXTRINSIC)

        transactionFilterApplyBtn.setOnClickListener { viewModel.applyClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        ).transactionHistoryComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: TransactionHistoryFilterViewModel) {
        viewModel.isApplyButtonEnabled.observe {
            transactionFilterApplyBtn.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }

    private fun CompoundButton.bindFilter(filter: TransactionFilter) {
        bindFromMap(filter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)
    }
}
