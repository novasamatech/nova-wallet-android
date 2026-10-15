package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetReviewBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.bindBittensorStakeResults

private const val PAYLOAD_KEY = "payload"

class BittensorSubnetReviewFragment : BaseFragment<BittensorSubnetReviewViewModel, FragmentBittensorSubnetReviewBinding>() {

    companion object {

        fun getBundle(payload: BittensorSubnetReviewPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentBittensorSubnetReviewBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argument<BittensorSubnetReviewPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetReviewComponentFactory().create(this, payload).inject(this)
    }

    override fun initViews() {
        binder.confirm.prepareForProgress(viewLifecycleOwner)
        binder.validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
        binder.account.setOnClickListener { viewModel.accountClicked() }
        binder.rate.setOnClickListener { showDescription(R.string.bittensor_exchange_rate, R.string.swap_rate_description) }
        binder.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, R.string.swap_slippage_description) }
        binder.networkFee.setOnClickListener { showDescription(R.string.network_fee, R.string.swap_network_fee_description) }
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
    }

    override fun subscribe(viewModel: BittensorSubnetReviewViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.feeCaption.observe { feeCaption.text = it }
        setupExternalActions(viewModel)
        viewModel.wallet.observe(wallet::showWallet)
        viewModel.account.observe(account::showAddress)
        viewModel.assets.observe(assets::setModel)
        viewModel.rate.observe(rate::showValue)
        slippage.showValue(viewModel.slippage)
        viewModel.validator.observe(validator::showValue)
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
        }
        viewModel.fee.observe(networkFee::setFeeStatus)
        viewModel.confirmButtonState.observe(confirm::setState)
        viewModel.walletChanged.observe { walletChanged.isVisible = it }
        bindBittensorStakeResults(
            results = viewModel.resultState,
            onDismiss = viewModel::resultDismissed,
            onTryAgain = viewModel::confirmClicked,
            onViewPosition = viewModel::viewPositionClicked
        )
    }

    private fun showDescription(titleRes: Int, descriptionRes: Int) {
        DescriptionBottomSheet(requireContext(), getString(titleRes), getString(descriptionRes)).show()
    }
}
