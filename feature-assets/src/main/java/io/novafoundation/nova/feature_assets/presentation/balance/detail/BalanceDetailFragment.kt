package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyBarMargin
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.setupBuyIntegration
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.transaction.history.showState
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetaiActions
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailBack
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailContainer
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailContent
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailRate
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailRateChange
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailTokenIcon
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailTokenName
import kotlinx.android.synthetic.main.fragment_balance_detail.balanceDetailsBalances
import kotlinx.android.synthetic.main.fragment_balance_detail.transfersContainer
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

    @Inject lateinit var imageLoader: ImageLoader

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

        balanceDetaiActions.receive.setOnClickListener {
            viewModel.receiveClicked()
        }

        balanceDetailsBalances.locked.setOnClickListener {
            viewModel.lockedInfoClicked()
        }
    }

    override fun inject() {
        val token = arguments!![KEY_TOKEN] as AssetPayload

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .balanceDetailComponentFactory()
            .create(this, token)
            .inject(this)
    }

    override fun subscribe(viewModel: BalanceDetailViewModel) {
        viewModel.sync()

        viewModel.state.observe(transfersContainer::showState)

        setupBuyIntegration(
            mixin = viewModel.buyMixin,
            buyButton = balanceDetaiActions.buy,
            customBuyClick = { viewModel.buyClicked() }
        )

        viewModel.assetDetailsModel.observe { asset ->
            balanceDetailTokenIcon.load(asset.token.configuration.iconUrl, imageLoader)
            balanceDetailTokenName.text = asset.token.configuration.symbol

            balanceDetailRate.text = asset.token.dollarRate

            balanceDetailRateChange.setTextColorRes(asset.token.rateChangeColorRes)
            balanceDetailRateChange.text = asset.token.recentRateChange

            balanceDetailsBalances.total.showAmount(asset.total)
            balanceDetailsBalances.transferable.showAmount(asset.transferable)
            balanceDetailsBalances.locked.showAmount(asset.locked)
        }

        viewModel.hideRefreshEvent.observeEvent {
            balanceDetailContainer.isRefreshing = false
        }

        viewModel.showFrozenDetailsEvent.observeEvent(::showLockedDetails)

        viewModel.sendEnabled.observe(balanceDetaiActions.send::setEnabled)
    }

    private fun setRefreshEnabled(bottomSheetState: Int) {
        val bottomSheetCollapsed = BottomSheetBehavior.STATE_COLLAPSED == bottomSheetState
        balanceDetailContainer.isEnabled = bottomSheetCollapsed
    }

    private fun showLockedDetails(model: AssetModel) {
        LockedTokensBottomSheet(requireContext(), model).show()
    }
}
