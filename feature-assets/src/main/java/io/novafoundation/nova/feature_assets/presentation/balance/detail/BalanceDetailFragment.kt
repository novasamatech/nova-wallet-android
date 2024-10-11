package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyBarMargin
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import io.novafoundation.nova.feature_assets.presentation.transaction.history.showState
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

import javax.inject.Inject

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
        balanceDetailTokenName.applyBarMargin()

        transfersContainer.initializeBehavior(anchorView = balanceDetailContent)

        transfersContainer.setScrollingListener(viewModel::transactionsScrolled)

        transfersContainer.setSlidingStateListener(::setRefreshEnabled)

        transfersContainer.setTransactionClickListener(viewModel::transactionClicked)

        transfersContainer.setFilterClickListener { viewModel.filterClicked() }

        balanceDetailContainer.setOnRefreshListener {
            viewModel.sync()
        }

        balanceDetailBack.setOnClickListener { viewModel.backClicked() }

        balanceDetaiActions.send.setOnClickListener {
            viewModel.sendClicked()
        }

        balanceDetaiActions.swap.setOnClickListener {
            viewModel.swapClicked()
        }

        balanceDetaiActions.receive.setOnClickListener {
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
        buyMixinUi.setupBuyButton(this, balanceDetaiActions.buy, viewModel.buyEnabled) {
            viewModel.buyClicked()
        }

        viewModel.assetDetailsModel.observe { asset ->
            balanceDetailTokenIcon.loadTokenIcon(asset.token.configuration.iconUrl, imageLoader)
            balanceDetailTokenName.text = asset.token.configuration.symbol.value

            balanceDetailRate.text = asset.token.rate

            balanceDetailRateChange.setTextColorRes(asset.token.rateChangeColorRes)
            balanceDetailRateChange.text = asset.token.recentRateChange

            balanceDetailsBalances.total.showAmount(asset.total)
            balanceDetailsBalances.transferable.showAmount(asset.transferable)
            balanceDetailsBalances.locked.showAmount(asset.locked)
        }

        viewModel.hideRefreshEvent.observeEvent {
            balanceDetailContainer.isRefreshing = false
        }

        viewModel.showLockedDetailsEvent.observeEvent(::showLockedDetails)

        viewModel.sendEnabled.observe(balanceDetaiActions.send::setEnabled)

        viewModel.swapButtonEnabled.observe(balanceDetaiActions.swap::setEnabled)

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
