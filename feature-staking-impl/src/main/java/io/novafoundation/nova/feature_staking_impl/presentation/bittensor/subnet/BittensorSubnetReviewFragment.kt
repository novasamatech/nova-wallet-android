package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.showLoadingValue
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
            .bittensorSubnetReviewComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews() {
        binder.execution.stepContainer.background = requireContext()
            .getBlockDrawable()
        binder.confirm.prepareForProgress(viewLifecycleOwner)
        binder.validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        binder.done.setOnClickListener { viewModel.viewPositionClicked() }
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
        binder.account.setOnClickListener { viewModel.accountClicked() }
        binder.rate.setOnClickListener { showDescription(R.string.bittensor_exchange_rate, R.string.swap_rate_description) }
        binder.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, R.string.swap_slippage_description) }
        binder.networkFee.setOnClickListener { showDescription(R.string.network_fee, R.string.swap_network_fee_description) }
        binder.execution.validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        binder.execution.rate.setOnClickListener { showDescription(R.string.bittensor_exchange_rate, R.string.swap_rate_description) }
        binder.execution.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, R.string.swap_slippage_description) }
        binder.execution.networkFee.setOnClickListener { showDescription(R.string.network_fee, R.string.swap_network_fee_description) }
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
    }

    override fun subscribe(viewModel: BittensorSubnetReviewViewModel) = with(binder) {
        observeValidations(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        viewModel.feeCaption.observe { feeCaption.text = it }
        setupExternalActions(viewModel)
        viewModel.wallet.observe(wallet::showWallet)
        viewModel.account.observe(account::showAddress)
        viewModel.assets.observe(assets::setModel)
        viewModel.executionAssets.observe(execution.assets::setModel)
        viewModel.executionSubtitle.observe { execution.subtitle.text = it }
        viewModel.showExecution.observe { show -> reviewContent.isVisible = !show; execution.root.isVisible = show }
        viewModel.isExecuting.observe { running ->
            if (running) execution.timer.setState(ExecutionTimerView.State.CountdownTimer(viewModel.remainingExecutionTime()))
        }
        viewModel.isCompleted.observe { complete ->
            done.isVisible = complete
            execution.root.setPadding(
                0,
                0,
                0,
                if (complete) 84.dp(requireContext()) else 0
            )
            if (complete) {
                execution.timer.setState(ExecutionTimerView.State.Success)
                execution.title.setText(R.string.common_completed)
                execution.operationLabel.setText(R.string.bittensor_exit_completed_operation)
            }
        }
        viewModel.rate.observe {
            rate.showValue(it)
            execution.rate.showValue(it)
        }
        slippage.showValue(viewModel.slippage)
        execution.slippage.showValue(viewModel.slippage)
        viewModel.validator.observe {
            validator.showValue(it)
            execution.validator.showValue(it)
        }
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
            execution.validator.image.isVisible = it != null
            execution.validator.image.setImageDrawable(it)
        }
        viewModel.monthlyEstimate.observe(monthlyTokens::showValue)
        viewModel.projectedAveragePrice.observe(projectedAveragePrice::showLoadingValue)
        viewModel.fee.observe {
            networkFee.setFeeStatus(it)
            execution.networkFee.setFeeStatus(it)
        }
        viewModel.confirmButtonState.observe(confirm::setState)
        bindBittensorStakeResults(
            results = viewModel.resultState,
            onDismiss = viewModel::resultDismissed,
            onTryAgain = viewModel::confirmClicked,
            onViewPosition = viewModel::viewPositionClicked
        )
    }

    private fun showDescription(
        titleRes: Int,
        descriptionRes: Int
    ) {
        DescriptionBottomSheet(requireContext(), getString(titleRes), getString(descriptionRes))
            .show()
    }
}
