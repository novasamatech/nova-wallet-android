package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyBarMargin
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import io.novafoundation.nova.feature_assets.presentation.transaction.history.showState
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.view.setTotalAmount
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailActions
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailBack
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailContainer
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailContent
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailTokenIcon
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailTokenName
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailsBalances
import kotlinx.android.synthetic.main.fragment_balance_detail.transfersContainer
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_balance_detail.priceChartView

private const val KEY_TOKEN = "KEY_TOKEN"

class BalanceDetailFragment : BaseFragment<BalanceDetailViewModel>() {

    companion object {

        fun getBundle(assetPayload: AssetPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_TOKEN, assetPayload)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var buyMixinUi: BuyMixinUi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_balance_detail, container, false)
    }

    override fun initViews() {
        hideKeyboard()

        balanceDetailBack.applyBarMargin()

        transfersContainer.initializeBehavior(anchorView = balanceDetailContainer)

        transfersContainer.setScrollingListener(viewModel::transactionsScrolled)

        transfersContainer.setSlidingStateListener(::setRefreshEnabled)

        transfersContainer.setTransactionClickListener(viewModel::transactionClicked)

        transfersContainer.setFilterClickListener { viewModel.filterClicked() }

        balanceDetailContainer.setOnRefreshListener {
            viewModel.sync()
        }

        balanceDetailBack.setOnClickListener { viewModel.backClicked() }

        balanceDetailActions.send.setOnClickListener {
            viewModel.sendClicked()
        }

        balanceDetailActions.swap.setOnClickListener {
            viewModel.swapClicked()
        }

        balanceDetailActions.receive.setOnClickListener {
            viewModel.receiveClicked()
        }

        balanceDetailsBalances.locked.setOnClickListener {
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
        viewModel.state.observe(transfersContainer::showState)

        buyMixinUi.setupBuyIntegration(this, viewModel.buyMixin)
        buyMixinUi.setupBuyButton(this, balanceDetailActions.buy, viewModel.buyEnabled) {
            viewModel.buyClicked()
        }

        viewModel.assetDetailsModel.observe { asset ->
            balanceDetailTokenIcon.setTokenIcon(asset.assetIcon, imageLoader)
            balanceDetailTokenName.text = asset.token.configuration.symbol.value

            balanceDetailsBalances.setTotalAmount(asset.total)
            balanceDetailsBalances.transferable.showAmount(asset.transferable)
            balanceDetailsBalances.locked.showAmount(asset.locked)
        }

        viewModel.priceChartFormatters.observe {
            priceChartView.setTextInjectors(it.price, it.priceChange, it.date)
        }

        viewModel.priceChartTitle.observe {
            priceChartView.setTitle(it)
        }

        viewModel.priceChartModels.observe {
            if (it == null) {
                priceChartView.isGone = true
                return@observe
            }

            priceChartView.setCharts(it)
        }

        viewModel.hideRefreshEvent.observeEvent {
            balanceDetailContainer.isRefreshing = false
        }

        viewModel.showLockedDetailsEvent.observeEvent(::showLockedDetails)

        viewModel.sendEnabled.observe(balanceDetailActions.send::setEnabled)

        viewModel.swapButtonEnabled.observe(balanceDetailActions.swap::setEnabled)

        viewModel.acknowledgeLedgerWarning.awaitableActionLiveData.observeEvent {
            LedgerNotSupportedWarningBottomSheet(
                context = requireContext(),
                onSuccess = { it.onSuccess(Unit) },
                message = it.payload
            ).show()
        }
    }

    private fun setRefreshEnabled(bottomSheetState: Int) {
        val bottomSheetCollapsed = BottomSheetBehavior.STATE_COLLAPSED == bottomSheetState
        balanceDetailContainer.isEnabled = bottomSheetCollapsed
    }

    private fun showLockedDetails(model: BalanceLocksModel) {
        LockedTokensBottomSheet(requireContext(), model).show()
    }
}
