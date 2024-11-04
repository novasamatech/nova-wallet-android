package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import android.widget.CompoundButton
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.databinding.FragmentTransactionsFilterBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter

class TransactionHistoryFilterFragment : BaseFragment<TransactionHistoryFilterViewModel, FragmentTransactionsFilterBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "TransactionHistoryFilterFragment.Payload"
        fun getBundle(payload: TransactionHistoryFilterPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun createBinding() = FragmentTransactionsFilterBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.transactionsFilterToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.transactionsFilterToolbar.setRightActionClickListener {
            viewModel.resetFilter()
        }

        binder.transactionsFilterRewards.bindFilter(TransactionFilter.REWARD)
        binder.transactionsFilterSwitchTransfers.bindFilter(TransactionFilter.TRANSFER)
        binder.transactionsFilterOtherTransactions.bindFilter(TransactionFilter.EXTRINSIC)
        binder.transactionsFilterSwaps.bindFilter(TransactionFilter.SWAP)

        binder.transactionFilterApplyBtn.setOnClickListener { viewModel.applyClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        ).transactionHistoryComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: TransactionHistoryFilterViewModel) {
        viewModel.isApplyButtonEnabled.observe {
            binder.transactionFilterApplyBtn.setState(if (it) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }

    private fun CompoundButton.bindFilter(filter: TransactionFilter) {
        lifecycleScope.launchWhenResumed {
            bindFromMap(filter, viewModel.filtersEnabledMap(), viewLifecycleOwner.lifecycleScope)
        }
    }
}
