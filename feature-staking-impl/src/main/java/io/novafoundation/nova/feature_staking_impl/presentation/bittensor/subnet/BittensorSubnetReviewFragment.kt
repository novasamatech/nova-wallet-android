package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setSelectable
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showTitleInfo
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showValidator
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetReviewBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showCompleted
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showUnconfirmed
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries

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
        binder.projectedAveragePrice.isVisible = viewModel.existingPosition
        binder.execution.stepContainer.background = requireContext()
            .getBlockDrawable()
        binder.confirm.prepareForProgress(viewLifecycleOwner)
        binder.validator.setSelectable(false)
        binder.validator.showTitleInfo()
        binder.done.setOnClickListener { viewModel.viewPositionClicked() }
        binder.retryExecution.setOnClickListener { viewModel.failureActionClicked() }
        binder.execution.toolbar.setHomeButtonListener { viewModel.back() }
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
        binder.account.setOnClickListener { viewModel.accountClicked() }
        // Only a new position explains the preset validator; an existing one opens its validator info.
        binder.validator.root.setOnClickListener {
            if (viewModel.existingPosition) {
                viewModel.validatorClicked()
            } else {
                showDescription(R.string.bittensor_validator, getString(R.string.bittensor_validator_description))
            }
        }
        binder.monthlyTokens.setOnClickListener {
            showDescription(R.string.bittensor_monthly_tokens, getString(R.string.bittensor_monthly_rewards_description))
        }
        binder.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, viewModel.slippageDescription) }
        binder.execution.validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        binder.execution.validator.setOnClickListener { viewModel.validatorClicked() }
        binder.execution.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, viewModel.slippageDescription) }
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
    }

    override fun subscribe(viewModel: BittensorSubnetReviewViewModel) = with(binder) {
        observeValidations(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.swapRateDescription.observe { description ->
            rate.setOnClickListener { description?.let { showDescription(R.string.bittensor_exchange_rate, it) } }
            execution.rate.setOnClickListener { description?.let { showDescription(R.string.bittensor_exchange_rate, it) } }
        }
        viewModel.networkFeeDescription.observe { description ->
            networkFee.setOnClickListener { showDescription(R.string.network_fee, description) }
            execution.networkFee.setOnClickListener { showDescription(R.string.network_fee, description) }
        }
        if (viewModel.existingPosition) {
            viewModel.averagePriceDescription.observe { description ->
                projectedAveragePrice.setOnClickListener {
                    description?.let { showDescription(R.string.bittensor_exit_avg_buy_price, it) }
                }
            }
        }
        setupExternalActions(viewModel)
        viewModel.wallet.observe(wallet::showWallet)
        viewModel.account.observe(account::showAddress)
        viewModel.assets.observe(assets::setModel)
        viewModel.executionAssets.observe(execution.assets::setModel)
        viewModel.executionSubtitle.observe { execution.subtitle.text = it }
        viewModel.showExecution.observe { show ->
            reviewContent.isVisible = !show
            execution.root.isVisible = show
        }
        viewModel.isExecuting.observe { running ->
            if (running) {
                execution.timer.setState(ExecutionTimerView.State.CountdownTimer(viewModel.remainingExecutionTime()))
                execution.title.setText(R.string.common_do_not_close_app)
                execution.operationLabel.setText(R.string.bittensor_subnet_execution_operation)
            }
        }
        viewModel.rate.observe {
            rate.showBittensorLoadingValue(it)
            execution.rate.showBittensorLoadingValue(it)
        }
        slippage.showValue(viewModel.slippage)
        execution.slippage.showValue(viewModel.slippage)
        viewModel.validator.observe(execution.validator::showValue)
        viewModel.validatorRow.observe { validator.showValidator(ExtendedLoadingState.Loaded(it), FeatureUtils.getCommonApi(requireContext()).imageLoader()) }
        viewModel.validatorIcon.observe {
            execution.validator.image.isVisible = it != null
            execution.validator.image.setImageDrawable(it)
        }
        viewModel.monthlyEstimate.observe(monthlyTokens::showBittensorLoadingValue)
        viewModel.projectedAveragePrice.observe(projectedAveragePrice::showBittensorLoadingValue)
        viewModel.fee.observe {
            networkFee.setFeeStatus(it)
        }
        viewModel.executionFee.observe(execution.networkFee::setFeeStatus)
        viewModel.confirmButtonState.observe(confirm::setState)
        viewModel.executionResult.observe { result ->
            val failure = result as? BittensorStakeResultUi.Failure
            val unconfirmed = result as? BittensorStakeResultUi.Unconfirmed
            val complete = result is BittensorStakeResultUi.Success
            done.isVisible = complete || unconfirmed != null
            retryExecution.isVisible = failure != null
            retryExecution.setText(if (failure?.canRetry == false) R.string.common_close else R.string.common_try_again)
            execution.root.setPadding(0, 0, 0, if (done.isVisible || failure != null) 84.dp(requireContext()) else 0)
            if (unconfirmed != null) {
                execution.showUnconfirmed(unconfirmed)
            } else {
                execution.showFailure(failure)
                if (complete) {
                    execution.showCompleted((result as BittensorStakeResultUi.Success).description)
                }
            }
        }
    }

    private fun showDescription(
        titleRes: Int,
        description: String
    ) {
        DescriptionBottomSheet(requireContext(), getString(titleRes), description)
            .show()
    }
}
