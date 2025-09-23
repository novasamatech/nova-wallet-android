package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.insets.applyBarMargin
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.databinding.FragmentBalanceDetailBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.setupButSellActionButton
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.setupBuySellSelectorMixin
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import io.novafoundation.nova.feature_assets.presentation.transaction.history.showState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.view.setTotalAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import javax.inject.Inject

private const val KEY_TOKEN = "KEY_TOKEN"

class BalanceDetailFragment : BaseFragment<BalanceDetailViewModel, FragmentBalanceDetailBinding>() {

    companion object {

        fun getBundle(assetPayload: AssetPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_TOKEN, assetPayload)
            }
        }
    }

    override fun createBinding() = FragmentBalanceDetailBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun applyInsets(rootView: View) {
        binder.root.applyNavigationBarInsets(consume = false)
        binder.balanceDetailBack.applyBarMargin()
    }

    override fun initViews() {
        hideKeyboard()

        binder.transfersContainer.initializeBehavior(anchorView = binder.balanceDetailContent)

        binder.transfersContainer.setScrollingListener(viewModel::transactionsScrolled)

        binder.transfersContainer.setSlidingStateListener(::setRefreshEnabled)

        binder.transfersContainer.setTransactionClickListener(viewModel::transactionClicked)

        binder.transfersContainer.setFilterClickListener { viewModel.filterClicked() }

        binder.balanceDetailContainer.setOnRefreshListener {
            viewModel.sync()
        }

        binder.balanceDetailBack.setOnClickListener { viewModel.backClicked() }

        binder.balanceDetailActions.send.setOnClickListener {
            viewModel.sendClicked()
        }

        binder.balanceDetailActions.swap.setOnClickListener {
            viewModel.swapClicked()
        }

        binder.balanceDetailActions.receive.setOnClickListener {
            viewModel.receiveClicked()
        }

        binder.balanceDetailsBalances.locked.setOnClickListener {
            viewModel.lockedInfoClicked()
        }
    }

    override fun inject() {
        val token = argument<AssetPayload>(KEY_TOKEN)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .balanceDetailComponentFactory()
            .create(this, token)
            .inject(this)
    }

    override fun subscribe(viewModel: BalanceDetailViewModel) {
        setupBuySellSelectorMixin(viewModel.buySellSelectorMixin)
        setupButSellActionButton(viewModel.buySellSelectorMixin, binder.balanceDetailActions.buySell)

        viewModel.state.observe(binder.transfersContainer::showState)

        viewModel.assetDetailsModel.observe { asset ->
            binder.balanceDetailTokenIcon.setTokenIcon(asset.assetIcon, imageLoader)
            binder.balanceDetailTokenName.text = asset.token.configuration.symbol.value

            binder.balanceDetailsBalances.setTotalAmount(asset.total)
            binder.balanceDetailsBalances.transferable.showAmount(asset.transferable)
            binder.balanceDetailsBalances.locked.showAmount(asset.locked)
        }

        viewModel.supportExpandableBalanceDetails.observe {
            binder.balanceDetailsBalances.showBalanceDetails(it)
        }

        viewModel.priceChartFormatters.observe {
            binder.priceChartView.setTextInjectors(it.price, it.priceChange, it.date)
        }

        viewModel.priceChartTitle.observe {
            binder.priceChartView.setTitle(it)
        }

        viewModel.priceChartModels.observe {
            if (it == null) {
                binder.priceChartView.isGone = true
                return@observe
            }

            binder.priceChartView.setCharts(it)
        }

        viewModel.hideRefreshEvent.observeEvent {
            binder.balanceDetailContainer.isRefreshing = false
        }

        viewModel.showLockedDetailsEvent.observeEvent(::showLockedDetails)

        viewModel.sendEnabled.observe(binder.balanceDetailActions.send::setEnabled)

        viewModel.swapButtonEnabled.observe(binder.balanceDetailActions.swap::setEnabled)

        viewModel.acknowledgeLedgerWarning.awaitableActionLiveData.observeEvent {
            LedgerNotSupportedWarningBottomSheet(
                context = requireContext(),
                onSuccess = { it.onSuccess(Unit) },
                message = it.payload
            ).show()
        }

        viewModel.chainUI.observe {
            binder.balanceDetailsChain.setChain(it)
        }
    }

    private fun setRefreshEnabled(bottomSheetState: Int) {
        val bottomSheetCollapsed = BottomSheetBehavior.STATE_COLLAPSED == bottomSheetState
        binder.balanceDetailContainer.isEnabled = bottomSheetCollapsed
    }

    private fun showLockedDetails(model: BalanceLocksModel) {
        LockedTokensBottomSheet(requireContext(), model).show()
    }
}
